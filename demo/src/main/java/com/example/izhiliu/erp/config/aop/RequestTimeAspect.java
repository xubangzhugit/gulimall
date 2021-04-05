package com.izhiliu.erp.config.aop;

import lombok.Data;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;


/**
 * 记录请求日志
 *
 * @author cheng
 * @date 2018/8/8
 * @time 16:40
 */

@Aspect
@Order(1)
@Component
public class RequestTimeAspect {

    private static final Logger log = LoggerFactory.getLogger(RequestTimeAspect.class);

    private ThreadLocal<ApiTimeConsuming> threadLocal = new ThreadLocal<>();

    @Pointcut("execution(public * com.izhiliu.erp.web.rest.item.*Resource.*(..))")
    public void requestLog() {
    }

    @Before("requestLog()")
    public void doBefore(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        final ApiTimeConsuming consuming = new ApiTimeConsuming();
        consuming.setStartTime(System.currentTimeMillis());
        consuming.setUrl(request.getRequestURI());
        consuming.setIp(request.getRemoteAddr());
        consuming.setLogin(request.getRemoteUser());
        threadLocal.set(consuming);
    }

    /**
     * 出现异常了可能会出事情, 这里不被执行, 当前线程的 RequestLog 不会被清除、日志也不会打印。
     *
     * @param result
     */
    @AfterReturning(returning = "result", pointcut = "requestLog()")
    public void doAfterReturn(Object result) {
        long endTime = System.currentTimeMillis();
        final ApiTimeConsuming apiTimeConsuming = threadLocal.get();
        apiTimeConsuming.setTimeConsuming(endTime - apiTimeConsuming.getStartTime());

        log.info(apiTimeConsuming.toString());
        threadLocal.remove();
    }

    @Data
    public static class ApiTimeConsuming implements Serializable {
        private static final long serialVersionUID = -5666780975311166878L;

        private String ip;
        private String api;
        private String url;
        private String login;
        private Long startTime;
        private Long timeConsuming;

        @Override
        public String toString() {
            return "[api cost time] "+ip+" "+login+" "+ url + " : " + timeConsuming;
        }
    }
}
