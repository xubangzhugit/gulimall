package com.izhiliu.erp.config.mq.consumer;


import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.erp.service.mq.consumer.MQProcessor;
import com.izhiliu.mq.RocketMQProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/15 19:11
 */
public abstract class AlibabaTagMQProcessor implements MQProcessor {

    static final String TOPIC_TO_CONSUMER_ALIBABA_MESSAGE = "keyouyun";

    @Resource
    EnvironmentHelper environmentHelper;

    protected String getTopic() {
        return TOPIC_TO_CONSUMER_ALIBABA_MESSAGE;
    }


    protected String getCid() {
        return "CID_" + TOPIC_TO_CONSUMER_ALIBABA_MESSAGE;
    }

    @PostConstruct
    void init(){
        mqProcessorMap.put(environmentHelper.handleTag(getTag()),this);
    }

}
