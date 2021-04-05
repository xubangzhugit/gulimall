package com.izhiliu.erp.config.mq.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.core.common.CollectEntity;
import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.erp.log.CustomLoggerUtils;
import com.izhiliu.erp.log.LoggerOp;
import com.izhiliu.erp.service.mq.consumer.MQProcessor;
import com.izhiliu.mq.RocketMQProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Instant;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/28 19:17
 */
public abstract class BaseCollectTagMQProcessor   implements MQProcessor {

    @Resource
    EnvironmentHelper environmentHelper;

    public abstract void process(CollectEntity<JSONObject> data);

    public abstract String getCode();

    @PostConstruct
    void init(){
        mqProcessorMap.put(environmentHelper.handleTag(getTag()),this);
    }


    @Override
    final
    public void doProcess(Message message, ConsumeContext contest) {
        final String body = new String(message.getBody());

        JSONObject dataWrapper;
        try {
            dataWrapper = JSON.parseObject(body);
        } catch (Exception e) {
            getLogger().error("[数据无法解析] : {}", body);
            return;
        }
        final String key = message.getKey();
        final Integer action = dataWrapper.getInteger("action");
        final String loginId = dataWrapper.getString("loginId");
        final String collectUrl = dataWrapper.getString("collectUrl");
        final Instant instant = Instant.ofEpochSecond(dataWrapper.getLong("timestamp"));


        getLogger().debug("-------------------------------------------------");
        getLogger().debug("[action] : {}", action);
        getLogger().debug("[loginId] : {}", loginId);
        getLogger().debug("[collectUrl] : {}", collectUrl);
        getLogger().debug("[timestamp] : {}", instant);
        getLogger().debug("[key] : {}", key);
        getLogger().debug("-------------------------------------------------");

        final LoggerOp loggerOp = new LoggerOp().setStatusPlus(LoggerOp.Status.START).setMessage("[action] : {} [collectUrl] : {} [timestamp] : {}  [key] : {} ").setKind("collect").setType("create").setCode(getCode()).setLoginId(loginId);
        getLogger().info(loggerOp.toString(),action,collectUrl,instant,key);

        CustomLoggerUtils.fill((trace) -> {
            process(new CollectEntity<>(action, loginId, collectUrl, instant, dataWrapper));
        }, throwable -> {
            getLogger().error(loggerOp.setStatusPlus(LoggerOp.Status.ERROR).toString(),throwable);
        });
        getLogger().info(loggerOp.setStatusPlus(LoggerOp.Status.OK).toString());
    }


}
