package com.nhnacademy.payment_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "주문 정보 응답")
public class OrderResponse {

    private Long orderId;
    private LocalDateTime orderDate;
    private String status;
    private Long totalAmount;
    private List<OrderItemResponse> items;

    @Getter
    @Builder
    public static class OrderItemResponse {
        private String bookTitle;
        private Integer quantity;
        private Integer price;
    }
}
