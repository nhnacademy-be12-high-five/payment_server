package com.nhnacademy.payment_server.client;

import com.nhnacademy.payment_server.dto.response.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "team5-order-server")
public interface OrderClient {
    @GetMapping("/order/api/orders/toss/{tossOrderId}") // 임시
    OrderResponse getOrderByTossId(@PathVariable("tossOrderId") String tossOrderId);
}
