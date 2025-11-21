package com.nhnacademy.payment_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {
    @Schema(description = "Toss에서 받은 결제 식별 키", example = "tviva2024...")
    private String paymentKey;
    @Schema(description = "주문 서버에서 생성된 주문 번호", example = "1")
    private Long orderId;
    @Schema(description = "주문 서버에서 생성된 최종 승인 요청할 금액", example = "50000")
    private Long amount;
}
