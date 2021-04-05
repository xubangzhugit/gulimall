package com.izhiliu.erp.config.item;

import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author Twilight
 * @date 2021/3/8 11:14
 */
@Component
@Slf4j
public class ItemDtsConfig {

    private static final String ITEM_DTS_SWITCH_CONFIG = "dts_switch_config";
    /**
     * 配置value为open时，为开启聊聊功能
     */
    private static final String CHAT_SWITCH_CONFIG_VALUE = "close";

    @Resource
    private ConfigProperties configProperties;

    public String getDtsByConfig() {
        List<ConfigProperties.Dir> dir = configProperties.getDir();
        String value = dir.stream().filter(e -> e.getKey().equals(ITEM_DTS_SWITCH_CONFIG)).findFirst().get().getValue();
        if (CommonUtils.isNotBlank(value) && !Objects.equals(value, CHAT_SWITCH_CONFIG_VALUE)){
            log.info("开启dts从acm配置文件获取,dts={}", value);
            return value;
        }
        return null;
    }
}
