package com.izhiliu.erp.config.mq.consumer.collect.core;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.config.processor.CollectMQProcessorVariable;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.core.common.CollectEntity;
import com.izhiliu.erp.config.mq.consumer.collect.CollectBaseMQProcessor;
import com.izhiliu.erp.service.item.ProductMetaDataService;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.ShopeeMetaDataConvert;
import com.izhiliu.mq.consumer.ConsumerObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * describe: 前端采集shopee数据处理
 * <p>
 *
 * @author cheng
 * @date 2019/1/24 14:56
 */
@Service
public class ShopeeCollectMQProcessor extends CollectBaseMQProcessor implements CollectMQProcessorVariable.ShopeeVariable {


    @Resource
    private ProductMetaDataService productMetaDataService;

    public ConsumerObject getConsumerObject() {
        return new ConsumerObject().setTopic(getTopic()).setTag(getTag()).setCid(getCid());
    }

    protected String getCid() {
        return this.getCidVariable();
    }

    @Override
    public String getTag() {
        return this.getTagVariable();
    }
    @Override
    public void process(CollectEntity<JSONObject> data) {
        final MetaDataObject.CollectController collectController = data.getData().getJSONObject("data").getObject("collectController", MetaDataObject.CollectController.class);
        final MetaDataObject.CollectController collectController1 = Objects.nonNull(collectController) ? collectController : MetaDataObject.COLLECT_CONTROLLER;
        final ShopeeMetaDataConvert.ShopeeMetaData dataCollected = new ShopeeMetaDataConvert.ShopeeMetaData(
            PlatformEnum.SHOPEE.getCode(),
            data.getData().getString("data"),
            data.getCollectUrl(),
            data.getLoginId(),
                collectController1
        );

        productMetaDataService.toShopee(dataCollected,null, collectController1);

    }

    @Override
    public String getCode() {
        return PlatformEnum.SHOPEE.name();
    }
}


