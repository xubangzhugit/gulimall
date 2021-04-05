package com.izhiliu.erp.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * @author harry
 * @since 2019-04-29 15:42
 */
public class UnLoginException extends AbstractThrowableProblem {

    private static final long serialVersionUID = -4884769510077842637L;

    public UnLoginException() {
        super(ErrorConstants.DEFAULT_TYPE, "未登录！", Status.INTERNAL_SERVER_ERROR, "");
    }
}
