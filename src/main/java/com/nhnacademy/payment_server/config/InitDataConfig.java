package com.nhnacademy.payment_server.config;

import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class InitDataConfig {

    private final PaymentMethodRepository repository;

    @Bean
    public CommandLineRunner initPaymentMethods() {
        return args -> initializeMethods();
    }

    @Transactional
    public void initializeMethods() {
        List<PaymentMethod> targetMethods = List.of(
                PaymentMethod.builder().name("Toss").alias("간편결제/신용카드").isActive(true).build(),
                PaymentMethod.builder().name("TRANSFER").alias("계좌이체").isActive(false).build(),
                PaymentMethod.builder().name("VIRTUAL_ACCOUNT").alias("무통장입금").isActive(false).build()
        );

        // N+1 문제 해결
        // DB에 있는 모든 이름을 한 번에 가져와서 Set
        Set<String> existingNames = repository.findAll().stream()
                .map(PaymentMethod::getName)
                .collect(Collectors.toSet());

        // 메모리 상에서 비교 후 없는 것만 필터링
        List<PaymentMethod> newMethods = targetMethods.stream()
                .filter(m -> !existingNames.contains(m.getName()))
                .toList();

        // 한 번에 저장
        if (!newMethods.isEmpty()) {
            repository.saveAll(newMethods);
        }
    }
}