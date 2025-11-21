package com.nhnacademy.payment_server;

import com.nhnacademy.payment_server.config.TossPaymentConfig;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RestClient restClient;
    @Mock
    private TossPaymentConfig tossPaymentConfig;
}
