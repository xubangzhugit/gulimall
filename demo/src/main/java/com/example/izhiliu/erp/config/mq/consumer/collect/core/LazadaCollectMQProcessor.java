package com.izhiliu.erp.config.mq.consumer.collect.core;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.config.processor.CollectMQProcessorVariable;
import com.izhiliu.core.common.CollectEntity;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.config.mq.consumer.collect.CollectBaseMQProcessor;
import com.izhiliu.erp.service.item.ProductMetaDataService;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.LazadaMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.map.LazadaMetaDataMap;
import com.izhiliu.mq.consumer.ConsumerObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * describe: 前端采集1688数据处理
 * <p>
 *
 * @author cheng
 * @date 2019/1/24 14:56
 */
@Service
public class LazadaCollectMQProcessor extends CollectBaseMQProcessor implements CollectMQProcessorVariable.LazadaVariable {


    @Resource
    private ProductMetaDataService productMetaDataService;

    @Resource
    private LazadaMetaDataMap lazadaMetaDataMap;

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
        LazadaMetaDataConvert.LazadaMetaData lazadaMetaData = lazadaMetaDataMap.map(
            data.getData().getJSONObject("data").getJSONObject("data").getJSONObject("data").getJSONObject("root").getString("fields"),
            data.getData().getJSONObject("data").getString("content"))
            .setLoginId(data.getLoginId());
        final MetaDataObject.CollectController collectController = data.getData().getJSONObject("data").getObject("collectController", MetaDataObject.CollectController.class);
        productMetaDataService.lazadaToShopee(lazadaMetaData, null, Objects.nonNull(collectController)?collectController: MetaDataObject.COLLECT_CONTROLLER);
    }

    @Override
    public String getCode() {
        return PlatformEnum.LAZADA.name();
    }
}
