package com.example.seata.at.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seata.at.storage.domain.entity.StorageTccFreeze;
import com.example.seata.at.storage.domain.mapper.StorageTccFreezeMapper;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StorageTccActionImpl implements StorageTccAction {
    private static final Logger log = LoggerFactory.getLogger(StorageTccActionImpl.class);

    private final StorageService storageService;
    private final StorageTccFreezeMapper freezeMapper;

    public StorageTccActionImpl(StorageService storageService, StorageTccFreezeMapper freezeMapper) {
        this.storageService = storageService;
        this.freezeMapper = freezeMapper;
    }

    @Override
    @Transactional
    public boolean prepare(BusinessActionContext actionContext, Long productId, Integer count) {
        String xid = actionContext != null ? actionContext.getXid() : io.seata.core.context.RootContext.getXID();
        if (xid == null || xid.isBlank()) {
            // When called outside a Seata global transaction, there is no XID bound to the context.
            // Generate a local XID to satisfy the DB NOT NULL constraint and keep logs traceable.
            xid = "local-" + java.util.UUID.randomUUID();
            log.warn("[TCC-Storage] TRY without global transaction. Generated local xid={}", xid);
        }
        log.info("[TCC-Storage] TRY prepare reserve: xid={}, productId={}, count={}", xid, productId, count);
        StorageTccFreeze existing = freezeMapper.selectOne(new LambdaQueryWrapper<StorageTccFreeze>().eq(StorageTccFreeze::getXid, xid));
        if (existing != null) {
            Integer status = existing.getStatus();
            if (status != null && status == 2) {
                log.warn("[TCC-Storage] TRY rejected due to previous CANCEL (anti-suspension). xid={}", xid);
                throw new IllegalStateException("Try suspended: already canceled");
            }
            log.info("[TCC-Storage] TRY idempotent: record already exists. xid={}", xid);
            return true;
        }
        StorageTccFreeze freeze = new StorageTccFreeze();
        freeze.setXid(xid);
        freeze.setProductId(productId);
        freeze.setCount(count);
        freeze.setStatus(0);
        freezeMapper.insert(freeze);
        return true;
    }

    @Override
    @Transactional
    public boolean commit(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        StorageTccFreeze freeze = freezeMapper.selectOne(new LambdaQueryWrapper<StorageTccFreeze>().eq(StorageTccFreeze::getXid, xid));
        if (freeze == null) {
            log.info("[TCC-Storage] COMMIT: no freeze found (treat success). xid={}", xid);
            return true;
        }
        Integer status = freeze.getStatus();
        if (status != null && status == 1) {
            log.info("[TCC-Storage] COMMIT idempotent: already committed. xid={}", xid);
            return true;
        }
        if (status != null && status == 2) {
            log.info("[TCC-Storage] COMMIT skipped: already canceled. xid={}", xid);
            return true;
        }
        int updated = freezeMapper.update(null, new LambdaUpdateWrapper<StorageTccFreeze>()
                .eq(StorageTccFreeze::getXid, xid)
                .eq(StorageTccFreeze::getStatus, 0)
                .set(StorageTccFreeze::getStatus, 1));
        if (updated == 0) {
            log.info("[TCC-Storage] COMMIT: state already changed by another worker. xid={}", xid);
            return true;
        }
        log.info("[TCC-Storage] COMMIT executing DB deduct: xid={}, productId={}, count={}", xid, freeze.getProductId(), freeze.getCount());
        storageService.deduct(freeze.getProductId(), freeze.getCount());
        return true;
    }

    @Override
    @Transactional
    public boolean cancel(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        StorageTccFreeze freeze = freezeMapper.selectOne(new LambdaQueryWrapper<StorageTccFreeze>().eq(StorageTccFreeze::getXid, xid));
        if (freeze == null) {
            StorageTccFreeze canceled = new StorageTccFreeze();
            canceled.setXid(xid);
            canceled.setStatus(2);
            freezeMapper.insert(canceled);
            log.info("[TCC-Storage] CANCEL: inserted cancel marker for empty rollback. xid={}", xid);
            return true;
        }
        if (freeze.getStatus() != null && freeze.getStatus() == 2) {
            log.info("[TCC-Storage] CANCEL idempotent: already canceled. xid={}", xid);
            return true;
        }
        if (freeze.getStatus() != null && freeze.getStatus() == 1) {
            log.info("[TCC-Storage] CANCEL skipped: already committed. xid={}", xid);
            return true;
        }
        freezeMapper.update(null, new LambdaUpdateWrapper<StorageTccFreeze>()
                .eq(StorageTccFreeze::getXid, xid)
                .set(StorageTccFreeze::getStatus, 2));
        log.info("[TCC-Storage] CANCEL: reservation marked canceled. xid={}", xid);
        return true;
    }
}
