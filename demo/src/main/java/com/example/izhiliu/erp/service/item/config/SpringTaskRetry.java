package com.izhiliu.erp.service.item.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * @author Seriel
 * @create 2019-09-05 10:10
 **/
@Service
@Slf4j
public class SpringTaskRetry {


    @Retryable(value = {Throwable.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000L,multiplier = 0.0))
    @SneakyThrows(Throwable.class)
    public <T extends Object> T  retryable(Supplier<T> objectUnaryOperator){
        final T o = objectUnaryOperator.get();
        return  o;
    }

    @Recover
    public <T extends Object> T recover( Throwable  e,Supplier<T> objectUnaryOperator) {
        log.error("start recover...{} ,{}",e.getMessage(),e);
        return  null;
    }
}

