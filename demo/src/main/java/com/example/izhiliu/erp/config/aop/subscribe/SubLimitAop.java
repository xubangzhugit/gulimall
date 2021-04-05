package com.izhiliu.erp.config.aop.subscribe;

import com.izhiliu.core.config.internation.InternationUtils;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.config.subscribe.AbstractSubLimit;
import com.izhiliu.core.config.subscribe.Enum.SubLimitAopEnums;
import com.izhiliu.core.config.subscribe.SubLimitAnnotation;
import com.izhiliu.erp.web.rest.errors.SubLimitException;
import com.izhiliu.feign.client.DariusService;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Author: louis
 * @Date: 2019/7/13 18:05
 */
@Aspect
@Component
public class SubLimitAop {

    private final Logger log = LoggerFactory.getLogger(SubLimitAop.class);


    @Resource
    private SubLimitService subLimitService;


    @Value("${isLimit}")
    private boolean isLimit;

    @Before(value = "@annotation(subLimitAnnotation)")
    public void handleLimit(JoinPoint joinPoint, SubLimitAnnotation subLimitAnnotation) throws Exception {
        final String currentLogin = SecurityUtils.currentLogin();
             subLimitService.handleLimit(currentLogin,subLimitAnnotation.limitProduct());
    }


    @AfterThrowing(pointcut = "@annotation(subLimitAnnotation)", throwing = "e")
    public void doAfterThrowing(JoinPoint point, SubLimitAnnotation subLimitAnnotation, Throwable e) throws NoSuchMethodException {
        final String currentLogin = SecurityUtils.currentLogin();
        subLimitService.doAfterThrowing(currentLogin,subLimitAnnotation.limitProduct());
    }


}
