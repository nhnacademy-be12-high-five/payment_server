package com.nhnacademy.payment_server.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionRequest {
    // 포인트 사용과 환불 요청을 위해 복사해온 DTO
    private Long memberId;
    private Long amount;
    private Long orderId;
}
