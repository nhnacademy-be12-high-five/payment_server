package com.nhnacademy.payment_server.controller;

import com.nhnacademy.payment_server.dto.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.PaymentConfirmResponse;
import com.nhnacademy.payment_server.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment API", description = "결제 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 최종 승인", description = "프론트엔드에서 받은 키로 Toss 결제 승인을 요청합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결제 승인 성공 (DONE)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (금액 불일치, 필수값 누락 등)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 주문 번호"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 또는 Toss 결제 실패")
    })
    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(@RequestBody PaymentConfirmRequest request){
        PaymentConfirmResponse response = paymentService.confirmPayment(request);

        return ResponseEntity.ok(response);
    }
}