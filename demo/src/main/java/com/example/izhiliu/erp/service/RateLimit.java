package com.izhiliu.erp.service;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.*;

@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String parameterName() default  "";
    double value()  default 20;
    double limitNum() default 20;  //默认每秒放入桶中的token


}