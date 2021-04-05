package com.izhiliu.erp.config.mq.consumer.collect.core;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.config.processor.CollectMQProcessorVariable;
import com.izhiliu.core.common.CollectEntity;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.config.mq.consumer.collect.CollectBaseMQProcessor;
import com.izhiliu.erp.service.item.ProductMetaDataService;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.E7MetaDataConvert;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.map.E7MetaDataMap;
import com.izhiliu.mq.consumer.ConsumerObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 *  17网采集
 * @Author: louis
 * @Date: 2020/7/20 14:18
 */
@Service
public class E7MQProcessor extends CollectBaseMQProcessor implements CollectMQProcessorVariable.E7Variable {

    @Resource
    private E7MetaDataMap e7MetaDataMap;

    @Resource
    private ProductMetaDataService productMetaDataService;


    @Override
    public String getTag() {
        return this.getTagVariable();
    }

    @Override
    public String getCode() {
        return PlatformEnum.E7.name();
    }

    @Override
    public ConsumerObject getConsumerObject() {
        return null;
    }

    @Override
    public void process(CollectEntity<JSONObject> data) {
        final MetaDataObject body = data.getData().getObject("data", MetaDataObject.class);
        final AlibabaProductProductInfoPlus productInfo = e7MetaDataMap.map(body);
        E7MetaDataConvert.E7MetaData e7MetaData = new E7MetaDataConvert.E7MetaData(PlatformEnum.E7.getCode(), null, data.getLoginId(), data.getCollectUrl(), productInfo);
        productMetaDataService.e7ToShopee(e7MetaData, null, MetaDataObject.COLLECT_CONTROLLER);

    }
}
