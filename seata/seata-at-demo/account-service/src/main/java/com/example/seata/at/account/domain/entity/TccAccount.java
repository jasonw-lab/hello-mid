package com.example.seata.at.account.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TCC Account エンティティ
 */
@Data
@TableName("tcc_account")
public class TccAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String xid;
    
    private Long userId;
    
    private BigDecimal total;
    
    private BigDecimal used;
    
    private BigDecimal residue;
    
    private BigDecimal frozen;
    
    private Integer status; // 0: PENDING, 1: SUCCESS, 2: FAILED
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
