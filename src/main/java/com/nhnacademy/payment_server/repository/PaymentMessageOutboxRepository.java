package com.nhnacademy.payment_server.repository;

import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMessageOutboxRepository extends JpaRepository<PaymentMessageOutbox, Long> {
    List<PaymentMessageOutbox> findTop10ByStatusOrderByCreatedAtAsc(MessageStatus status);
}
