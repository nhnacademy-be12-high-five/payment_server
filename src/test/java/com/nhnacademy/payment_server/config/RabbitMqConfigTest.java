package com.nhnacademy.payment_server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.MessageConverter;

class RabbitMqConfigTest {

    @Test
    void testRabbitMqBeansCreated() {
        // given
        RabbitMqConfig config = new RabbitMqConfig();

        // when
        Queue successQueue = config.paymentSuccessQueue();
        Queue failQueue = config.paymentFailQueue();
        MessageConverter converter = config.jsonMessageConverter();

        // then
        assertThat(successQueue).isNotNull();
        assertThat(successQueue.getName()).isEqualTo("payment-success-queue");
        assertThat(successQueue.isDurable()).isTrue();

        assertThat(failQueue).isNotNull();
        assertThat(failQueue.getName()).isEqualTo("payment-fail-queue");
        assertThat(failQueue.isDurable()).isTrue();

        assertThat(converter).isNotNull();
    }
}
