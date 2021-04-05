package com.izhiliu.erp.service.mq.consumer;

import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.log.LockCloseable;
import com.izhiliu.mq.consumer.ConsumerObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Author: louis
 * @Date: 2020/8/6 14:41
 */
public interface BaseMQProcessor extends MQProcessor {

    public static final Logger log = LoggerFactory.getLogger(BaseMQProcessor.class);

    default ConsumerObject getConsumerObject() {
        return new ConsumerObject();
    }

    default LockCloseable getCloseable() {
        return new LockCloseable();
    }

    default boolean isCloseable() {
        return true;
    }

    default Logger getLogger() {
        return null;
    }

    default void doProcess(Message message, ConsumeContext contest) {
        this.process(message, contest);
    }



}
