package com.izhiliu.erp.config.mq;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.erp.config.ApplicationProperties;
import com.izhiliu.erp.service.mq.consumer.MQProcessor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ApplicationReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {
    private static Logger logger = LoggerFactory.getLogger(ApplicationReadyEventListener.class);
    private final static String TOPIC = "keyouyun";
    @Resource
    ApplicationProperties applicationProperties;

    @Resource
    EnvironmentHelper environmentHelper;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("spring boot start success!!");
        String tags = String.join("||", MQProcessor.mqProcessorMap.keySet().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toCollection(LinkedHashSet::new)));
        initConsumer(tags);
    }

    private void initConsumer(String tags) {
        if (applicationProperties.getRocketMq().getEnable()) {
            Properties properties = new Properties();
            // 您在控制台创建的 Group ID
            properties.put(PropertyKeyConst.GROUP_ID, environmentHelper.handleCid("GID_LUX"));
            // AccessKey 阿里云身份验证，在阿里云服务器管理控制台创建
            properties.put(PropertyKeyConst.AccessKey, applicationProperties.getRocketMq().getAccess());
            // SecretKey 阿里云身份验证，在阿里云服务器管理控制台创建
            properties.put(PropertyKeyConst.SecretKey, applicationProperties.getRocketMq().getSecret());
            // 设置 TCP 接入域名，到控制台的实例基本信息中查看
            properties.put(PropertyKeyConst.NAMESRV_ADDR, applicationProperties.getRocketMq().getAddr());
            properties.put(PropertyKeyConst.ConsumeThreadNums, applicationProperties.getRocketMq().getConsumeThreadNums());
            Consumer consumer = ONSFactory.createConsumer(properties);
            //订阅多个 Tag
            consumer.subscribe(TOPIC, tags, (message, context) -> {
                 logger.info("[Rocket-MQ] - message:{}", message);
                String key = message.getTag().concat(":").concat(message.getMsgID());
                RLock rLock = redissonClient.getLock(key);
                if (rLock.tryLock()) {
                    try {
                        if (!MQProcessor.mqProcessorMap.get(message.getTag()).process(message, context)) {
                            return Action.ReconsumeLater;
                        }
                    } catch (Throwable e) {
                        logger.error("[Rocket-MQ] msgId:{} tag:{} key: {} consumer error ", message.getMsgID(), message.getTag(), message.getKey(), e);
                        return Action.ReconsumeLater;

                    } finally {
                        rLock.unlock();
                    }
                } else {
                    return Action.CommitMessage;
                }
                return Action.CommitMessage;
            });
            consumer.start();
            logger.info("start consumer success GID:{},tags:{}", environmentHelper.handleCid("GID_LUX"), tags);

        } else {
            logger.info("[Rocket-MQ] consumer unable");
        }
    }
}
