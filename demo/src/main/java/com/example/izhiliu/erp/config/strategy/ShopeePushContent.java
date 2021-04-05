package com.izhiliu.erp.config.strategy;

import com.izhiliu.erp.service.item.BaseShopeePushService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: louis
 * @Date: 2020/7/14 16:50
 */
@Component
public class ShopeePushContent {

    public static Map<String, BaseShopeePushService> pushServiceMap = new ConcurrentHashMap<>();

    public BaseShopeePushService getContent(String type) {
        return pushServiceMap.get(type);
    }
}
