package com.nhnacademy.payment_server.dto.response;

import com.nhnacademy.payment_server.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class PaymentConfirmResponse {
    @Schema(description = "최종 승인 완료된 결제 번호", example = "1")
    private String paymentId;
    @Schema(description = "결제 상태", example = "DONE")
    private PaymentStatus status;
    @Schema(description = "결제 완료된 금액(영수증)", example = "50000")
    private Long amount;

    public PaymentConfirmResponse(String paymentId, PaymentStatus status, Long amount) {
        this.paymentId = paymentId;
        this.status = status;
        this.amount = amount;
    }
}
