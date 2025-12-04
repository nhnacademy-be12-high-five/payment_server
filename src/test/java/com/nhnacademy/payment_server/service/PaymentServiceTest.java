package com.nhnacademy.payment_server.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nhnacademy.payment_server.client.MemberPointClient;
import com.nhnacademy.payment_server.config.TossPaymentConfig;
import com.nhnacademy.payment_server.dto.request.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.request.PointTransactionRequest;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import com.nhnacademy.payment_server.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private RestClient tossRestClient;
    @Mock
    private TossPaymentConfig tossPaymentConfig;
    @Mock
    MemberPointClient memberPointClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("결제 실패시 포인트 롤백 호출")
    void confirmPayment_Rollback(){
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("testKey", "apple", 10000L, "TOSS");

        // Toos 호출에서 에러
        when(tossRestClient.post()).thenThrow(new RuntimeException("테스트용 Toss 통신 오류"));

        // when 예외가 터지는지 확인
        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("결제 승인에 실패하였습니다");

        // then 포인트 롤백 호출 됐는지 확인
        verify(memberPointClient, times(1)).revertPoint(any(PointTransactionRequest.class));
    }
}
