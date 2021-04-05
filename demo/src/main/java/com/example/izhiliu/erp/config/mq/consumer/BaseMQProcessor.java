package com.izhiliu.erp.config.mq.consumer;

import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.erp.config.ApplicationProperties;
import com.izhiliu.erp.service.mq.consumer.MQProcessor;
import com.izhiliu.log.LockCloseable;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Objects;

@Slf4j
public abstract class BaseMQProcessor implements MQProcessor {

    public BaseMQProcessor() {
        setTopic();
    }

    protected   String   topic;

    @Resource
    ApplicationProperties applicationProperties;
    @Resource
    EnvironmentHelper environmentHelper;

    @PostConstruct
    void init(){
        mqProcessorMap.put(environmentHelper.handleTag(getTag()),this);
    }


    public String getTopic() {

        Objects.requireNonNull(topic);
        return topic;
    }

    public void setMyTopic(String topic) {
        this.topic = topic;
    }

    /**
     *    如果要自定义的话就调这个
     */
    public abstract void setTopic();


    protected String getAccess() {
        final String access = applicationProperties.getRocketMq().getAccess();
        if(log.isDebugEnabled()){
            log.debug("access {}",access);
        }
        return access;
    }

    protected String getSecret() {
        final String secret = applicationProperties.getRocketMq().getSecret();
        if(log.isDebugEnabled()){
            log.debug("secret {}",secret);
        }
        return secret;
    }

    protected String getAddr() {
        final String addr = applicationProperties.getRocketMq().getAddr();
        if(log.isDebugEnabled()){
            log.debug("addr {}",addr);
        }
        return addr;
    }

    protected Boolean enable() {
        final Boolean enable = applicationProperties.getRocketMq().getEnable();
        if(log.isDebugEnabled()){
            log.debug("enable {}",enable);
        }
        return enable;
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
