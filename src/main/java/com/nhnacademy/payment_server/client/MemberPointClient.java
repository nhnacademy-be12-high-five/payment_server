package com.nhnacademy.payment_server.client;

import com.nhnacademy.payment_server.dto.PointTransactionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "member-server")
public interface MemberPointClient {

    @PostMapping("/internal/points/use")
    void usePoint(@RequestBody PointTransactionRequest requestDto);

    @PostMapping("/internal/points/revert")
    void revertPoint(@RequestBody PointTransactionRequest requestDto);
}