package com.example.seata.at.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.account.domain.entity.AccountTccFreeze;
import com.example.seata.at.account.domain.mapper.AccountTccFreezeMapper;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Account の TCC 実装クラス。
 * - TRY では凍結テーブルにレコードを挿入（または既存確認）し、サスペンション対策・冪等性を担保します。
 * - CONFIRM では状態を 0→1 に更新してから実際の減算（debit）を行います（重複実行防止）。
 * - CANCEL では空振りにも対応し、必要に応じてキャンセルマーカーを挿入します。
 */
@Component
public class AccountTccActionImpl implements AccountTccAction {
    private static final Logger log = LoggerFactory.getLogger(AccountTccActionImpl.class);

    private final AccountService accountService;
    private final AccountTccFreezeMapper freezeMapper;

    public AccountTccActionImpl(AccountService accountService, AccountTccFreezeMapper freezeMapper) {
        this.accountService = accountService;
        this.freezeMapper = freezeMapper;
    }

    @Override
    @Transactional
    public boolean prepare(BusinessActionContext actionContext, Long userId, BigDecimal amount) {
        String xid = actionContext != null ? actionContext.getXid() : io.seata.core.context.RootContext.getXID();
        log.info("[TCC-Account] TRY prepare reserve: xid={}, userId={}, amount={}", xid, userId, amount);
        // サスペンション対策: 事前にCANCELが存在する場合はTRYを拒否する
        AccountTccFreeze existing = freezeMapper.selectOne(new LambdaQueryWrapper<AccountTccFreeze>().eq(AccountTccFreeze::getXid, xid));
        if (existing != null) {
            Integer status = existing.getStatus();
            if (status != null && status == 2) {
                log.warn("[TCC-Account] TRY rejected due to previous CANCEL (anti-suspension). xid={}", xid);
                throw new IllegalStateException("Try suspended: already canceled");
            }
            // 冪等性: 既に同一XIDのレコードがある場合は成功として返す（重複TRYを許容）
            log.info("[TCC-Account] TRY idempotent: record already exists. xid={}", xid);
            return true;
        }
        AccountTccFreeze freeze = new AccountTccFreeze();
        freeze.setXid(xid);
        freeze.setUserId(userId);
        freeze.setAmount(amount);
        freeze.setStatus(0);
        freezeMapper.insert(freeze);
        return true;
    }

    @Override
    @Transactional
    public boolean commit(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        AccountTccFreeze freeze = freezeMapper.selectOne(new LambdaQueryWrapper<AccountTccFreeze>().eq(AccountTccFreeze::getXid, xid));
        if (freeze == null) {
            log.info("[TCC-Account] COMMIT: no freeze found (treat success). xid={}", xid);
            return true;
        }
        Integer status = freeze.getStatus();
        if (status != null && status == 1) {
            log.info("[TCC-Account] COMMIT idempotent: already committed. xid={}", xid);
            return true;
        }
        if (status != null && status == 2) {
            log.info("[TCC-Account] COMMIT skipped: already canceled. xid={}", xid);
            return true;
        }
        // 状態遷移によるロック: 0→1 への原子的更新で二重の減算を防止
        int updated = freezeMapper.update(null, new LambdaUpdateWrapper<AccountTccFreeze>()
                .eq(AccountTccFreeze::getXid, xid)
                .eq(AccountTccFreeze::getStatus, 0)
                .set(AccountTccFreeze::getStatus, 1));
        if (updated == 0) {
            log.info("[TCC-Account] COMMIT: state already changed by another worker. xid={}", xid);
            return true;
        }
        log.info("[TCC-Account] COMMIT executing DB debit: xid={}, userId={}, amount={}", xid, freeze.getUserId(), freeze.getAmount());
        accountService.debit(freeze.getUserId(), freeze.getAmount());
        return true;
    }

    @Override
    @Transactional
    public boolean cancel(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        AccountTccFreeze freeze = freezeMapper.selectOne(new LambdaQueryWrapper<AccountTccFreeze>().eq(AccountTccFreeze::getXid, xid));
        if (freeze == null) {
            // 空振り（Empty Rollback）対応: TRYが未実行でもCANCELが先に来た場合にキャンセルマーカーを挿入
            AccountTccFreeze canceled = new AccountTccFreeze();
            canceled.setXid(xid);
            canceled.setStatus(2);
            freezeMapper.insert(canceled);
            log.info("[TCC-Account] CANCEL: inserted cancel marker for empty rollback. xid={}", xid);
            return true;
        }
        if (freeze.getStatus() != null && freeze.getStatus() == 2) {
            log.info("[TCC-Account] CANCEL idempotent: already canceled. xid={}", xid);
            return true;
        }
        // 既にCOMMIT済みであれば何もしない（CANCELはスキップ）
        if (freeze.getStatus() != null && freeze.getStatus() == 1) {
            log.info("[TCC-Account] CANCEL skipped: already committed. xid={}", xid);
            return true;
        }
        freezeMapper.update(null, new LambdaUpdateWrapper<AccountTccFreeze>()
                .eq(AccountTccFreeze::getXid, xid)
                .set(AccountTccFreeze::getStatus, 2));
        log.info("[TCC-Account] CANCEL: reservation marked canceled. xid={}", xid);
        return true;
    }
}
