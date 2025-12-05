package com.nhnacademy.payment_server.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderValidationInfoResponse {
    private Long orderId;
    private Long realAmount; // 원래 Integer인데 여기 서버에서는 Long이라서 잭슨이 변환
    private String orderKey;
    private Long memberId;
}