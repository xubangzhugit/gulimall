package com.izhiliu.erp.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * @author Harry(yuzh)
 * @since 2019-01-17
 */
public class SystemIsBusyException extends AbstractThrowableProblem {

    public SystemIsBusyException() {
        super(ErrorConstants.SYSTEM_IS_BUSY_EXCEPTION_TYPE, "正在为您同步订单，请稍后再试~ ", Status.INTERNAL_SERVER_ERROR);
    }
}
