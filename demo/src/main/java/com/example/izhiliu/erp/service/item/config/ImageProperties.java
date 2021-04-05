package com.izhiliu.erp.service.item.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Seriel
 * @create 2019-09-05 14:16
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "image", ignoreUnknownFields = false)
public class ImageProperties {

    /**
     * 图片报错 上限
     */
    private int  imageErrorSizeMax = 3;
    /**
     * 每次处理的 大小
     */
    private int  imageRunSize = 9;
    /**
     * 重试次数
     */
    public static final int RETRY_MAX_SIE = 3;
}
