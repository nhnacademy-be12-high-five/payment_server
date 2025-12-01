package com.nhnacademy.payment_server.repository;

import com.nhnacademy.payment_server.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentKey(String PaymentKey);
}
