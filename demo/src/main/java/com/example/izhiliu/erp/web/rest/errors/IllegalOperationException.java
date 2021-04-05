package com.izhiliu.erp.web.rest.errors;

import com.izhiliu.core.Exception.AbstractException;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * describe: 非法操作
 * <p>
 *
 * @author cheng
 * @date 2019/1/23 17:03
 */
public class IllegalOperationException extends AbstractException {

    public IllegalOperationException(String title) {
        super(null, title, Status.INTERNAL_SERVER_ERROR, null);
    }

    public IllegalOperationException(String title,boolean globalization) {
        super(null, title, Status.INTERNAL_SERVER_ERROR, null);
        setGlobalization(globalization);
    }

    public IllegalOperationException(String title, String detail) {
        super(null, title, Status.INTERNAL_SERVER_ERROR, detail);
    }


    public IllegalOperationException(String title,String[] param) {
        super(null, title, Status.INTERNAL_SERVER_ERROR, null);
        setParam(param);
        setGlobalization(true);
    }

    public IllegalOperationException(String title, String detail,String[] param) {
        super(null, title, Status.INTERNAL_SERVER_ERROR, detail);
        setParam(param);
        setGlobalization(true);
    }
}
