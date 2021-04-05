package com.izhiliu.erp.config.mq.consumer.collect.core;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.config.processor.CollectMQProcessorVariable;
import com.izhiliu.core.common.CollectEntity;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.config.mq.consumer.collect.CollectBaseMQProcessor;
import com.izhiliu.erp.service.item.ProductMetaDataService;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.TaobaoMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.map.TaobaoMetaDateMap;
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
public class TaobaoCollectMQProcessor extends CollectBaseMQProcessor implements CollectMQProcessorVariable.TaobaoVariable {


    @Resource
    private ProductMetaDataService productMetaDataService;

    @Resource
    private TaobaoMetaDateMap taobaoMetaDateMap;

    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Resource
    private ProviderService providerService;


    @Override
    public String getTag() {
        return this.getTagVariable();
    }

    public ConsumerObject getConsumerObject() {
        return new ConsumerObject().setTopic(getTopic()).setTag(getTag()).setCid(getCid());
    }

    protected String getCid() {
        return this.getCidVariable();
    }
    @Override
    public void process(CollectEntity<JSONObject> data) {
        final MetaDataObject body = data.getData().getObject("data",MetaDataObject.class);
//        body.setBeforeUseConfiguration();
        final AlibabaProductProductInfoPlus productInfo = taobaoMetaDateMap.map(body);
        final TaobaoMetaDataConvert.TaoBaoMetaData alibabaMetaData = new TaobaoMetaDataConvert.TaoBaoMetaData(PlatformEnum.TAOBAO.getCode(), null, data.getLoginId(), data.getCollectUrl(), productInfo);
        productMetaDataService.alibabaToShopee(alibabaMetaData, null,body.getCollectController());

        executorService.execute(()->{
            try {

            Provider provider = new Provider();
            provider.setName(alibabaMetaData.getProductInfo().getSupplierUserId());
            provider.setWangwang(alibabaMetaData.getProductInfo().getSupplierLoginId());
            provider.setUrl(alibabaMetaData.getUrl());
            provider.setLoginId(alibabaMetaData.getLoginId());
            provider.setType("taobao");
            providerService.saveProvider(provider);
            }catch (Exception e){
                log.error("save provider error",e);
            }
        });
    }
    @Override
    public String getCode() {
        return PlatformEnum.TAOBAO.name();
    }

}


