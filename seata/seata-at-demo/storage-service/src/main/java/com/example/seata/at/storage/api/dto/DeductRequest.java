package com.example.seata.at.storage.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class DeductRequest {
    @NotNull(message = "productId is required")
    @JsonProperty("productId")
    @JsonAlias({"product_id", "productID"})
    private Long productId;

    @NotNull(message = "count is required")
    private Integer count;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}
