package com.izhiliu.erp.service.image.mq;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.service.image.cache.ImageBankCacheService;
import com.izhiliu.erp.service.mq.consumer.BaseMQProcessor;
import com.izhiliu.model.SubscribeSuccessDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Objects;

/**
 *  图片空间应用订购成功通知
 * @Author: louis
 * @Date: 2020/8/31 15:08
 */
@Component
@Slf4j
public class ImageProductConsumer implements BaseMQProcessor {

    private final static String GOLD = "gold";
    private final static String DIAMONDS = "diamonds";
    private final static String PICTURESPACE = "pictureSpace";

    @Resource
    private EnvironmentHelper environmentHelper;
    @Resource
    private ImageBankCacheService imageBankCacheService;

    @Override
    public String getTag() {
        return SubscribeSuccessDTO.TAG;
    }

    @PostConstruct
    void init(){
        mqProcessorMap.put(environmentHelper.handleTag(getTag()),this);
    }

    @Override
    public boolean process(Message message, ConsumeContext contest) {
        SubscribeSuccessDTO dto = JSON.parseObject(new String(message.getBody()), SubscribeSuccessDTO.class);
        if (CommonUtils.isBlank(dto.getProductKey()) || CommonUtils.isBlank(dto.getLogin())) {
            return true;
        }
        if (!isMatch(dto.getProductKey())) {
            return true;
        }
        log.info("图片空间应用订购成功通知,参数对象:" + dto);
        return imageBankCacheService.cleanCache(dto);
    }

    private static boolean isMatch(String productKey) {
        return Objects.equals(GOLD, productKey) || Objects.equals(DIAMONDS, productKey)
                || Objects.equals(PICTURESPACE, productKey);
    }

}
