package com.izhiliu.erp.config.mq.consumer.shopee;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.config.BaseVariable;
import com.izhiliu.config.processor.BaseProcessorVariable;
import com.izhiliu.erp.config.mq.consumer.BaseMQProcessor;
import com.izhiliu.erp.log.CustomLoggerUtils;
import com.izhiliu.erp.log.LogConstant;
import com.izhiliu.erp.log.LoggerOp;
import com.izhiliu.erp.service.item.ShopeeProductMoveService;
import com.izhiliu.mq.consumer.ConsumerObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Seriel
 * @create 2019-09-24 15:04
 * 店铺搬家
 **/
@Slf4j
@Service
public class LuxShopMoveActionMQProcessor extends BaseMQProcessor implements BaseProcessorVariable {


    @Resource
    ShopeeProductMoveService shopeeProductMoveService;

    @Override
    public String getVariable() {
        return BaseVariable.LuxInternalActionVariable.LUX_SHOP_MOVE_ACTION_VERSION;
    }

    @Override
    public void setTopic() {
        setMyTopic(BaseVariable.LuxInternalActionVariable.TOPIC_LUX_INTERNAL_ACTION);
    }

    @Override
    public ConsumerObject getConsumerObject() {
        return new ConsumerObject().setTopic(getTopic()).setTag(getTag()).setCid(getCid());
    }
    protected String getCid() {
        return getCidVariable();
    }


    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getTag() {
        return BaseVariable.LuxInternalActionVariable.TAG_LUX_SHOP_MOVE_ACTION_VERSION;
    }

    @Override
    public void doProcess(Message message, ConsumeContext contest) {
        final LoggerOp loggerOp = new LoggerOp().start().setKind(LogConstant.SHOPEE_PRODUCT).setType(LogConstant.SHOP_MOVE).setCode(LogConstant.MOVE);
        final String key = message.getKey();
         String productId = new String(message.getBody());
//        if(productId.startsWith("{")){
//           return;
//        }
        loggerOp.setMessage(" key:{} , productId  {} ");
        log.info(loggerOp.toString(),key,productId);
        CustomLoggerUtils.fill(trace -> {
            shopeeProductMoveService.syncShopeeProductMoveTask(key, JSONObject.parseObject(productId,String.class));
        },throwable -> {
            loggerOp.error().setMessage("key:{} , productId  {} ");
        });
        log.info(loggerOp.ok().toString(),key,productId);
    }



}
