package com.izhiliu.erp.web.rest.errors;

import com.izhiliu.core.Exception.AbstractException;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

/**
 * describe: 数据不存在异常
 * <p>
 *
 * @author cheng
 * @date 2019/1/12 9:31
 */
public class DataNotFoundException extends AbstractException {


    public DataNotFoundException(String title) {
        this(title,false);
    }

    /**
     *
     * @param title
     * @param globalization  是否国际化
     */
    public DataNotFoundException(String title,boolean globalization) {
        super(null, title, Status.INTERNAL_SERVER_ERROR);
        setGlobalization(globalization);
    }

    public DataNotFoundException(String title,String[] strings) {
        super(null, title, Status.INTERNAL_SERVER_ERROR);
        setParam(strings);
        setGlobalization(true);
    }

    public DataNotFoundException(String title, String detail) {
        super(null, title, Status.INTERNAL_SERVER_ERROR, detail);
    }

    public DataNotFoundException(String title, String detail,boolean globalization) {
        super(null, title, Status.INTERNAL_SERVER_ERROR, detail);
        setGlobalization(true);
    }
    public DataNotFoundException(String title, String detail,String[] strings) {
        super(null, title, Status.INTERNAL_SERVER_ERROR, detail);
        setParam(strings);
        setGlobalization(true);
    }
}
