package com.example.seata.at.order.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.order.api.dto.CommonResponse;
import com.example.seata.at.order.api.dto.OrderCreateRequest;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Service
public class OrderService {
    private final OrderMapper orderMapper;
    private final RestTemplate restTemplate;

    private static final String STORAGE_BASE_URL = "http://localhost:8082";
    private static final String ACCOUNT_BASE_URL = "http://localhost:8083";

    public OrderService(OrderMapper orderMapper, RestTemplate restTemplate) {
        this.orderMapper = orderMapper;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Order createOrder(OrderCreateRequest req) {
        Order order = new Order();
        order.setUserId(req.getUserId());
        order.setProductId(req.getProductId());
        order.setCount(req.getCount());
        order.setAmount(req.getAmount());
        order.setStatus(0);
        orderMapper.insert(order);

        // call storage deduct
        DeductRequest deductRequest = new DeductRequest();
        deductRequest.setProductId(req.getProductId());
        deductRequest.setCount(req.getCount());
        CommonResponse<?> storageRes;
        try {
            storageRes = restTemplate.postForObject(STORAGE_BASE_URL + "/api/storage/deduct", deductRequest, CommonResponse.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Storage service error: " + e.getMessage(), e);
        }
        if (storageRes == null || !Boolean.TRUE.equals(storageRes.isSuccess())) {
            throw new RuntimeException("Storage deduct failed: " + (storageRes == null ? "null" : storageRes.getMessage()));
        }

        // call account debit
        DebitRequest debitRequest = new DebitRequest();
        debitRequest.setUserId(req.getUserId());
        debitRequest.setAmount(req.getAmount());
        CommonResponse<?> accountRes;
        try {
            accountRes = restTemplate.postForObject(ACCOUNT_BASE_URL + "/api/account/debit", debitRequest, CommonResponse.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Account service error: " + e.getMessage(), e);
        }
        if (accountRes == null || !Boolean.TRUE.equals(accountRes.isSuccess())) {
            throw new RuntimeException("Account debit failed: " + (accountRes == null ? "null" : accountRes.getMessage()));
        }

        // update order status to finished
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getId, order.getId()).set(Order::getStatus, 1);
        orderMapper.update(null, uw);
        order.setStatus(1);
        return order;
    }

    // DTOs for cross-service calls (local copy)
    public static class DeductRequest {
        private Long productId;
        private Integer count;
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }

    public static class DebitRequest {
        private Long userId;
        private java.math.BigDecimal amount;
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    }
}
