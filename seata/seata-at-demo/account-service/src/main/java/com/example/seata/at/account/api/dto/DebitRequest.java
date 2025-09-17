package com.example.seata.at.account.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class DebitRequest {
    @NotNull(message = "userId is required")
    @JsonProperty("userId")
    @JsonAlias({"user_id", "userID"})
    private Long userId;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}