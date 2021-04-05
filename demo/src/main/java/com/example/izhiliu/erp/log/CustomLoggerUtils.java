package com.izhiliu.erp.log;


import com.alibaba.fastjson.JSON;
import org.slf4j.MDC;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author seriel
 */
public final class CustomLoggerUtils {


    public static String isJson(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;

        }
        return JSON.toJSONString(value);
    }


    public static Map<String, String> invokeMetHod(Object args) {
        return Arrays.stream(BeanUtils.getPropertyDescriptors(args.getClass()))
                .filter(pd -> !"class".equals(pd.getName()))
                .collect(HashMap::new,
                        (map, pd) -> map.put(pd.getName(), CustomLoggerUtils.isJson(ReflectionUtils.invokeMethod(pd.getReadMethod(), args))),
                        HashMap::putAll);
    }


    public static void fill(Consumer<String> supplier, Consumer<Throwable> throwableConsumer) {
        Throwable isThrowable = null;
        final String val = UUID.randomUUID().toString();
        try {
            supplier.accept("trace");
        } catch (Throwable e) {
            throwableConsumer.accept(e);
            isThrowable = e;
        } finally {
        }
        if (Objects.nonNull(isThrowable)) {
            throw new RuntimeException(isThrowable.getMessage() + " ");
        }
    }

}
