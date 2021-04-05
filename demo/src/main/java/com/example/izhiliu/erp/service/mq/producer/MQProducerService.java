package com.izhiliu.erp.service.mq.producer;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.ons.api.*;
import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;
import com.aliyun.openservices.shade.com.alibaba.fastjson.serializer.SerializerFeature;
import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.erp.config.ApplicationProperties;
import com.izhiliu.erp.config.mq.vo.ShopeeActionVO;
import com.izhiliu.erp.domain.enums.InternationEnum;
import com.izhiliu.erp.service.item.module.channel.ShopeeModelChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

@Service
public class MQProducerService {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final static String TOPIC="keyouyun";
    private final static String GID="GID_LUX";

    @Autowired
    protected ApplicationProperties applicationProperties;

    @Resource
    protected EnvironmentHelper environmentHelper;

    protected Producer producer;

    @Resource
    protected ShopeeModelChannel shopeeModelBridge;

    @Resource
    private MessageSource messageSource;

    @PostConstruct
    private void initMQ() {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, environmentHelper.handlePid(GID));
        properties.put(PropertyKeyConst.AccessKey, applicationProperties.getRocketMq().getAccess());
        properties.put(PropertyKeyConst.SecretKey, applicationProperties.getRocketMq().getSecret());
//        properties.put(PropertyKeyConst.OnsChannel, ONSChannel.CLOUD);
        properties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, applicationProperties.getRocketMq().getSendMsgTimeOutMillis());
        properties.put(PropertyKeyConst.NAMESRV_ADDR, applicationProperties.getRocketMq().getAddr());
        producer = ONSFactory.createProducer(properties);
        producer.start();
    }

    public void sendMQ(String tag, String key, Object message){
        Message msg = new Message(TOPIC,
                environmentHelper.handleTag(tag),
                JSON.toJSONBytes(message, SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullNumberAsZero, SerializerFeature.WriteNullBooleanAsFalse));
        msg.setKey(key);

        try {
           SendResult sendResult =  producer.send(msg);
           log.info("[Rocket-MQ] Send mq message success. Topic: {} tag:{} key:{} msgId: {}", sendResult.getTopic(), tag, key, sendResult.getMessageId());
        } catch (Exception e) {
            log.error("[Rocket-MQ]  Send mq message failed. Topic:{} tag:{}, key:{} ", msg.getTopic(), tag, key, e);
        }
    }
    public void sendAsyncMQ(String tag, String key, Object message){
        try {
            Message msg = new Message(TOPIC,
                environmentHelper.handleTag(tag),
                JSON.toJSONBytes(message, SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullNumberAsZero, SerializerFeature.WriteNullBooleanAsFalse));
            msg.setKey(key);
            producer.sendAsync(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("[Rocket-MQ] Send mq message success. Topic: {} tag:{} key:{} msgId: {}", sendResult.getTopic(), tag, key, sendResult.getMessageId());
                }
                @Override
                public void onException(OnExceptionContext context) {
                    //todo 消息发送失败，需要进行重试处理，可重新发送这条消息或持久化这条数据进行补偿处理
                    log.info("[Rocket-MQ]  Send mq message failed. Topic:{} tag:{}, key:{} ,msgId:{}", msg.getTopic(), tag, key, context.getMessageId(), context.getException());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendOneWayMQ(String tag, String key, Object message){
        Message msg = new Message(TOPIC,
                environmentHelper.handleTag(tag),
                JSON.toJSONBytes(message, SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullNumberAsZero, SerializerFeature.WriteNullBooleanAsFalse));
        msg.setKey(key);
        try {
            producer.sendOneway(msg);
        } catch (Exception e) {
            log.error("[Rocket-MQ]  Send mq message failed. Topic:{} tag:{}, key:{} ", msg.getTopic(), tag, key, e);
        }
    }
    public void sendMQ(String tag, String key, Object message, Consumer<Message> consumer) {
        Message msg = new Message(
                TOPIC,
                environmentHelper.handleTag(tag),
                JSON.toJSONBytes(message, SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullNumberAsZero, SerializerFeature.WriteNullBooleanAsFalse));
        msg.setKey(key);
        if(Objects.nonNull(consumer)){
            consumer.accept(msg);
        }
        producer.sendAsync(msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("[Rocket-MQ] Send mq message success. Topic: {} tag:{} key:{} msgId: {}", sendResult.getTopic(), tag, key, sendResult.getMessageId());
            }

            @Override
            public void onException(OnExceptionContext context) {
                //todo 消息发送失败，需要进行重试处理，可重新发送这条消息或持久化这条数据进行补偿处理
                log.info("[Rocket-MQ]  Send mq message failed. Topic:{} tag:{}, key:{} ,msgId:{}", msg.getTopic(), tag, key, context.getMessageId(), context.getException());
            }
        });
    }

}
