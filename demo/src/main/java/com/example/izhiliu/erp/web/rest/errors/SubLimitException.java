package com.izhiliu.erp.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;


public class SubLimitException extends AbstractThrowableProblem {


    public SubLimitException(String title) {
        super(null, title, Status.INTERNAL_SERVER_ERROR);
    }

}
