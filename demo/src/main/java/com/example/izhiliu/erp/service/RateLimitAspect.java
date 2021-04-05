package com.izhiliu.erp.service;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.config.BodyValidStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Seriel
 * @create 2019-08-05 11:18
 **/
@Component
@Scope
@Aspect
@Slf4j
public class RateLimitAspect {

    @Resource
    RedisLockHelper redisLockHelper;

    //用来存放不同接口的RateLimiter(key为接口名称，value为RateLimiter)
    private ConcurrentHashMap<String, RateLimiter> map = new ConcurrentHashMap<>();


    private RateLimiter rateLimiter;


    @Pointcut("@annotation(RateLimit)")
    public void serviceLimit() {
    }

    @Around("serviceLimit()&&@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        final boolean isRun;
        Object obj = null;
        //获取拦截的方法名
        Signature sig = joinPoint.getSignature();
        //获取拦截的方法名
        MethodSignature msig = (MethodSignature) sig;
        String functionName = msig.getName(); // 注解所在方法名区分不同的限流策略
        //返回被织入增加处理目标对象
        String parameterName = rateLimit.parameterName();
        if (StringUtils.isNotBlank(parameterName)) {
            final Method method = msig.getMethod();
            Parameter[] parameters = method.getParameters();
            int i = 0;
            for (; i < parameters.length; i++) {
                if (Objects.equals(parameterName, parameters[i].getName())) {
                    break;
                }
            }
            final Object[] args1 = joinPoint.getArgs();
            if (log.isDebugEnabled()) {
                log.info(" Parameter name  [{}]", args1[i]);
            }
            isRun = redisLockHelper.lock("RateLimit:"+ JSONObject.toJSONString(args1[i]),10,TimeUnit.SECONDS);

        } else {
//        final HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
//        final String ipAddress = getIpAddress(request);
            final String currentLogin = SecurityUtils.currentLogin();
            functionName = functionName + currentLogin;
            if (log.isDebugEnabled()) {
                log.info(" functionName  [{}]", functionName);
            }
            //获取注解信息
//            double limitNum = rateLimit.limitNum(); //获取注解每秒加入桶中的token
            double limitNum = 5; //获取注解每秒加入桶中的token
            //获取rateLimiter
            if (map.containsKey(functionName)) {
                rateLimiter = map.get(functionName);
            } else {
                map.put(functionName, RateLimiter.create(limitNum, 1, TimeUnit.SECONDS));
                rateLimiter = map.get(functionName);
            }
            isRun = rateLimiter.tryAcquire();
        }


        try {
            if (isRun) {
                //执行方法
                obj = joinPoint.proceed();
            } else {
                //拒绝了请求（服务降级）
                obj = ResponseEntity.status(500).body(BodyValidStatus.builder().code("500").title("访问太频繁 请休息下再试试吧").field("limit").type("customize").build());
                log.info("拒绝了请求：" + obj);
//                outErrorResult(result);
            }
        } catch (Throwable throwable) {
            log.error(throwable.getMessage(),throwable);
            obj = ResponseEntity.status(500).body(BodyValidStatus.builder().code("500").title(throwable.getMessage()).field(throwable.getLocalizedMessage()).type("customize").build());
        }
        return obj;
    }


    /**
     * 获取Ip地址
     *
     * @param request
     * @return
     */
    private static String getIpAddress(HttpServletRequest request) {
        String Xip = request.getHeader("X-Real-IP");
        String XFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)) {
            //多次反向代理后会有多个ip值，第一个ip才是真实ip
            int index = XFor.indexOf(",");
            if (index != -1) {
                return XFor.substring(0, index);
            } else {
                return XFor;
            }
        }
        XFor = Xip;
        if (StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)) {
            return XFor;
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getRemoteAddr();
        }
        return XFor;
    }


}
