package com.izhiliu.erp.web.rest.errors;

import com.izhiliu.core.Exception.AbstractException;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * 错误的订单状态
 *
 * @author Harry(yuzh)
 * @since 2019-01-15
 */
public class OrderStatusIsNotExistException extends AbstractException {

    public OrderStatusIsNotExistException(String message) {
        super(ErrorConstants.ORDER_STATUS_IS_NOT_EXIST_TYPE, "order.status.not.exist.exception", Status.BAD_REQUEST,message);
        setGlobalization(true);
    }
}
