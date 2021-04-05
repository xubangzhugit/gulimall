package com.izhiliu.erp.config.mq.consumer.shopee;

import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.config.BaseVariable;
import com.izhiliu.config.processor.BaseProcessorVariable;
import com.izhiliu.erp.config.mq.consumer.BaseMQProcessor;
import com.izhiliu.erp.service.item.ShopeeProductMoveService;
import com.izhiliu.erp.web.rest.item.param.ShopeeProductMoveParam;
import com.izhiliu.mq.consumer.ConsumerObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * @author Seriel
 * @create 2019-09-24 15:04
 * 店铺搬家
 **/
@Slf4j
@Service
public class LuxRemoveShopMoveActionMQProcessor extends BaseMQProcessor implements BaseProcessorVariable {


    @Resource
    ShopeeProductMoveService shopeeProductMoveService;

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
        return BaseVariable.LuxInternalActionVariable.TAG_LUX_REMOVE_SHOP_MOVE_ACTION_VERSION;
    }

    @Override
    public void doProcess(Message message, ConsumeContext contest) {
        final String key = message.getKey();
        final String taskId = new String(message.getBody());
        shopeeProductMoveService.deleteProductMoveTask(new ShopeeProductMoveParam().setTaskId(Arrays.asList(Long.parseLong(taskId))));
    }


    @Override
    public String getVariable() {
        return BaseVariable.LuxInternalActionVariable.LUX_REMOVE_SHOP_MOVE_ACTION_VERSION;
    }
}
