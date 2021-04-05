package com.izhiliu.erp.config.mq.consumer.alibaba;

import com.alibaba.account.param.AlibabaAccountAgentBasicParam;
import com.alibaba.account.param.AlibabaAccountAgentBasicResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.ocean.rawsdk.ApiExecutor;
import com.alibaba.ocean.rawsdk.common.SDKResult;
import com.alibaba.product.param.AlibabaCrossProductInfoParam;
import com.alibaba.product.param.AlibabaCrossProductInfoResult;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.config.ApplicationProperties;
import com.izhiliu.erp.config.mq.consumer.AlibabaTagMQProcessor;
import com.izhiliu.erp.config.mq.vo.AlibabaOneClickDistributionVO;
import com.izhiliu.erp.service.item.ProductMetaDataService;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.AlibabaMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductSKUInfoPlus;
import com.izhiliu.feign.client.ezreal.ProviderService;
import com.izhiliu.feign.client.model.dto.Provider;
import com.izhiliu.log.LockCloseable;
import com.izhiliu.mq.consumer.ConsumerObject;
import com.izhiliu.uaa.feignclient.UaaService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * describe: 1688一件铺货消息处理
 * <p>
 *
 * @author cheng
 * @date 2019/1/26 15:57
 */
@Service
@Slf4j
public class AlibabaOneClickDistributionMQProcessor extends AlibabaTagMQProcessor {

    public static final String TAB_CROSS_BORDER_ONE_CLICK_DISTRIBUTION = "CROSS_BORDER_ONE_CLICK_DISTRIBUTION";

    private static final String OFFER_URL = "https://detail.1688.com/offer/{OFFER_ID}.html";
    private static final String OFFER_ID = "{OFFER_ID}";

    @Resource
    private ApplicationProperties properties;

    @Resource
    private ProductMetaDataService productMetaDataService;

    @Resource
    private UaaService uaaService;

    private ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Resource
    private ProviderService providerService;

    @Resource
    private ApiExecutor apiExecutor;

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getTag() {
        return TAB_CROSS_BORDER_ONE_CLICK_DISTRIBUTION;
    }

    @Override
    public ConsumerObject getConsumerObject() {
        return new ConsumerObject().setTopic(getTopic()).setTag(getTag()).setCid(getCid());
    }

    @Override
    public LockCloseable getCloseable() {
        return new LockCloseable();
    }

    @Override
    public boolean isCloseable() {
        return true;
    }


    @Override
    public void doProcess(Message message, ConsumeContext contest) {
        final AlibabaOneClickDistributionVO param = JSON.parseObject(new String(message.getBody()), AlibabaOneClickDistributionVO.class);

        log.info("[消费消息队列消息] - [一键铺货] : {}", param.getBody());

        final JSONObject body = JSON.parseObject(param.getBody());
        final String key = param.getKey();

        final JSONObject data = body.getJSONObject("data");
        final Long userId = data.getLong("userInfo");
        final Long offerId = data.getLong("offerId");

        /*
         * 1624961198
         */
        log.info("[key]: {}", key);
        log.info("[userId]: {}", userId);
        log.info("[offerId]: {}", offerId);

        int count = 3;
        ResponseEntity<Map<String, String>> alibabaAccountToken = null;
        while (count > 0) {
            try {
                alibabaAccountToken = uaaService.getAlibabaAccountToken(key, userId);
                break;
            } catch (Exception e) {
                log.error("[调用UAA失败] ：{}, {}", e.getMessage(), count - 1);
                count--;
            }
        }

        if (alibabaAccountToken == null || alibabaAccountToken.getBody() == null) {
            log.error("[根据appKey和1688UserId获取Token失败] - key:{}, userId:{}", key, userId);
            return;
        }

        final String accessToken = alibabaAccountToken.getBody().get("access_token");
        final String loginId = alibabaAccountToken.getBody().get("login");

        log.info("[accessToken]: {}", accessToken);
        log.info("[loginId]: {}", loginId);

        if (accessToken == null || loginId == null) {
            throw new RuntimeException("该用户授权了,但是在系统找不到账户信息... [" + loginId + "]");
        }

        final AlibabaCrossProductInfoResult result = getProductInfo(offerId, accessToken);
        if (result.getSuccess() != null && result.getSuccess()) {
            getLogger().info("AlibabaOneClickDistributionMQProcessor {}",JSONObject.toJSONString(result));
            final AlibabaProductProductInfoPlus productInfo = JSONObject.parseObject(JSONObject.toJSONString(result.getProductInfo()),AlibabaProductProductInfoPlus.class);;
            if(Objects.nonNull(productInfo)){
                productInfo.setSkuInfos(Stream.of(productInfo.getSkuInfos()).map(alibabaProductProductSKUInfoPlus -> {
                    alibabaProductProductSKUInfoPlus.setStock(alibabaProductProductSKUInfoPlus.getAmountOnSale());
                    return  alibabaProductProductSKUInfoPlus;
                }).toArray(value -> new AlibabaProductProductSKUInfoPlus[value]));
            }
            final AlibabaMetaDataConvert.AlibabaMetaData alibabaMetaData = new AlibabaMetaDataConvert.AlibabaMetaData(PlatformEnum.ALIBABA.getCode(), JSON.toJSONString(result.getProductInfo()), loginId, OFFER_URL.replace(OFFER_ID, offerId.toString()), productInfo);
            productMetaDataService.alibabaToShopee(alibabaMetaData, null, MetaDataObject.COLLECT_CONTROLLER);
            log.info("[消费1688一件铺货消息成功]");
            //新增供应商
            executorService.execute(() -> {
                try {
                    AlibabaAccountAgentBasicParam alibabaAccountAgentBasicParam = new AlibabaAccountAgentBasicParam();
                    alibabaAccountAgentBasicParam.setLoginId(productInfo.getSupplierLoginId());
                    AlibabaAccountAgentBasicResult alibabaAccountAgentBasicResult = apiExecutor.execute(alibabaAccountAgentBasicParam, accessToken).getResult();
                    if(null != alibabaAccountAgentBasicResult){
                        Provider provider = new Provider();
                        provider.setName(alibabaAccountAgentBasicResult.getResult().getCompanyName());
                        provider.setWangwang(productInfo.getSupplierLoginId());
                        provider.setType("1688");
                        provider.setLoginId(loginId);
                        providerService.saveProvider(provider);
                    }
                } catch (Exception e) {
                    log.error("save provider error ", e);
                }
            });

        } else {
            log.error("[获取跨境商品错误] : {}", result.getMessage());
        }
    }

    private AlibabaCrossProductInfoResult getProductInfo(Long productId, String accessToken) {

        log.info("[productId]: {}", productId);

        final ApiExecutor executor = new ApiExecutor(properties.getAlibaba().getKey(), properties.getAlibaba().getSecret());
        final AlibabaCrossProductInfoParam param = new AlibabaCrossProductInfoParam();
        param.setProductId(productId);
        final SDKResult<AlibabaCrossProductInfoResult> execute = executor.execute(param, accessToken);
        if (execute.getResult() == null) {
            log.error("[获取1688商品详情失败]- {}, {}", execute.getErrorCode(), execute.getErrorMessage());
        }
        return execute.getResult();
    }

    protected String getAccess() {
        return properties.getRocketMq().getAccess();
    }

    protected String getSecret() {
        return properties.getRocketMq().getSecret();
    }

    protected String getAddr() {
        return properties.getRocketMq().getAddr();
    }

    protected Boolean enable() {
        return properties.getRocketMq().getEnable();
    }
}
