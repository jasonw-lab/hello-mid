package com.example.seata.at.account.api;

import com.example.seata.at.account.api.dto.CommonResponse;
import com.example.seata.at.account.api.dto.DebitRequest;
import com.example.seata.at.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/debit")
    public CommonResponse<String> debit(@Valid @RequestBody DebitRequest req) {
        accountService.debit(req.getUserId(), req.getAmount());
        return CommonResponse.ok("debited");
    }

    @ExceptionHandler(AccountService.InsufficientBalanceException.class)
    public ResponseEntity<CommonResponse<Void>> handleBalance(AccountService.InsufficientBalanceException ex) {
        return ResponseEntity.badRequest().body(CommonResponse.fail(ex.getMessage()));
    }
}
