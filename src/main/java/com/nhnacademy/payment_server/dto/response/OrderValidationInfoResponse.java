package com.nhnacademy.payment_server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderValidationInfoResponse {
    private Long orderId;
    private Long paymentAmount;
    private String orderKey;
    private Long userId;
    private long usedPoint; // 포인트는 안 쓸 수도 있으니까 혹시 null이라면 자동 0
}