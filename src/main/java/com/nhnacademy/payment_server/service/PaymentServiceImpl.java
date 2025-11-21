package com.nhnacademy.payment_server.service;

import com.nhnacademy.payment_server.client.MemberPointClient;
import com.nhnacademy.payment_server.config.TossPaymentConfig;
import com.nhnacademy.payment_server.dto.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.PaymentConfirmResponse;
import com.nhnacademy.payment_server.dto.PointTransactionRequest;
import com.nhnacademy.payment_server.dto.TossConfirmResponse;
import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService{

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final RestClient tossRestClient;
    private final TossPaymentConfig tossPaymentConfig; // (시크릿 키)
    private final MemberPointClient memberPointClient; // Feign

    @Override
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest confirmRequest) {
        // TODO Order Server 연동 (금액 검증 및 Toss OrderId 조회)

        log.info("결제 승인 요청 진입 orderId: [{}], amount: [{}]", confirmRequest.getOrderId(), confirmRequest.getAmount());

        // 1. Toss 최종 결제 전 포인트 차감
        // TODO Order Server에서 받은 userId, amount 사용
        Long userId = 1L;
        Long usePointAmount = 1000L;
        String description = "주문번호 " + confirmRequest.getOrderId() + " 상품 결제에 사용";

        try{
            memberPointClient.usePoint(new PointTransactionRequest(userId, usePointAmount, description));
            log.info("포인트 차감 성공 userId=[{}], amount=[{}]", userId, usePointAmount);
        } catch (Exception e) {
            log.error("포인트 차감 실패", e);
            throw new RuntimeException("포인트 사용 실패: " + e.getMessage());
        }

        // 2. 최종 결제를 위한 토스 외부 API 호출
        try {
            Map<String, Object> requestBody = Map.of(
                    "paymentKey", confirmRequest.getPaymentKey(),
                    "orderId", "w13sb5AJA_wUP6OpVL_lk_1763699157664", // TODO Feign으로 order 서버에서 조회했던 토스의 orderId(String)가 들어감
                    "amount", confirmRequest.getAmount()
            );

            TossConfirmResponse tossResponse = tossRestClient.post()
                    .uri("/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, getTossAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(TossConfirmResponse.class);

            if (tossResponse == null || !"DONE".equals(tossResponse.getStatus())) {
                throw new RuntimeException("Toss 결제 에러: DONE 상태가 아닙니다");
            }

            // 3. DB 저장
            PaymentMethod paymentMethod = paymentMethodRepository.findByName(tossResponse.getMethod())
                    .orElseThrow(() -> new RuntimeException("결제수단 선택 안됨"));

            Payment payment = new Payment(
                    paymentMethod,
                    confirmRequest.getOrderId(),
                    tossResponse.getRequestedAt().toLocalDateTime(),
                    tossResponse.getApprovedAt().toLocalDateTime(),
                    tossResponse.getStatusEnum(),
                    tossResponse.getPaymentKey(),
                    tossResponse.getTotalAmount()
            );
            Payment savedPayment = paymentRepository.save(payment);

            log.info("결제 승인 및 저장 완료 paymentId: [{}], status: [{}]",
                    savedPayment.getId(), savedPayment.getStatus());

            return new PaymentConfirmResponse(
                    savedPayment.getId().toString(), // paymentId
                    savedPayment.getStatus(),
                    savedPayment.getAmount()
            );

        } catch (Exception e) {
            log.error("Toss API 승인 요청 중 에러 발생 orderId: [{}]", confirmRequest.getOrderId(), e);
            // 4. 결제 실패시 포인트 환불
            try{
                memberPointClient.revertPoint(new PointTransactionRequest(userId, usePointAmount, "결제 실패로 인한 포인트 환불"));
                log.info("포인트 롤백 성공");
            } catch (Exception error){
                log.error("포인트 롤백 실패 userId=[{}]", userId, error);
            }
            throw new RuntimeException("결제 승인에 실패하였습니다", e);
        }

    }

    private String getTossAuthHeader() {
        // 토스 secretKey를 base64로 인코딩 -> Basic Auth 헤더
        String secretKey = tossPaymentConfig.getSecretKey() + ":";
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedKey;
    }
}
