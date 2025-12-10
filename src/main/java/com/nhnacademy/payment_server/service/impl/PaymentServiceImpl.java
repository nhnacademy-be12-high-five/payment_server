package com.nhnacademy.payment_server.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.payment_server.adaptor.TossPaymentAdapter;
import com.nhnacademy.payment_server.client.MemberPointClient;
import com.nhnacademy.payment_server.client.OrderClient;
import com.nhnacademy.payment_server.dto.message.PaymentSuccessMessage;
import com.nhnacademy.payment_server.dto.request.PaymentCancelRequest;
import com.nhnacademy.payment_server.dto.request.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.request.PointTransactionRequest;
import com.nhnacademy.payment_server.dto.response.OrderValidationInfoResponse;
import com.nhnacademy.payment_server.dto.response.PaymentCancelResponse;
import com.nhnacademy.payment_server.dto.response.PaymentConfirmResponse;
import com.nhnacademy.payment_server.dto.response.TossConfirmResponse;
import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.entity.PaymentStatus;
import com.nhnacademy.payment_server.exception.BusinessException;
import com.nhnacademy.payment_server.exception.ErrorCode;
import com.nhnacademy.payment_server.repository.PaymentMessageOutboxRepository;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import com.nhnacademy.payment_server.service.PaymentService;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final OrderClient orderClient; // Feign
    private final TossPaymentAdapter tossAdapter;
    private final PaymentMessageOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest requestDto) {
        String provider = requestDto.getPaymentMethod().toUpperCase();

        return switch (provider) {
            case "TOSS" -> processTossPayment(requestDto);
            case "POINT" -> throw new BusinessException(ErrorCode.UNSUPPORTED_METHOD);
            default -> throw new BusinessException(ErrorCode.METHOD_NOT_FOUND);
        };
    }

    public PaymentConfirmResponse processTossPayment(PaymentConfirmRequest requestDto) {
        log.info("결제 승인 요청 진입 orderKey: {}, amount: {}", requestDto.getOrderKey(), requestDto.getAmount());

        // 검증을 위해 주문 서버에서 결제 정보를 받아오기
        String orderKey = requestDto.getOrderKey();
        OrderValidationInfoResponse orderDto = orderClient.getOrderByKey(orderKey);

        Long orderId = orderDto.getOrderId();
        Long memberId = orderDto.getMemberId();
        Long realAmount = orderDto.getRealAmount();

        // 금액 위변조 검증
        if (!Objects.equals(requestDto.getAmount(), realAmount)) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }

        // todo 주문서버에서 실제 차감포인트금액 알아오기
        Long usePointAmount = 1000L;

        try {
            if (usePointAmount > 0) {
                memberPointClient.usePoint(new PointTransactionRequest(memberId, usePointAmount, orderId));
                log.info("포인트 차감 성공 memberId={}, amount={}", memberId, usePointAmount);
            }
        } catch (Exception e) {
            log.error("멤버 서버 통신 오류, 포인트 차감 실패", e);
            throw new BusinessException(ErrorCode.POINT_API_ERROR);
        }

        // 토스 - 최종 결제 승인
        TossConfirmResponse tossResponse;

        try {
            tossResponse = tossAdapter.requestConfirm(requestDto.getPaymentKey(), orderKey, realAmount);
        } catch (Exception tossEx) {
            log.error("Toss 최종 승인 실패. 롤백 진행. orderKey: {}", orderKey, tossEx);

            if (usePointAmount > 0) {
                try {
                    memberPointClient.revertPoint(new PointTransactionRequest(memberId, usePointAmount, orderId));
                } catch (Exception pointEx) {
                    log.error("포인트 롤백 실패 memberId={}, orderId={}", memberId, orderId, pointEx);
                }
            }
            throw new BusinessException(ErrorCode.TOSS_API_ERROR);
        }

        // DB 저장
        PaymentMethod paymentMethod = paymentMethodRepository.findByName("TOSS")
                .orElseThrow(() -> new BusinessException(ErrorCode.METHOD_NOT_FOUND));

        Payment payment = Payment.builder()
                .paymentMethod(paymentMethod)
                .orderId(orderId)
                .requestedAt(tossResponse.getRequestedAt().toLocalDateTime())
                .approvedAt(tossResponse.getApprovedAt().toLocalDateTime())
                .status(tossResponse.getStatusEnum())
                .paymentKey(tossResponse.getPaymentKey())
                .amount(tossResponse.getTotalAmount())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        log.info("결제 승인 및 저장 완료 paymentId: [{}], status: [{}]", savedPayment.getId(), savedPayment.getStatus());

        // Outbox DB 저장
        try {
            PaymentSuccessMessage message = new PaymentSuccessMessage(
                    savedPayment.getOrderId(),
                    savedPayment.getPaymentKey(),
                    savedPayment.getAmount(),
                    savedPayment.getApprovedAt()
            );

            // 객체 -> JSON 문자열 변환
            String jsonPayload = objectMapper.writeValueAsString(message);

            PaymentMessageOutbox outbox = PaymentMessageOutbox.builder()
                    .paymentId(savedPayment.getId())
                    .payload(jsonPayload)
                    .status(MessageStatus.READY) // 대기 상태
                    .build();

            outboxRepository.save(outbox);

            log.info("결제 성공 메시지 Outbox 저장 완료. orderId={}", savedPayment.getOrderId());

        } catch (JsonProcessingException e) {
            log.error("메시지 변환 오류", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return PaymentConfirmResponse.from(savedPayment);
    }




    @Override
    public PaymentCancelResponse cancelPayment(PaymentCancelRequest requestDto) {
        Payment payment = paymentRepository.findByPaymentKey(requestDto.getPaymentKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getStatus().equals(PaymentStatus.DONE)) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        try {
            tossAdapter.requestCancel(payment.getPaymentKey(), requestDto.getCancelReason());
        } catch (Exception e) {
            log.error("Toss 결제 취소 실패. paymentKey={}", payment.getPaymentKey(), e);
            throw new BusinessException(ErrorCode.TOSS_API_ERROR);
        }

        // DB 결제 상태 변경 (더티 체킹)
        payment.setStatus(PaymentStatus.CANCELED);
        payment.setCancelledAt(LocalDateTime.now());

        // 포인트 환불 로직 - 실패해도 결제는 취소 유지
        if (requestDto.getRefundPointAmount() != null && requestDto.getRefundPointAmount() > 0) {
            try {
                memberPointClient.revertPoint(new PointTransactionRequest(
                        requestDto.getMemberId(),
                        requestDto.getRefundPointAmount(),
                        requestDto.getOrderId()
                ));
                log.info("결제 취소 및 포인트 환불 성공. orderId={}", requestDto.getOrderId());
            } catch (Exception e) {
                log.error("결제 취소 성공, 포인트 환불 실패. orderId={}", requestDto.getOrderId(), e);
            }
        }
        return PaymentCancelResponse.from(payment, requestDto.getCancelReason());
    }
}