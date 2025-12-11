package com.nhnacademy.payment_server.scheduler;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import com.nhnacademy.payment_server.repository.PaymentMessageOutboxRepository;
import com.nhnacademy.payment_server.service.PaymentOutboxService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentMessageSchedulerTest {

    @InjectMocks
    private PaymentMessageScheduler scheduler;

    @Mock
    private PaymentMessageOutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PaymentOutboxService outboxService;

    @Test
    @DisplayName("대기 중인 메시지 없을 시 아무것도 안함")
    void sendPendingMessages_noMessages() {
        given(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(MessageStatus.READY))
                .willReturn(Collections.emptyList());

        scheduler.sendPendingMessages();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("메시지 전송 성공 시 DONE 상태 변경 및 서비스 호출")
    void sendPendingMessages_success() {
        String payload = "{\"orderId\":1}";
        PaymentMessageOutbox message = PaymentMessageOutbox.builder()
                .id(1L)
                .paymentId(100L)
                .payload(payload)
                .status(MessageStatus.READY)
                .createdAt(LocalDateTime.now())
                .build();

        given(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(MessageStatus.READY))
                .willReturn(List.of(message));

        scheduler.sendPendingMessages();

        // RabbitMQ 전송 메서드가 호출되었는지 확인
        verify(rabbitTemplate, times(1)).convertAndSend(eq("payment-success-queue"), eq(payload));

        // updateStatus 호출 되었는지 확인
        verify(outboxService).updateStatus(1L, MessageStatus.DONE);
    }

    @Test
    @DisplayName("RabbitMQ 전송 실패 시 재시도 카운트 증가 및 서비스 호출")
    void sendPendingMessages_failure() {
        // given
        PaymentMessageOutbox message = PaymentMessageOutbox.builder()
                .id(1L)
                .payload("{}")
                .status(MessageStatus.READY)
                .build();

        given(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(MessageStatus.READY))
                .willReturn(List.of(message));

        // RabbitMQ 실패 상황
        doThrow(new AmqpException("RabbitMQ Connection Failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyObject()); // anyObject() 또는 eq("{}")

        scheduler.sendPendingMessages();

        verify(outboxService).increaseRetryCount(1L);
    }

    // RabbitTemplate의 convertAndSend 인자 매칭용 헬퍼 (any() 사용 시 컴파일 에러 방지)
    private Object anyObject() {
        return org.mockito.ArgumentMatchers.any();
    }
}