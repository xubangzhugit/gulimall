package com.izhiliu.erp.config.converter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/30 13:58
 */
@Component
public class StringToInstant implements Converter<String, LocalDateTime> {

    @Override
    public LocalDateTime convert(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
