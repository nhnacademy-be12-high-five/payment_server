package com.nhnacademy.payment_server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.payment_server.adaptor.TossPaymentAdapter;
import com.nhnacademy.payment_server.client.MemberPointClient;
import com.nhnacademy.payment_server.client.OrderClient;
import com.nhnacademy.payment_server.dto.request.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.request.PointTransactionRequest;
import com.nhnacademy.payment_server.dto.response.OrderValidationInfoResponse;
import com.nhnacademy.payment_server.dto.response.PaymentConfirmResponse;
import com.nhnacademy.payment_server.dto.response.TossConfirmResponse;
import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.entity.PaymentStatus;
import com.nhnacademy.payment_server.exception.BusinessException;
import com.nhnacademy.payment_server.exception.ErrorCode;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import com.nhnacademy.payment_server.service.impl.PaymentServiceImpl;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private MemberPointClient memberPointClient;
    @Mock
    private TossPaymentAdapter tossPaymentAdapter;
    @Mock
    private OrderClient orderClient;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("결제 승인 성공: 모든 검증 통과 및 저장 완료")
    void confirmPayment_Success() {
        // given
        String paymentKey = "test_key";
        String tossOrderId = "toss_123";
        Long orderId = 100L;
        Long amount = 50000L;
        Long memberId = 1L;

        PaymentConfirmRequest request = new PaymentConfirmRequest(paymentKey, tossOrderId, amount, "TOSS");

        // 1. 주문 서버 응답 Mocking
        OrderValidationInfoResponse orderDto = OrderValidationInfoResponse.builder()
                .orderId(orderId)
                .realAmount(amount)
                .orderKey(tossOrderId)
                .memberId(memberId)
                .build();
        given(orderClient.getOrderByKey(tossOrderId)).willReturn(orderDto);

        // 2. Toss 응답 Mocking
        TossConfirmResponse tossResponse = TossConfirmResponse.builder()
                .method("카드")
                .status("DONE")
                .totalAmount(amount)
                .paymentKey(paymentKey)
                .requestedAt(OffsetDateTime.now())
                .approvedAt(OffsetDateTime.now())
                .build();

        given(tossPaymentAdapter.requestConfirm(any(), any(), any())).willReturn(tossResponse);

        // 3. 결제 수단 조회 Mocking
        PaymentMethod method = new PaymentMethod("TOSS", "토스", true);
        given(paymentMethodRepository.findByName("TOSS")).willReturn(Optional.of(method));

        // 4. 저장 Mocking (ID가 있는 객체 리턴)
        Payment savedPayment = new Payment(method, orderId, LocalDateTime.now(), LocalDateTime.now(), PaymentStatus.DONE, paymentKey, amount);

        ReflectionTestUtils.setField(savedPayment, "id", 1L);

        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        // when
        PaymentConfirmResponse response = paymentService.confirmPayment(request);

        // then
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.DONE);

        // 포인트 차감 호출 확인
        verify(memberPointClient).usePoint(any(PointTransactionRequest.class));
        // RabbitMQ 발송 확인
        verify(rabbitTemplate).convertAndSend(eq("payment-success-queue"), any(Object.class));
    }

    @Test
    @DisplayName("결제 실패시 예외 및 포인트 롤백 호출")
    void confirmPayment_Rollback(){
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("testKey", "apple", 10000L, "TOSS");

        OrderValidationInfoResponse orderDto = OrderValidationInfoResponse.builder()
                .orderId(100L)
                .orderKey("apple")
                .realAmount(10000L)
                .memberId(1L)
                .build();
        given(orderClient.getOrderByKey("apple")).willReturn(orderDto);

        given(tossPaymentAdapter.requestConfirm(any(), any(), any()))
                .willThrow(new RuntimeException("Toss 통신 오류"));

        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TOSS_API_ERROR);

        verify(memberPointClient, times(1)).revertPoint(any(PointTransactionRequest.class));
    }

    @Test
    @DisplayName("결제 실패: 금액 불일치 (위변조 감지)")
    void confirmPayment_Fail_AmountMismatch() {
        String tossOrderId = "hack_order";
        PaymentConfirmRequest request = new PaymentConfirmRequest("key", tossOrderId, 100L, "TOSS"); // 100원 요청

        OrderValidationInfoResponse orderDto = OrderValidationInfoResponse.builder()
                .realAmount(50000L)
                .orderKey(tossOrderId)
                .build();
        given(orderClient.getOrderByKey(tossOrderId)).willReturn(orderDto);

        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AMOUNT);
    }
}
