package com.example.seata.at.storage.service;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * Storage の TCC アクション定義インタフェース。
 * - Seata の TCC（2段階コミット）の TRY/CONFIRM/CANCEL を規定します。
 * - prepare が TRY、commit が CONFIRM、cancel が CANCEL に相当します。
 */
@LocalTCC
public interface StorageTccAction {

    /**
     * TRY フェーズ: 在庫引当の予約を記録します。
     * @param actionContext Seata が付与する文脈（XIDなど）
     * @param productId 対象プロダクトID
     * @param count 引当予定数
     */
    @TwoPhaseBusinessAction(name = "storageDeductAction", commitMethod = "commit", rollbackMethod = "cancel")
    boolean prepare(
            BusinessActionContext actionContext,
            @BusinessActionContextParameter(paramName = "productId") Long productId,
            @BusinessActionContextParameter(paramName = "count") Integer count
    );

    /**
     * CONFIRM フェーズ: TRY で予約した内容に基づいて在庫を確定（減算）します。
     * @param actionContext Seata が渡す文脈（XID 等）
     */
    boolean commit(BusinessActionContext actionContext);

    /**
     * CANCEL フェーズ: TRY の予約を取り消し、在庫の副作用を発生させません。
     * @param actionContext Seata が渡す文脈（XID 等）
     */
    boolean cancel(BusinessActionContext actionContext);
}
