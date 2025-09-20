package com.example.seata.at.account.service;

import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Account の TCC を呼び出す薄いサービスクラス。
 * - 実際の Try/Confirm/Cancel の本体は AccountTccAction（Seata のコールバック対象）が担います。
 * - 本クラスはビジネス層から TRY を起動するためのエントリのみ提供します。
 */
@Service
public class AccountTccService {
    private static final Logger log = LoggerFactory.getLogger(AccountTccService.class);

    private final AccountTccAction accountTccAction;

    public AccountTccService(AccountTccAction accountTccAction) {
        this.accountTccAction = accountTccAction;
    }

    /**
     * TRY フェーズの起動。
     * - 実体は AccountTccAction#prepare に委譲され、Seata により後続の Confirm/Cancel がコールバックされます。
     */
    public void tryDebit(Long userId, BigDecimal amount, String orderNo) {
        String xid = null;
        try { xid = RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("[TCC-TRY][ACCOUNT] xid={}, orderNo={}, userId={}, amount={}", xid, orderNo, userId, amount);
        accountTccAction.prepare(null, userId, amount);
    }

    /**
     * CONFIRM フェーズ（参考）。
     * - 実運用では Seata が自動で commit コールバックするため、ここではログのみ。
     */
    public void confirmDebit(Long userId, BigDecimal amount, String orderNo) {
        String xid = null;
        try { xid = RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("[TCC-CONFIRM][ACCOUNT] xid={}, orderNo={}, userId={}, amount={}", xid, orderNo, userId, amount);
    }

    /**
     * CANCEL フェーズ（参考）。
     * - 実運用では Seata が自動で cancel コールバックするため、ここではログのみ。
     */
    public void cancelDebit(Long userId, BigDecimal amount, String orderNo) {
        String xid = null;
        try { xid = RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("[TCC-CANCEL][ACCOUNT] xid={}, orderNo={}, userId={}, amount={}", xid, orderNo, userId, amount);
    }
}
