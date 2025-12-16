package com.nhnacademy.payment_server.service;

import com.nhnacademy.payment_server.dto.response.DailySalesResponse;
import com.nhnacademy.payment_server.dto.response.PaymentStatsResponse;
import com.nhnacademy.payment_server.entity.PaymentStatus;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentStatService {

    private final PaymentRepository paymentRepository;

    // 전체 요약 통계
    public PaymentStatsResponse getTotalStats() {
        Long sales = paymentRepository.sumAmountByStatus(PaymentStatus.DONE).orElse(0L);
        Long cancels = paymentRepository.sumAmountByStatus(PaymentStatus.CANCELED).orElse(0L);
        long successCount = paymentRepository.countByStatus(PaymentStatus.DONE);
        long cancelCount = paymentRepository.countByStatus(PaymentStatus.CANCELED);

        return PaymentStatsResponse.builder()
                .totalSalesAmount(sales)
                .totalCancelAmount(cancels)
                .netSalesAmount(sales - cancels)
                .successCount(successCount)
                .cancelCount(cancelCount)
                .totalTransactionCount(successCount + cancelCount)
                .build();
    }

    // 기간별 일일 매출 통계
    public List<DailySalesResponse> getDailyStats(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.findDailySalesBetween(start, end);
    }
}