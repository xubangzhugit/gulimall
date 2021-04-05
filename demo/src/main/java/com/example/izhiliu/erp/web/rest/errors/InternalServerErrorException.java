package com.izhiliu.erp.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * Simple exception with a message, that returns an Internal Server Error code.
 */
public class InternalServerErrorException extends AbstractThrowableProblem {

    private static final long serialVersionUID = 1L;

    /*public InternalServerErrorException(String message) {
        super(ErrorConstants.DEFAULT_TYPE, message, Status.INTERNAL_SERVER_ERROR);
    }*/


    public InternalServerErrorException(String title, String message) {
        super(ErrorConstants.DEFAULT_TYPE, title, Status.INTERNAL_SERVER_ERROR, message);
    }

    public InternalServerErrorException(String title) {
        super(ErrorConstants.DEFAULT_TYPE, title, Status.INTERNAL_SERVER_ERROR, "");
    }

}
