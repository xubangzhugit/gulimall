package com.izhiliu.erp.config.mq.consumer.callback;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.config.BaseVariable;
import com.izhiliu.erp.config.mq.consumer.BaseMQProcessor;
import com.izhiliu.erp.config.mq.consumer.callback.base.CallbacEntity;
import com.izhiliu.log.LockCloseable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract   class CallbackBaseMQProcessor extends BaseMQProcessor {


    /**
     *    如果要自定义的话就调这个
     */
    @Override
    public  void setTopic(){
        setMyTopic(BaseVariable.CallbackProduct.TOPIC_TO_PRODUCT_CALLBACK);
    };



    @Override
    public void doProcess(Message message, ConsumeContext contest) {
        String body = new String(message.getBody());
        final String key = message.getKey();
        JSONObject dataWrapper;
        try {
            dataWrapper = JSON.parseObject(body);
        } catch (Exception var10) {
            log.error("[数据无法解析] : {}", body);
            return;
        }
        this.process(new CallbacEntity(dataWrapper,key));
    }
    public abstract void process(CallbacEntity callbacEntity);

    @Override
    public LockCloseable getCloseable() {
        return new LockCloseable();
    }

    @Override
    public boolean isCloseable() {
        return true;
    }
}
