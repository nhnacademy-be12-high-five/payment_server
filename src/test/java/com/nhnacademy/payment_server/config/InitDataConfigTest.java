package com.nhnacademy.payment_server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;

@ExtendWith(MockitoExtension.class)
class InitDataConfigTest {

    @Mock
    private PaymentMethodRepository repository;

    @InjectMocks
    private InitDataConfig initDataConfig;

    @Test
    @DisplayName("DB에 데이터가 없으면 3개 모두 저장 (Branch: True)")
    void initPaymentMethods_saveAll() throws Exception {
        given(repository.findAll()).willReturn(Collections.emptyList());

        CommandLineRunner runner = initDataConfig.initPaymentMethods();
        runner.run();

        ArgumentCaptor<List<PaymentMethod>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<PaymentMethod> savedMethods = captor.getValue();
        assertThat(savedMethods).hasSize(3);
        assertThat(savedMethods)
                .extracting(PaymentMethod::getName)
                .containsExactlyInAnyOrder("Toss", "TRANSFER", "VIRTUAL_ACCOUNT");
    }

    @Test
    @DisplayName("DB에 이미 데이터가 있으면 저장x (Branch: False)")
    void initPaymentMethods_skipSave() throws Exception {
        List<PaymentMethod> existingMethods = List.of(
                PaymentMethod.builder().name("Toss").alias("간편결제/신용카드").isActive(true).build(),
                PaymentMethod.builder().name("TRANSFER").alias("계좌이체").isActive(false).build(),
                PaymentMethod.builder().name("VIRTUAL_ACCOUNT").alias("무통장입금").isActive(false).build()
        );
        given(repository.findAll()).willReturn(existingMethods);

        CommandLineRunner runner = initDataConfig.initPaymentMethods();
        runner.run();

        verify(repository, never()).saveAll(anyList());
    }
}