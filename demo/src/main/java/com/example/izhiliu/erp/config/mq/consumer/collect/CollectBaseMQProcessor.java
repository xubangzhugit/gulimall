package com.izhiliu.erp.config.mq.consumer.collect;

import com.izhiliu.erp.config.mq.consumer.BaseCollectTagMQProcessor;
import com.izhiliu.log.LockCloseable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


public abstract   class CollectBaseMQProcessor extends BaseCollectTagMQProcessor {


   protected Logger  log  =  LoggerFactory.getLogger(getClass());

    public static final String TOPIC_TO_ERP_MQ_MESSAGE = "TOPIC_TO_ERP_MQ_MESSAGE";

    /**
     *    如果要自定义的话就调这个
     */
    public  void setTopic(){
        setMyTopic(TOPIC_TO_ERP_MQ_MESSAGE);
    };

    public CollectBaseMQProcessor() {
        setTopic();
    }

    protected   String   topic;



    public String getTopic() {

        Objects.requireNonNull(topic);
        return topic;
    }

    public void setMyTopic(String topic) {
        this.topic = topic;
    }



    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public LockCloseable getCloseable() {
        return new LockCloseable();
    }

    @Override
    public boolean isCloseable() {
        return true;
    }
}
