package com.nhnacademy.payment_server.scheduler;

import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import com.nhnacademy.payment_server.repository.PaymentMessageOutboxRepository;
import com.nhnacademy.payment_server.service.PaymentOutboxService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageScheduler {

    private final PaymentMessageOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentOutboxService outboxService;
    private static final int MAX_RETRY_COUNT = 3;

    // 2초마다 실행
    @Scheduled(fixedDelay = 2000)
    public void sendPendingMessages() {
        // 제일 먼저 도착한 대기 메시지 상위 10개 조회
        List<PaymentMessageOutbox> messages =
                outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(MessageStatus.READY);

        if (messages.isEmpty()) {
            return;
        }

        for (PaymentMessageOutbox message : messages) {
            if (message.getRetryCount() >= MAX_RETRY_COUNT) {
                log.warn("메시지 전송 실패 횟수 초과. FAILED 처리. id={}", message.getId());

                outboxService.markAsFailed(message.getId());

                continue;
            }
            try {
                // 트랜잭션 없는 RabbitMQ 전송
                rabbitTemplate.convertAndSend("payment-success-queue", message.getPayload());

                outboxService.updateStatus(message.getId(), MessageStatus.DONE);

                log.info("지연 메시지 발송 성공: outboxId={}", message.getId());
            } catch (Exception e) {
                log.error("메시지 발송 실패. count={} id={}", message.getRetryCount() + 1, message.getId(), e);

                outboxService.increaseRetryCount(message.getId());
            }
        }
    }
}