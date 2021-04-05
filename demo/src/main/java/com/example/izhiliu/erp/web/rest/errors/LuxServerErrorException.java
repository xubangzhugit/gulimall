package com.izhiliu.erp.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * @Author: louis
 * @Date: 2020/7/15 14:04
 */
public class LuxServerErrorException extends AbstractThrowableProblem {

    public LuxServerErrorException(String title) {
        super(null, title, Status.INTERNAL_SERVER_ERROR);
    }

    public LuxServerErrorException(String title, String message) {
        super(ErrorConstants.DEFAULT_TYPE, title, Status.INTERNAL_SERVER_ERROR, message);
    }
}
