package com.example.seata.at.storage.service;

import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Storage の TCC を呼び出す薄いサービスクラス。
 * - 実際の Try/Confirm/Cancel の本体は StorageTccAction（Seata のコールバック対象）が担います。
 * - 本クラスはビジネス層から TRY を起動するためのエントリのみ提供します。
 */
@Service
public class StorageTccService {
    private static final Logger log = LoggerFactory.getLogger(StorageTccService.class);

    private final StorageTccAction storageTccAction;

    public StorageTccService(StorageTccAction storageTccAction) {
        this.storageTccAction = storageTccAction;
    }

    /**
         * TRY フェーズの起動。
         * - 実体は StorageTccAction#prepare に委譲され、Seata により後続の Confirm/Cancel がコールバックされます。
         */
        public void tryDeduct(Long productId, Integer count, String orderNo) {
        String xid = null;
        try { xid = RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("[TCC-TRY][STORAGE] xid={}, orderNo={}, productId={}, count={}", xid, orderNo, productId, count);
        storageTccAction.prepare(null, productId, count);
    }

    /**
         * CONFIRM フェーズ（参考）。
         * - 実運用では Seata が自動で commit コールバックするため、ここではログのみ。
         */
        public void confirmDeduct(Long productId, Integer count, String orderNo) {
        String xid = null;
        try { xid = RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("[TCC-CONFIRM][STORAGE] xid={}, orderNo={}, productId={}, count={}", xid, orderNo, productId, count);
    }

    public void cancelDeduct(Long productId, Integer count, String orderNo) {
        String xid = null;
        try { xid = RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("[TCC-CANCEL][STORAGE] xid={}, orderNo={}, productId={}, count={}", xid, orderNo, productId, count);
    }
}
