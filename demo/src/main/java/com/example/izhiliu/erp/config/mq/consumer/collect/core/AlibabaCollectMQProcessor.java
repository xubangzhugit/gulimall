package com.izhiliu.erp.config.mq.consumer.collect.core;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.config.processor.CollectMQProcessorVariable;
import com.izhiliu.core.common.CollectEntity;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.config.mq.consumer.collect.CollectBaseMQProcessor;
import com.izhiliu.erp.log.CustomLoggerUtils;
import com.izhiliu.erp.service.item.ProductMetaDataService;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.AlibabaMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.map.AlibabaMetaDateMap;
import com.izhiliu.feign.client.ezreal.ProviderService;
import com.izhiliu.feign.client.model.dto.Provider;
import com.izhiliu.mq.consumer.ConsumerObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.*;

/**
 * describe: 前端采集shopee数据处理
 * <p>
 *
 * @author cheng
 * @date 2019/1/24 14:56
 */
@Service
public class AlibabaCollectMQProcessor extends CollectBaseMQProcessor implements CollectMQProcessorVariable.AlibabaVariable {


    @Resource
    private ProductMetaDataService productMetaDataService;

    @Resource
    private AlibabaMetaDateMap alibabaMetaDataMap;

    protected ExecutorService executorService =  new ThreadPoolExecutor(
            5,
            5,
            1,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<Runnable>(1000), Executors.defaultThreadFactory(),new ThreadPoolExecutor.AbortPolicy());

    @Resource
    private ProviderService providerService;

    @Override
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
        final AlibabaProductProductInfoPlus productInfo = alibabaMetaDataMap.map(body,data.getCollectUrl());
        final AlibabaMetaDataConvert.AlibabaMetaData alibabaMetaData = new AlibabaMetaDataConvert.AlibabaMetaData(PlatformEnum.ALIBABA.getCode(), null, data.getLoginId(), data.getCollectUrl(), productInfo);
        productMetaDataService.alibabaToShopee(alibabaMetaData, null,body.getCollectController());
//        final Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        executorService.execute(()->{
            CustomLoggerUtils.fill((trace) -> {
//                MDC.put("X-B3-TraceId",copyOfContextMap.get("X-B3-TraceId"));
                Provider provider = new Provider();
                provider.setName(alibabaMetaData.getProductInfo().getSupplierUserId());
                provider.setWangwang(alibabaMetaData.getProductInfo().getSupplierLoginId());
                provider.setUrl(alibabaMetaData.getUrl());
                provider.setLoginId(alibabaMetaData.getLoginId());
                provider.setType("1688");
                providerService.saveProvider(provider);
            },throwable -> {
                log.error("save provider error",throwable);
            });
        });
    }

    @Override
    public String getCode() {
        return PlatformEnum.ALIBABA.name();
    }

}


