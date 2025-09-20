package com.example.seata.at.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.order.api.dto.CommonResponse;
import com.example.seata.at.order.api.dto.OrderCreateRequest;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.mapper.OrderMapper;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderTccService {
    private static final Logger log = LoggerFactory.getLogger(OrderTccService.class);
    private final OrderMapper orderMapper;
    private final RestTemplate restTemplate;

    private static final String STORAGE_BASE_URL = "http://localhost:8082";
    private static final String ACCOUNT_BASE_URL = "http://localhost:8083";

    public OrderTccService(OrderMapper orderMapper, RestTemplate restTemplate) {
        this.orderMapper = orderMapper;
        this.restTemplate = restTemplate;
    }

    @GlobalTransactional(name = "order-create-tcc-tx", rollbackFor = Exception.class)
    public Order createOrderTcc(OrderCreateRequest req) {
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Start createOrderTcc: orderNo={}, userId={}, productId={}, count={}, amount={}, xid={}",
                req.getOrderNo(), req.getUserId(), req.getProductId(), req.getCount(), req.getAmount(), xid);

        // Idempotency check
        Order existing = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, req.getOrderNo()));
        if (existing != null) {
            log.info("Idempotent(TCC): order already exists. orderNo={}, id={}, status={}", existing.getOrderNo(), existing.getId(), existing.getStatus());
            return existing;
        }

        // Create order pending
        Order order = new Order();
        order.setOrderNo(req.getOrderNo());
        order.setUserId(req.getUserId());
        order.setProductId(req.getProductId());
        order.setCount(req.getCount());
        order.setAmount(req.getAmount());
        order.setStatus(0);
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException dup) {
            Order dupOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, req.getOrderNo()));
            if (dupOrder != null) { return dupOrder; }
            throw dup;
        }

        // Call storage TCC try
        DeductTccRequest deductReq = new DeductTccRequest();
        deductReq.setOrderNo(req.getOrderNo());
        deductReq.setProductId(req.getProductId());
        deductReq.setCount(req.getCount());
        CommonResponse<?> storageRes;
        try {
            storageRes = restTemplate.postForObject(STORAGE_BASE_URL + "/api/storage/deduct/tcc", deductReq, CommonResponse.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Storage service TCC error: " + e.getMessage(), e);
        }
        if (storageRes == null || !Boolean.TRUE.equals(storageRes.isSuccess())) {
            throw new RuntimeException("Storage TCC try failed: " + (storageRes == null ? "null" : storageRes.getMessage()));
        }

        // Call account TCC try
        DebitTccRequest debitReq = new DebitTccRequest();
        debitReq.setOrderNo(req.getOrderNo());
        debitReq.setUserId(req.getUserId());
        debitReq.setAmount(req.getAmount());
        CommonResponse<?> accountRes;
        try {
            accountRes = restTemplate.postForObject(ACCOUNT_BASE_URL + "/api/account/debit/tcc", debitReq, CommonResponse.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Account service TCC error: " + e.getMessage(), e);
        }
        if (accountRes == null || !Boolean.TRUE.equals(accountRes.isSuccess())) {
            throw new RuntimeException("Account TCC try failed: " + (accountRes == null ? "null" : accountRes.getMessage()));
        }

        // Mark confirmed
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getId, order.getId()).set(Order::getStatus, 1);
        orderMapper.update(null, uw);
        order.setStatus(1);
        log.info("createOrderTcc success. orderNo={}, id={}", order.getOrderNo(), order.getId());
        return order;
    }

    // Local DTOs for cross-service TCC endpoints
    public static class DeductTccRequest {
        private String orderNo;
        private Long productId;
        private Integer count;
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
    public static class DebitTccRequest {
        private String orderNo;
        private Long userId;
        private java.math.BigDecimal amount;
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    }
}
