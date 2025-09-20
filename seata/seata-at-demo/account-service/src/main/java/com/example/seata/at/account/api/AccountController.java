package com.example.seata.at.account.api;

import com.example.seata.at.account.api.dto.CommonResponse;
import com.example.seata.at.account.api.dto.DebitRequest;
import com.example.seata.at.account.service.AccountService;
import com.example.seata.at.account.service.AccountTccService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;
    private final AccountTccService accountTccService;

    public AccountController(AccountService accountService, AccountTccService accountTccService) {
        this.accountService = accountService;
        this.accountTccService = accountTccService;
    }

    @PostMapping("/debit")
    public CommonResponse<String> debit(@Valid @RequestBody DebitRequest req) {
        // Log received request body at INFO level
        log.info("Received DebitRequest: userId={}, amount={}", req.getUserId(), req.getAmount());
        accountService.debit(req.getUserId(), req.getAmount());
        return CommonResponse.ok("debited");
    }

    @PostMapping("/debit/tcc")
    public CommonResponse<String> debitTcc(@Valid @RequestBody DebitRequest req) {
        String orderNo = req.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            orderNo = java.util.UUID.randomUUID().toString();
        }
        log.info("Received DebitRequest TCC: orderNo={}, userId={}, amount={}", orderNo, req.getUserId(), req.getAmount());
        accountTccService.tryDebit(req.getUserId(), req.getAmount(), orderNo);
        return CommonResponse.ok("tcc-try-debited");
    }

    @ExceptionHandler(AccountService.InsufficientBalanceException.class)
    public ResponseEntity<CommonResponse<Void>> handleBalance(AccountService.InsufficientBalanceException ex) {
        return ResponseEntity.badRequest().body(CommonResponse.fail(ex.getMessage()));
    }
}
