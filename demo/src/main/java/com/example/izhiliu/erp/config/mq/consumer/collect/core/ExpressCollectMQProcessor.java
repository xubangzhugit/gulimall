package com.izhiliu.erp.config.mq.consumer.collect.core;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.config.processor.CollectMQProcessorVariable;
import com.izhiliu.core.common.CollectEntity;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.config.mq.consumer.collect.CollectBaseMQProcessor;
import com.izhiliu.erp.service.item.ProductMetaDataService;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.ExpressMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.map.ExpressMetaDataMap;
import com.izhiliu.mq.consumer.ConsumerObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/3 9:34
 */
@Service
public class ExpressCollectMQProcessor extends CollectBaseMQProcessor implements CollectMQProcessorVariable.ExpressVariable {


    @Resource
    private ProductMetaDataService productMetaDataService;

    @Resource
    private ExpressMetaDataMap expressMetaDataMap;

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
        JSONObject body = data.getData().getJSONObject("data");
        ExpressMetaDataConvert.ExpressMetaData expressMetaData = expressMetaDataMap.map(
            body.getString("html"),
            body.getString("content")
        );

        expressMetaData.setLoginId(data.getLoginId());
        expressMetaData.setUrl(data.getCollectUrl());

        productMetaDataService.expressToShopee(expressMetaData, null, MetaDataObject.COLLECT_CONTROLLER );
    }

    @Override
    public String getCode() {
        return PlatformEnum.ALIEXPRESS.name();
    }
}
