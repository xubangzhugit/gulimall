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
 *  同步折扣商品信息
 * @Author: louis
 * @Date: 2020/8/12 11:25
 */
@Component
@Slf4j
public class DiscountSyncItemConsumer implements BaseMQProcessor {


    @Resource
    private DiscountItemService discountItemService;
    @Resource
    private EnvironmentHelper environmentHelper;

    @Override
    public String getTag() {
        return DiscountItemMessageDTO.TAG;
    }

    @PostConstruct
    void init(){
        mqProcessorMap.put(environmentHelper.handleTag(getTag()),this);
    }


    @Override
    public boolean process(Message message, ConsumeContext contest) {
        DiscountItemMessageDTO dto = JSON.parseObject(new String(message.getBody()), DiscountItemMessageDTO.class);
        log.info("折扣同步商品信息,参数对象:" , dto);
        return discountItemService.syncProductSku(dto);
    }
}
