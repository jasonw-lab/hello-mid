package com.example.seata.at.account.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.account.domain.entity.Account;
import com.example.seata.at.account.domain.mapper.AccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {
    private final AccountMapper accountMapper;

    public AccountService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Transactional
    public void debit(Long userId, BigDecimal amount) {
        LambdaUpdateWrapper<Account> uw = new LambdaUpdateWrapper<>();
        uw.eq(Account::getUserId, userId)
          .ge(Account::getResidue, amount)
          .setSql("used = used + " + amount)
          .setSql("residue = residue - " + amount);
        int updated = accountMapper.update(null, uw);
        if (updated == 0) {
            throw new InsufficientBalanceException("Insufficient balance for userId=" + userId + ", amount=" + amount);
        }
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) { super(message); }
    }
}
