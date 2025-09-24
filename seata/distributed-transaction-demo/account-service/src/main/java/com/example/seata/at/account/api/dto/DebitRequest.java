package com.example.seata.at.account.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class DebitRequest {
    @NotNull(message = "userId is required")
    @JsonProperty("userId")
    @JsonAlias({"user_id", "userID"})
    @BusinessActionContextParameter(paramName = "userId")
    private Long userId;

    @NotNull(message = "amount is required")
    @BusinessActionContextParameter(paramName = "amount")
    private BigDecimal amount;

//    // Optional business key for TCC orchestration
//    @BusinessActionContextParameter(paramName = "orderNo")
//    private String orderNo;

    // New: Numeric orderId to be persisted in tcc_account
    @BusinessActionContextParameter(paramName = "orderId")
    private Long orderId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }


    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
}