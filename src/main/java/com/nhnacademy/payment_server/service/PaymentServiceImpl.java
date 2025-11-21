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
        // TODO Feign으로 order 서버에서 orderId와 금액조회 후 금액검증
        // 조회 -> 토스API -> 저장 방식인데 @Transactional이 이 과정의 분리를 고려해볼 수도 있음
        log.info("결제 승인 요청 진입!! orderId: [{}], amount: [{}]", confirmRequest.getOrderId(), confirmRequest.getAmount());

        // [Feign] 포인트 사용 로직
        Long userId = 1L; // TODO 추후 주문서버로 부터 userId와 amount 받기
        Long usePointAmount = 1000L; // 일단 임시로 1000포인트 사용
        String description = "주문번호 " + confirmRequest.getOrderId() + " 상품 결제에 사용";

        try{
            memberPointClient.usePoint(new PointTransactionRequest(userId, usePointAmount, description));
            log.info("포인트 차감 성공. userId=[{}], amount=[{}]", userId, usePointAmount);
        } catch (Exception e) {
            log.error("포인트 차감 실패", e);
            throw new RuntimeException("포인트 사용 실패: " + e.getMessage());
        }

        // 토스 결제 승인 로직
        try {
            Map<String, Object> requestBody = Map.of(
                    "paymentKey", confirmRequest.getPaymentKey(),
                    "orderId", "w13sb5AJA_wUP6OpVL_lk_1763699157664", // TODO Feign으로 order 서버에서 조회했던 토스의 orderId(String)가 들어감
                                                                            // 테스트 할때 새로받은 orderId 계속 갈아끼워야함
                    "amount", confirmRequest.getAmount()
            );

            // Toss API 호출 (RestClient)
            TossConfirmResponse tossResponse = tossRestClient.post()
                    .uri("/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, getTossAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve() // 요청 실행
                    .body(TossConfirmResponse.class);

            if (tossResponse == null || !"DONE".equals(tossResponse.getStatus())) {
                // 처음부터 FAILED 였다면 -> 4xx 5xx 에러가 떴을것
                throw new RuntimeException("Toss 결제 에러");
            }

            String tossMethod = tossResponse.getMethod();
            log.info(">>> Toss가 보낸 결제수단: [{}]", tossMethod);

            PaymentMethod paymentMethod = paymentMethodRepository.findByName(tossResponse.getMethod())
                    .orElseThrow(() -> new RuntimeException("결제수단 선택 안됨"));

            // DB에 엔티티 저장
            Payment payment = new Payment(
                    paymentMethod,
                    confirmRequest.getOrderId(), // 토스가 준 String 값이 아니고 오더에서 넘어온 주문 번호
                   //1L, // 테스트용
                    tossResponse.getRequestedAt().toLocalDateTime(),
                    tossResponse.getApprovedAt().toLocalDateTime(),
                    tossResponse.getStatusEnum(),
                    tossResponse.getPaymentKey(),
                    tossResponse.getTotalAmount()
            );
            Payment savedPayment = paymentRepository.save(payment);

            log.info("결제 승인 및 저장 완료!! paymentId: [{}], status: [{}]",
                    savedPayment.getId(), savedPayment.getStatus());

            return new PaymentConfirmResponse(
                    savedPayment.getId().toString(), // paymentId
                    savedPayment.getStatus(),
                    savedPayment.getAmount()
            );

        } catch (Exception e) {
            log.error("Toss API 승인 요청 중 에러 발생!! orderId: [{}]", confirmRequest.getOrderId(), e);
            // [Feign] 포인트 환불 로직
            try{
                memberPointClient.revertPoint(new PointTransactionRequest(userId, usePointAmount, "결제 실패로 인한 포인트 환불"));
            } catch (Exception error){
                log.error("포인트 롤백 실패...... userId=[{}]", userId, error);
            }
            throw new RuntimeException("결제 승인에 실패하였습니다.", e);
        }

    }

    private String getTossAuthHeader() {
        // 시크릿 키 뒤에 ':'를 붙인뒤 base64로 인코딩하여 Basic 인증 헤더를 만든다 (공식 문서 설명)
        String secretKey = tossPaymentConfig.getSecretKey() + ":";
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedKey;
    }
}
