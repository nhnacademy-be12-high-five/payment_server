package com.nhnacademy.payment_server.controller;

import com.nhnacademy.payment_server.docs.AdminPaymentSwagger;
import com.nhnacademy.payment_server.dto.response.DailySalesResponse;
import com.nhnacademy.payment_server.dto.response.PaymentStatsResponse;
import com.nhnacademy.payment_server.service.PaymentStatService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/admin/stats")
@RequiredArgsConstructor
public class AdminPaymentController implements AdminPaymentSwagger {

    private final PaymentStatService paymentStatService;

    @GetMapping("/summary")
    @Override
    public ResponseEntity<PaymentStatsResponse> getSummary() {
        return ResponseEntity.ok(paymentStatService.getTotalStats());
    }

    @GetMapping("/daily")
    @Override
    public ResponseEntity<List<DailySalesResponse>> getDailyStats(LocalDate startDate, LocalDate endDate) {
        // 날짜 파라미터 없으면 최근 7일
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(7);

        return ResponseEntity.ok(
                paymentStatService.getDailyStats(startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
        );
    }
}