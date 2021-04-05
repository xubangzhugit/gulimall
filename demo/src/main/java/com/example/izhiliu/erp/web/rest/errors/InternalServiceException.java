package com.izhiliu.erp.web.rest.errors;

import com.izhiliu.core.Exception.AbstractException;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/3 10:46
 */
public class InternalServiceException extends AbstractException {

    public InternalServiceException(String detailed) {
        super(null, "internal.service.exception", Status.INTERNAL_SERVER_ERROR, detailed);
    }
}
