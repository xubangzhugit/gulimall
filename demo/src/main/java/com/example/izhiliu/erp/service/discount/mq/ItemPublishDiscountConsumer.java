package com.izhiliu.erp.service.discount.mq;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.erp.service.discount.DiscountItemService;
import com.izhiliu.erp.service.mq.consumer.BaseMQProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 *  商品发布或更新同步折扣商品信息
 * @Author: louis
 * @Date: 2020/8/17 11:25
 */
@Component
@Slf4j
public class ItemPublishDiscountConsumer implements BaseMQProcessor {


    @Resource
    private DiscountItemService discountItemService;
    @Resource
    private EnvironmentHelper environmentHelper;

    @Override
    public String getTag() {
        return DiscountItemPushMessageDTO.TAG;
    }

    @PostConstruct
    void init(){
        mqProcessorMap.put(environmentHelper.handleTag(getTag()),this);
    }


    @Override
    public boolean process(Message message, ConsumeContext contest) {
        DiscountItemPushMessageDTO dto = JSON.parseObject(new String(message.getBody()), DiscountItemPushMessageDTO.class);
        log.info("商品发布或更新同步折扣商品信息,参数对象:" + dto);
        return discountItemService.publishToSyncDiscount(dto);
    }
}
