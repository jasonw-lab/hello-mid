package com.example.seata.at.order.service;

import com.example.seata.at.order.api.dto.OrderDTO;
import com.example.seata.at.order.domain.entity.TccOrder;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * Order TCC Service Interface
 */
//public interface OrderTccService {
//
//    /**
//     * TCC モードで注文を作成する
//     * @param req 注文作成リクエスト
//     * @return 作成された注文
//     */
//    Order tryCreate(OrderCreateRequest req);
//}

public interface OrderTccService {
    @TwoPhaseBusinessAction(name = "OrderTccAction", commitMethod = "confirm", rollbackMethod = "cancel")
    TccOrder tryCreate(OrderDTO orderDTO, @BusinessActionContextParameter(paramName = "orderId") Long orderId);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}
