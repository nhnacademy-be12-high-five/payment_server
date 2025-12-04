package com.nhnacademy.payment_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MethodStatusRequest {
    @Schema(description = "결제수단 상태 (활성, 비활성)")
    private boolean isActive;
}