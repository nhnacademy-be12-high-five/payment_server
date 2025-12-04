package com.nhnacademy.payment_server.service.impl;

import com.nhnacademy.payment_server.dto.response.PaymentMethodResponse;
import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import com.nhnacademy.payment_server.service.PaymentMethodService;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    public List<PaymentMethodResponse> getActiveMethods() {
        List<PaymentMethod> entities = paymentMethodRepository.findByIsActiveTrue();
        List<PaymentMethodResponse> responses = new ArrayList<>();

        for (PaymentMethod entity : entities) {
            responses.add(PaymentMethodResponse.from(entity));
        }
        return responses;
    }

    @Override
    public List<PaymentMethodResponse> getAllMethods() {
        List<PaymentMethod> entities = paymentMethodRepository.findAll();
        List<PaymentMethodResponse> responses = new ArrayList<>();

        for (PaymentMethod entity : entities) {
            responses.add(PaymentMethodResponse.from(entity));
        }
        return responses;
    }

    @Override
    @Transactional
    public void updateStatus(Long methodId, boolean isActive) {
        PaymentMethod method = paymentMethodRepository.findById(methodId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결제 수단입니다."));
        method.updateStatus(isActive);
    }
}