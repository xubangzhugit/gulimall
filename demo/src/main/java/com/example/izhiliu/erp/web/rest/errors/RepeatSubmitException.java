package com.izhiliu.erp.web.rest.errors;

import com.izhiliu.core.Exception.AbstractException;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/18 14:37
 */
public class RepeatSubmitException extends AbstractException {

    public RepeatSubmitException(String title) {
        super(ErrorConstants.REPEAT_SUBMIT, title, Status.INTERNAL_SERVER_ERROR, null);
    }
    public RepeatSubmitException(String title,boolean globalization) {
        super(ErrorConstants.REPEAT_SUBMIT, title, Status.INTERNAL_SERVER_ERROR, null,globalization);
    }

    /**
     *
     * @param title  返回显示的
     * @param globalizationParam 国际化附带的参数
     */
    public RepeatSubmitException(String title,String[] globalizationParam ) {
        super(ErrorConstants.REPEAT_SUBMIT, title, Status.INTERNAL_SERVER_ERROR, null);
        setParam(globalizationParam);
        setGlobalization(true);
    }

    public RepeatSubmitException(String title, String detail) {
        super(ErrorConstants.REPEAT_SUBMIT, title, Status.INTERNAL_SERVER_ERROR, detail);
    }
}
