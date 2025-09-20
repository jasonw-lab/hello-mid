package com.example.seata.at.account.service;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.math.BigDecimal;

/**
 * Account の TCC アクション定義インタフェース。
 * - Seata の TCC（2段階コミット）の TRY/CONFIRM/CANCEL を規定します。
 * - prepare が TRY、commit が CONFIRM、cancel が CANCEL に相当します。
 */
@LocalTCC
public interface AccountTccAction {

    /**
     * TRY フェーズ: 残高の凍結を意図した予約を記録します。
     * @param actionContext Seata が付与する文脈（XIDなど）
     * @param userId 対象ユーザID
     * @param amount 凍結（減算）予定額
     */
    @TwoPhaseBusinessAction(name = "accountDebitAction", commitMethod = "commit", rollbackMethod = "cancel")
    boolean prepare(
            BusinessActionContext actionContext,
            @BusinessActionContextParameter(paramName = "userId") Long userId,
            @BusinessActionContextParameter(paramName = "amount") BigDecimal amount
    );

    /**
     * CONFIRM フェーズ: TRY で予約した内容に基づいて減算を確定させます。
     * @param actionContext Seata が渡す文脈（XID 等）
     */
    boolean commit(BusinessActionContext actionContext);

    /**
     * CANCEL フェーズ: TRY の予約を取り消し、最終的な副作用を発生させません。
     * @param actionContext Seata が渡す文脈（XID 等）
     */
    boolean cancel(BusinessActionContext actionContext);
}
