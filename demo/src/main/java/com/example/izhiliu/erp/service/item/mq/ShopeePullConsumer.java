package com.izhiliu.erp.service.item.mq;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.erp.service.image.cache.ImageBankCacheService;
import com.izhiliu.erp.service.item.dto.ShopeePullMessageDTO;
import com.izhiliu.erp.service.mq.consumer.BaseMQProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 *  同步商品任务
 * @Author: louis
 * @Date: 2020/9/25 11:21
 */
@Component
@Slf4j
public class ShopeePullConsumer implements BaseMQProcessor {

    @Resource
    private EnvironmentHelper environmentHelper;
    @Resource
    private PullAction pullAction;

    @Override
    public String getTag() {
        return ShopeePullMessageDTO.TAG;
    }

    @PostConstruct
    void init(){
        mqProcessorMap.put(environmentHelper.handleTag(getTag()),this);
    }

    @Override
    public boolean process(Message message, ConsumeContext contest) {
        ShopeePullMessageDTO dto = JSON.parseObject(new String(message.getBody()), ShopeePullMessageDTO.class);
        log.info("同步shopee商品,参数对象:" + dto);
        return pullAction.pull(dto);
    }


}
