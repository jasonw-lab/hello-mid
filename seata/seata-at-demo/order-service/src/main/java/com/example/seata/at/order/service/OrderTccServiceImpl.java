package com.example.seata.at.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seata.at.order.api.dto.CommonResponse;
import com.example.seata.at.order.api.dto.OrderDTO;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.entity.TccOrder;
import com.example.seata.at.order.domain.mapper.OrderMapper;
import com.example.seata.at.order.domain.mapper.TccOrderMapper;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Order TCC Service Implementation
 */
@Service
public class OrderTccServiceImpl implements OrderTccService {
    private static final Logger log = LoggerFactory.getLogger(OrderTccServiceImpl.class);
//    private final OrderMapper orderMapper;
    private final TccOrderMapper orderMapper;
    private final RestTemplate restTemplate;

    private static final String STORAGE_BASE_URL = "http://localhost:8082";
    private static final String ACCOUNT_BASE_URL = "http://localhost:8083";

    public OrderTccServiceImpl(TccOrderMapper orderMapper, RestTemplate restTemplate) {
//        this.orderMapper = orderMapper;
        this.orderMapper = orderMapper;
        this.restTemplate = restTemplate;
    }

    @Override
//    @GlobalTransactional(name = "order-create-tcc-tx", rollbackFor = Exception.class)
    public TccOrder tryCreate(OrderDTO req, Long orderId) {
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("=== TCC オーダー作成開始 === orderNo={}, userId={}, productId={}, count={}, amount={}, xid={}",
                req.getOrderNo(), req.getUserId(), req.getProductId(), req.getCount(), req.getAmount(), xid);

        // 冪等性チェック
        TccOrder existingTcc = orderMapper.selectOne(new LambdaQueryWrapper<TccOrder>()
                .eq(TccOrder::getXid, xid));
        if (existingTcc != null) {
            log.info("TCCオーダー既存（冪等性）: xid={}, status={}", xid, existingTcc.getStatus());
//            // 既存のオーダーを返す
//            TccOrder tccOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
//                    .eq(Order::getOrderNo, existingTcc.getOrderNo()));
            return existingTcc;
        }

        // TCCオーダー作成（ステータス: PENDING）
        TccOrder tccOrder = new TccOrder();
        tccOrder.setXid(xid);
        tccOrder.setOrderNo(req.getOrderNo());
        tccOrder.setUserId(req.getUserId());
        tccOrder.setProductId(req.getProductId());
        tccOrder.setCount(req.getCount());
        tccOrder.setAmount(req.getAmount());
        tccOrder.setStatus("PENDING"); // PENDING
        orderMapper.insert(tccOrder);
        log.info("TCCオーダー作成完了: xid={}, orderNo={}", xid, req.getOrderNo());

        // Storage TCC Try 呼び出し
        log.info("--- Storage TCC Try 呼び出し ---");
        DeductTccRequest deductReq = new DeductTccRequest();
        deductReq.setOrderNo(req.getOrderNo());
        deductReq.setProductId(req.getProductId());
        deductReq.setCount(req.getCount());
        
        try {
            CommonResponse<?> storageRes = restTemplate.postForObject(
                STORAGE_BASE_URL + "/api/storage/deduct/tcc", deductReq, CommonResponse.class);
            if (storageRes == null || !Boolean.TRUE.equals(storageRes.isSuccess())) {
                throw new RuntimeException("Storage TCC try failed: " + (storageRes == null ? "null" : storageRes.getMessage()));
            }
            log.info("Storage TCC Try 成功");
        } catch (Exception e) {
            log.error("Storage TCC Try 失敗: {}", e.getMessage());
            throw new RuntimeException("Storage TCC try failed: " + e.getMessage(), e);
        }

        // Account TCC Try 呼び出し
        log.info("--- Account TCC Try 呼び出し ---");
        DebitTccRequest debitReq = new DebitTccRequest();
        debitReq.setOrderNo(req.getOrderNo());
        debitReq.setUserId(req.getUserId());
        debitReq.setAmount(req.getAmount());
        
        try {
            CommonResponse<?> accountRes = restTemplate.postForObject(
                ACCOUNT_BASE_URL + "/api/account/debit/tcc", debitReq, CommonResponse.class);
            if (accountRes == null || !Boolean.TRUE.equals(accountRes.isSuccess())) {
                throw new RuntimeException("Account TCC try failed: " + (accountRes == null ? "null" : accountRes.getMessage()));
            }
            log.info("Account TCC Try 成功");
        } catch (Exception e) {
            log.error("Account TCC Try 失敗: {}", e.getMessage());
            throw new RuntimeException("Account TCC try failed: " + e.getMessage(), e);
        }

        // 実際のオーダーを作成（ステータス: 1=確定）
//        Order order = new Order();
//        order.setOrderNo(req.getOrderNo());
//        order.setUserId(req.getUserId());
//        order.setProductId(req.getProductId());
//        order.setCount(req.getCount());
//        order.setAmount(req.getAmount());
//        order.setStatus(1); // 確定
//        orderMapper.insert(order);
        
//        // TCCオーダーを成功に更新
        tccOrder.setStatus("CONFIRMED"); // CONFIRMED
        orderMapper.updateById(tccOrder);
        
        log.info("=== TCC オーダー作成完了 === orderNo={}, id={}", tccOrder.getOrderNo(), tccOrder.getId());

        // 全部成功 → TM commit
        // TM が commit 決定 → storage.confirm / account.confirm / order.confirm 実行
        return tccOrder;
    }

    @Override
    public boolean confirm(BusinessActionContext context) {
        log.info("===  OrderTccServiceImpl confirm ");

        Long orderId = (Long) context.getActionContext("orderId");
        TccOrder order = orderMapper.selectById(orderId);
        order.setStatus("CONFIRMED");
        orderMapper.updateById(order);
        return true;
    }

    @Override
    public boolean cancel(BusinessActionContext context) {
        log.info("===  OrderTccServiceImpl cancel ");
        Long orderId = (Long) context.getActionContext("orderId");
        TccOrder order = orderMapper.selectById(orderId);
        order.setStatus("CANCELLED");
        orderMapper.updateById(order);
        return true;
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