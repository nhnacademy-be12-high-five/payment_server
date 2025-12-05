package com.nhnacademy.payment_server.client;

import com.nhnacademy.payment_server.dto.response.OrderValidationInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "team5-order-server")
public interface OrderClient {
    @GetMapping("/order/api/orders/{orderId}/payment-info")
    OrderValidationInfoResponse getOrderInfo(@PathVariable("orderId") Long orderId);

    @GetMapping("/order/api/orders/key/{orderKey}")
    OrderValidationInfoResponse getOrderByKey(@PathVariable("orderKey") String orderKey);
}
