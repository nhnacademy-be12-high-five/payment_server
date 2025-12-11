package com.nhnacademy.payment_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelRequest {

    @Schema(description = "취소할 결제 고유 키 (Toss)", example = "tviva2024...")
    private String paymentKey;

    @Schema(description = "취소 사유", example = "단순 변심")
    private String cancelReason;

    // 주문 서버가 알려줘야 함
    @Schema(description = "복구될 포인트 금액 (없으면 0)", example = "1000")
    private Long refundPointAmount;

    @Schema(description = "주문 번호 (포인트 서버 전달용)", example = "123")
    private Long orderId;

    @Schema(description = "회원 ID (포인트 서버 전달용)", example = "1")
    private Long memberId;
}