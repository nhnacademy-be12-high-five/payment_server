package com.nhnacademy.payment_server.service;

import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import com.nhnacademy.payment_server.repository.PaymentMessageOutboxRepository;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentOutboxService {

    private final PaymentMessageOutboxRepository outboxRepository;
    private final PaymentRepository paymentRepository;

    // 클래스 단위로 적용 되어 있던 긴 트랜잭션을 분산하여 짧은 트랜잭션으로 동작하게 만든 메서드들
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(Long id, MessageStatus status) {
        outboxRepository.findById(id).ifPresent(msg -> msg.setStatus(status));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increaseRetryCount(Long id) {
        outboxRepository.findById(id).ifPresent(PaymentMessageOutbox::incrementRetryCount);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long id) {
        outboxRepository.findById(id).ifPresent(msg -> msg.setStatus(MessageStatus.FAILED));
    }

    @Transactional
    public Payment savePaymentAndOutbox(Payment payment, PaymentMessageOutbox outbox) {
        Payment savedPayment = paymentRepository.save(payment);

        PaymentMessageOutbox finalOutbox = PaymentMessageOutbox.builder()
                .paymentId(savedPayment.getId())
                .payload(outbox.getPayload())
                .status(MessageStatus.READY)
                .build();

        outboxRepository.save(finalOutbox);

        return savedPayment;
    }
}