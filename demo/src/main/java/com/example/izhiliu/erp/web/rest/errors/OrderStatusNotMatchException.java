package com.izhiliu.erp.web.rest.errors;

import com.izhiliu.core.Exception.AbstractException;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * 修改地址：当前订单所属店铺id在当前用户店铺中不存在
 *
 * @author Harry(yuzh)
 * @since 2019-01-15
 */
public class OrderStatusNotMatchException extends AbstractException {

    public OrderStatusNotMatchException() {
        super(ErrorConstants.ORDER_STATUS_NOT_MATCH_TYPE, "order.status.not.match.exception", Status.INTERNAL_SERVER_ERROR);
        setGlobalization(true);
    }
}
