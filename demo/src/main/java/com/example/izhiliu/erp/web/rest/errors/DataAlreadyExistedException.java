package com.izhiliu.erp.web.rest.errors;

import com.izhiliu.core.Exception.AbstractException;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/18 15:12
 */
public class DataAlreadyExistedException extends AbstractException {

    public DataAlreadyExistedException(String detail) {
        super(null, "data already existed.", Status.INTERNAL_SERVER_ERROR, detail);
    }

    public DataAlreadyExistedException(String title, String detail) {
        super(null, title, Status.INTERNAL_SERVER_ERROR, detail);
    }
    public DataAlreadyExistedException(String title, String detail,boolean globalization) {
        super(null, title, Status.INTERNAL_SERVER_ERROR, detail);
        setGlobalization(globalization);
    }
}
