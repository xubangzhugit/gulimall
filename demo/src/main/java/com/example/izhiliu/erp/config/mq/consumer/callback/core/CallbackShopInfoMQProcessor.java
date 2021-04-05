package com.izhiliu.erp.config.mq.consumer.callback.core;


import com.izhiliu.erp.config.mq.consumer.callback.base.CallbacEntity;
import com.izhiliu.erp.config.mq.consumer.callback.CallbackBaseMQProcessor;

import com.izhiliu.erp.service.item.module.channel.ShopeeModelChannel;
import com.izhiliu.mq.consumer.ConsumerObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;


/**
 * describe: 前端采集shopee数据处理
 * <p>
 *
 * @author cheng
 * @date 2019/1/24 14:56
 */
@Service
@Slf4j
public class CallbackShopInfoMQProcessor extends CallbackBaseMQProcessor {



    @Override
    public ConsumerObject getConsumerObject() {
        return new ConsumerObject().setTopic(getTopic()).setTag(getTag()).setCid(getCid());
    }

    protected String getCid() {
        return "CID_UAA_SHOP_LUX_MSG";
    }

    @Override
    public  void setTopic(){
        setMyTopic("TOPIC_UAA_MQ_MESSAGE");
    };
    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getTag() {
        return "UAA_SHOP_INFO";
    }

//    PID_UAA_SHOP_MSG
    @Resource
    protected ShopeeModelChannel shopeeModelBridge;


    @Override
    public void process(CallbacEntity callbacEntity) {
        //临时停止店铺授权后的商品同步，等拆表后再开启
//        final ShopeeShopDTO shopeeShopDTO = callbacEntity.getData().toJavaObject(ShopeeShopDTO.class);
//        shopeeModelBridge.pull(shopeeShopDTO.getShopId(),shopeeShopDTO.getLogin());
    }








}


