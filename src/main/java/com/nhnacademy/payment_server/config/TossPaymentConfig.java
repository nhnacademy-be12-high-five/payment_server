package com.nhnacademy.payment_server.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class TossPaymentConfig {

    // 나중에 yml-warehouse에 toss.secretKey, toss.clientKey 값 넣고 주입받음 (추후 넣을예정 현재는 application.yml에 임시로 넣음)
    @Value("${toss.secretKey}")
    private String secretKey;

    @Value("${toss.clientKey}")
    private String clientKey;
}