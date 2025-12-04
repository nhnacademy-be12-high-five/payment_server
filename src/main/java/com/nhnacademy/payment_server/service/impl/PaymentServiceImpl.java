package com.nhnacademy.payment_server.service.impl;

import com.nhnacademy.payment_server.adaptor.TossPaymentAdapter;
import com.nhnacademy.payment_server.client.MemberPointClient;
import com.nhnacademy.payment_server.client.OrderClient;
import com.nhnacademy.payment_server.dto.message.PaymentSuccessMessage;
import com.nhnacademy.payment_server.dto.request.PaymentCancelRequest;
import com.nhnacademy.payment_server.dto.request.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.request.PointTransactionRequest;
import com.nhnacademy.payment_server.dto.response.OrderResponse;
import com.nhnacademy.payment_server.dto.response.PaymentCancelResponse;
import com.nhnacademy.payment_server.dto.response.PaymentConfirmResponse;
import com.nhnacademy.payment_server.dto.response.TossConfirmResponse;
import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.entity.PaymentStatus;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import com.nhnacademy.payment_server.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final MemberPointClient memberPointClient; // Feign
    private final OrderClient orderClient;
    private final TossPaymentAdapter tossAdapter;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest confirmRequest) {
        String provider = confirmRequest.getPaymentMethod().toUpperCase();

        switch (provider) {
            case "POINT" -> throw new UnsupportedOperationException("포인트 전액 결제는 아직 지원하지 않습니다.");
            case "TOSS" -> {

                // TODO Order Server 금액 검증 및 Toss OrderId 조회

                log.info("결제 승인 요청 진입 orderId: [{}], amount: [{}]", confirmRequest.getOrderId(),
                        confirmRequest.getAmount());

                // 1. Toss 최종 결제 전 포인트 차감
                // TODO Order Server에서 받은 memberId, amount, orderId, tossOrderId 사용

                Long memberId = 1L;
                Long usePointAmount = 1000L;
                Long orderId = 1L;
                //String tossOrderId = "w13sb5AJA_wUP6OpVL_lk_1764576862215"; // html 테스트 시도마다 변경

                String tossOrderId = confirmRequest.getOrderId();
                OrderResponse orderDto = orderClient.getOrderByTossId(tossOrderId);

                Long internalOrderId = orderDto.getOrderId(); // dto가 Long으로 바껴야함
                // Long memberId = orderDto.getMemberId(); dto에 추가되면 나중에 주석풀고 사용

                if (!Objects.equals(confirmRequest.getAmount(), orderDto.getTotalAmount())) {
                    throw new RuntimeException("금액 위변조");
                }

                try {
                    memberPointClient.usePoint(new PointTransactionRequest(memberId, usePointAmount, internalOrderId));
                    log.info("포인트 차감 성공 memberId=[{}], amount=[{}]", memberId, usePointAmount);
                } catch (Exception e) {
                    log.error("포인트 차감 실패", e);
                    throw new RuntimeException("포인트 사용 실패: " + e.getMessage());
                }

                // 2. 최종 결제를 위한 토스 외부 API 호출
                TossConfirmResponse tossResponse;
                try {
                    tossResponse = tossAdapter.requestConfirm(confirmRequest.getPaymentKey(), tossOrderId,
                            confirmRequest.getAmount());
                } catch (Exception e) {
                    log.error("Toss API 승인 요청 중 에러 발생 orderId: [{}]", confirmRequest.getOrderId(), e);
                    // 실패 시 포인트 롤백
                    try {
                        //Feign으로 order 서버에서 조회했던 토스의 orderId(String) 넣기
                        memberPointClient.revertPoint(new PointTransactionRequest(memberId, usePointAmount, orderId));
                    } catch (Exception error) {
                        log.error("포인트 롤백 실패 memberId=[{}]", memberId, error);
                    }
                    throw new RuntimeException("Toss 결제 승인 실패", e);
                }

                // 3. DB 저장
                PaymentMethod paymentMethod = paymentMethodRepository.findByName("TOSS")
                        .orElseThrow(() -> new EntityNotFoundException("결제 수단(TOSS) 데이터가 DB에 없습니다."));

                Payment payment = new Payment(
                        paymentMethod,
                        internalOrderId,
                        tossResponse.getRequestedAt().toLocalDateTime(),
                        tossResponse.getApprovedAt().toLocalDateTime(),
                        tossResponse.getStatusEnum(),
                        tossResponse.getPaymentKey(),
                        tossResponse.getTotalAmount()
                );
                Payment savedPayment = paymentRepository.save(payment);

                log.info("결제 승인 및 저장 완료 paymentId: [{}], status: [{}]",
                        savedPayment.getId(), savedPayment.getStatus());

                // RabbitMQ
                try {
                    PaymentSuccessMessage message = new PaymentSuccessMessage(
                            savedPayment.getOrderId(),
                            savedPayment.getPaymentKey(),
                            savedPayment.getAmount(),
                            savedPayment.getApprovedAt()
                    );

                    rabbitTemplate.convertAndSend("payment-success-queue", message);
                    log.info("주문 서버로 결제 성공 메시지 전송 완료. orderId={}", savedPayment.getOrderId());

                } catch (Exception e) {
                    log.error("결제는 성공, 주문 서버 알림 전송 실패 orderId={}", savedPayment.getOrderId(), e);
                }

                return PaymentConfirmResponse.from(savedPayment);
            }
            default -> throw new IllegalArgumentException("지원하지 않는 결제 수단입니다.");
        }
    }

    @Override
    public PaymentCancelResponse cancelPayment(PaymentCancelRequest requestDto) {
        Payment payment = paymentRepository.findByPaymentKey(requestDto.getPaymentKey())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결제입니다."));

        if (!payment.getStatus().equals(PaymentStatus.DONE)) {
            throw new IllegalArgumentException("DONE 상태인 주문만 취소가 가능합니다.");
        }

        try {
            tossAdapter.requestCancel(payment.getPaymentKey(), requestDto.getCancelReason());
        } catch (Exception e) {
            log.error("Toss 결제 취소 요청 실패. paymentKey={}", payment.getPaymentKey(), e);
            throw new RuntimeException("Toss 결제 취소에 실패했습니다: " + e.getMessage());
        }

        // DB 결제 상태 변경 (더티 체킹)
        payment.setStatus(PaymentStatus.CANCELED);
        payment.setCancelledAt(LocalDateTime.now());

        // 포인트 환불 요청 (Feign) 결제 취소는 됐으니 포인트 환불 실패해도 진행됨
        if (requestDto.getRefundPointAmount() != null && requestDto.getRefundPointAmount() > 0) {
            try {
                memberPointClient.revertPoint(new PointTransactionRequest(
                        requestDto.getMemberId(),
                        requestDto.getRefundPointAmount(),
                        requestDto.getOrderId()
                ));
                log.info("포인트 환불 성공. orderId={}", requestDto.getOrderId());
            } catch (Exception e) {
                log.error("결제 취소 성공, 포인트 환불 실패. orderId={}", requestDto.getOrderId(), e);
            }
        }

        return PaymentCancelResponse.from(payment, requestDto.getCancelReason());
    }
}
