package com.izhiliu.erp.service.mq.consumer;

import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.log.LockCloseable;
import com.izhiliu.mq.RocketMQProcessor;
import com.izhiliu.mq.consumer.ConsumerObject;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * describe: 消息处理程序
 * <p>
 * @date 2019/1/24 14:33
 */
public interface MQProcessor {


    Map<String, MQProcessor> mqProcessorMap = new ConcurrentHashMap<>();

    Logger getLogger();

    /**
     * 模板方法获取 TAG
     */
    default String getTag() {
        throw new NullPointerException("写下tag行不行  没有就很尴尬的");
    }

    /**
     * 处理消息
     */
    void doProcess(Message message, ConsumeContext contest);

    default int threadCount() {
        //  之前是  5个
        return Runtime.getRuntime().availableProcessors();
    }

    default boolean process(Message message, ConsumeContext contest) {
        return isCloseable() ? tryRunProcess(message, contest) : runProcess(message, contest);
    }

    default boolean tryRunProcess(Message message, ConsumeContext contest) {
        try (LockCloseable closeable = getCloseable()) {
            doProcess(message, contest);
        } catch (Exception e) {
            getLogger().error("消息消费失败: msgId:{}", message.getMsgID(), e);
        }
        return true;
    }

    default boolean runProcess(Message message, ConsumeContext contest) {
        try {
            doProcess(message, contest);
        } catch (Exception e) {
            getLogger().error("消息消费失败: msgId:{}", message.getMsgID(), e);
        }
        return true;
    }


    ConsumerObject getConsumerObject();

    /**
     * 获取自定义的 信息 比如traceId
     *
     * @return
     */
    LockCloseable getCloseable();


    /**
     * 是否启用 自定义信息
     *
     * @return
     */
    boolean isCloseable();
}
