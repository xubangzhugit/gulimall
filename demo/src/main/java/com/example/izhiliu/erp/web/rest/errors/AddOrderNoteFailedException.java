package com.izhiliu.erp.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * @author Harry(yuzh)
 * @since 2019-01-15
 */
public class AddOrderNoteFailedException extends AbstractThrowableProblem {

    public AddOrderNoteFailedException() {
        super(ErrorConstants.ADD_ORDER_NOTE_FAILED_TYPE, "add order note failed.", Status.INTERNAL_SERVER_ERROR);
    }

}
