package com.izhiliu.erp.config.mq.consumer.collect.core;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.config.processor.CollectMQProcessorVariable;
import com.izhiliu.core.common.CollectEntity;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.config.mq.consumer.collect.CollectBaseMQProcessor;
import com.izhiliu.erp.service.item.ProductMetaDataService;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.PingduoduoMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.map.PingduoduoMetaDateMap;
import com.izhiliu.feign.client.ezreal.ProviderService;
import com.izhiliu.feign.client.model.dto.Provider;
import com.izhiliu.mq.consumer.ConsumerObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * describe: 前端采集shopee数据处理
 * <p>
 *
 * @author cheng
 * @date 2019/1/24 14:56
 */
@Slf4j
@Service
public class PingduoduoCollectMQProcessor extends CollectBaseMQProcessor implements CollectMQProcessorVariable.PingduoduoVariable {


    @Resource
    private ProductMetaDataService productMetaDataService;

    @Resource
    private PingduoduoMetaDateMap pingduoduoMetaDateMap;

    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Resource
    private ProviderService providerService;

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
        final MetaDataObject body = data.getData().getObject("data",MetaDataObject.class);
//        body.setBeforeUseConfiguration();
        final AlibabaProductProductInfoPlus productInfo = pingduoduoMetaDateMap.map(body);
        final PingduoduoMetaDataConvert.PingduoduoMeteData pingduoduoMeteData  = new PingduoduoMetaDataConvert.PingduoduoMeteData(PlatformEnum.PDD.getCode(), null, data.getLoginId(), data.getCollectUrl(), productInfo);
        productMetaDataService.pingduoduoToShopee(pingduoduoMeteData, null,MetaDataObject.COLLECT_CONTROLLER );

        executorService.execute(()->{
            try {
            Provider provider = new Provider();
            provider.setName(pingduoduoMeteData.getProductInfo().getSupplierUserId());
            provider.setWangwang(pingduoduoMeteData.getProductInfo().getSupplierLoginId());
            provider.setUrl(pingduoduoMeteData.getUrl());
            provider.setLoginId(pingduoduoMeteData.getLoginId());
            provider.setType("pdd");
            providerService.saveProvider(provider);
            }catch (Exception e){
                log.error("save provider error",e);
            }
        });
    }

    @Override
    public String getCode() {
        return PlatformEnum.PDD.name();
    }

}


