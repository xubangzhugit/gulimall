package com.izhiliu.erp.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * 修改地址：当前订单所属店铺id在当前用户店铺中不存在
 *
 * @author Harry(yuzh)
 * @since 2019-01-15
 */
public class OrderNotMatchException extends AbstractThrowableProblem {

    public OrderNotMatchException(String message ) {
        super(ErrorConstants.ORDER_NOT_MATCH_TYPE, message, Status.BAD_REQUEST);
    }
}
