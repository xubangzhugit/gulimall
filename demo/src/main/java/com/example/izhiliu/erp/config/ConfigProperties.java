package com.izhiliu.erp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author Twilight
 * @date 2021/3/8 10:08
 */
@Data
@ConfigurationProperties(prefix = "config", ignoreUnknownFields = false)
public class ConfigProperties {
    private List<Dir> dir;
    @Data
    public static class Dir{
        private String key;
        private String value;
    }
}
