package com.nhnacademy.payment_server.config;

import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class InitDataConfig {

    @Bean
    public CommandLineRunner initPaymentMethods(PaymentMethodRepository repository) {
        return args -> {
            List<PaymentMethod> methods = List.of(
                    PaymentMethod.builder().name("TOSS").alias("간편결제 / 신용카드").isActive(true).build(),
                    PaymentMethod.builder().name("BANK_TRANSFER").alias("계좌이체").isActive(false).build(),
                    PaymentMethod.builder().name("VIRTUAL_ACCOUNT").alias("가상계좌").isActive(false).build()
            );

            for (PaymentMethod method : methods) {
                if (!repository.existsByName(method.getName())) {
                    repository.save(method);
                }
            }
        };
    }
}