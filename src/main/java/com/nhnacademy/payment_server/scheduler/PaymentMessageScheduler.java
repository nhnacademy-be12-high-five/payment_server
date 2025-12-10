package com.nhnacademy.payment_server.scheduler;

import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import com.nhnacademy.payment_server.repository.PaymentMessageOutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageScheduler {

    private final PaymentMessageOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    // 2초마다 실행
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void sendPendingMessages() {
        // 제일 먼저 도착한 대기 메시지 상위 10개 조회
        List<PaymentMessageOutbox> messages =
                outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(MessageStatus.READY);

        if (messages.isEmpty()) {
            return;
        }

        for (PaymentMessageOutbox message : messages) {
            try {
                rabbitTemplate.convertAndSend("payment-success-queue", message.getPayload());

                message.setStatus(MessageStatus.DONE);

                log.info("지연 메시지 발송 성공: outboxId={}, paymentId={}", message.getId(), message.getPaymentId());

            } catch (Exception e) {
                log.error("메시지 발송 실패 (재시도 예정): outboxId={}", message.getId(), e);
            }
        }
    }
}