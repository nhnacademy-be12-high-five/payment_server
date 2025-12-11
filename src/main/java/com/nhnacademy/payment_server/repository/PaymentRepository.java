package com.nhnacademy.payment_server.repository;

import com.nhnacademy.payment_server.entity.Payment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // 조회용
    Optional<Payment> findByPaymentKey(String PaymentKey);

    // 결제 취소용 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.paymentKey = :paymentKey")
    Optional<Payment> findByPaymentKeyForUpdate(@Param("paymentKey") String paymentKey);
}
