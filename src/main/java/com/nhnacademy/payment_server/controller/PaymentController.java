package com.nhnacademy.payment_server.controller;

import com.nhnacademy.payment_server.dto.request.PaymentCancelRequest;
import com.nhnacademy.payment_server.dto.request.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.response.PaymentConfirmResponse;
import com.nhnacademy.payment_server.service.PaymentService;
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
public class PaymentController implements PaymentSwagger {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(@RequestBody PaymentConfirmRequest requestDto){
        PaymentConfirmResponse responseDto = paymentService.confirmPayment(requestDto);

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelPayment(@RequestBody PaymentCancelRequest request) {
        paymentService.cancelPayment(request);
        return ResponseEntity.ok().build();
    }
}