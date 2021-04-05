package com.izhiliu.erp.service.discount.mq;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.erp.service.discount.DiscountDetailService;
import com.izhiliu.erp.service.mq.consumer.BaseMQProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @Author: louis
 * @Date: 2020/8/6 14:35
 */
@Component
@Slf4j
public class DiscountDetailConsumer implements BaseMQProcessor {
    public static final String TAG = "DISCOUNT_DETAIL_TAG";

    @Resource
    private DiscountDetailService discountDetailService;
    @Resource
    private EnvironmentHelper environmentHelper;

    @Override
    public String getTag() {
        return TAG;
    }

    @PostConstruct
    void init(){
        mqProcessorMap.put(environmentHelper.handleTag(getTag()),this);
    }


    @Override
    public boolean process(Message message, ConsumeContext contest) {
        DiscountDetailMessageDTO dto = JSON.parseObject(new String(message.getBody()), DiscountDetailMessageDTO.class);
        log.info("同步折扣,参数对象:" + dto);
        return discountDetailService.syncDiscountDetail(dto);
    }

}
