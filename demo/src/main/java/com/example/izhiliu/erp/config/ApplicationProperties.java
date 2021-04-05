package com.izhiliu.erp.config;

import com.alibaba.ocean.rawsdk.ApiExecutor;
import com.izhiliu.open.shopee.open.sdk.api.category.ShopCategoryApi;
import com.izhiliu.open.shopee.open.sdk.api.category.ShopCategoryApiImpl;
import com.izhiliu.open.shopee.open.sdk.api.discount.ShopeeDiscountApi;
import com.izhiliu.open.shopee.open.sdk.api.discount.ShopeeDiscountApiImpl;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApiImpl;
import com.izhiliu.open.shopee.open.sdk.api.logistic.LogisticApi;
import com.izhiliu.open.shopee.open.sdk.api.publik.PublicApi;
import com.izhiliu.open.shopee.open.sdk.api.publik.PublicApiImpl;
import com.izhiliu.open.shopee.open.sdk.api.shop.ShopApi;
import com.izhiliu.open.shopee.open.sdk.api.shop.ShopApiImpl;
import lombok.Data;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Properties specific to Erp.
 * <p>
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@Data
@Component
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {
    private Logger logger = LoggerFactory.getLogger("ApplicationProperties");

    private Boolean syncChinaAdministrativeDivisionAddressRepository;
    private Boolean SyncShopOrderTimeLimit;
    private Boolean disableCredits;

    /**
     * 汇率隔几天刷新一次
     */
    private Integer currencyRateRefreshDays;

    private final Qiniu qiniu = new Qiniu();
    private final RocketMq rocketMq = new RocketMq();
    private final Alibaba alibaba = new Alibaba();
    private final SyncBasicData syncBasicData = new SyncBasicData();
    private final Shopee shopee = new Shopee();
    private String okHttpProxy;


    /**
     * Bean Load ----------------------------------
     */

    @Bean
    public ShopCategoryApi shopCategoryApi() { return  new ShopCategoryApiImpl(new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build());
    }

    @Bean
    public ShopeeDiscountApi shopeeDiscountApi() {
        return new ShopeeDiscountApiImpl(new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build());
    }
    /* --------------------------------------------- */


    @Bean
    public LogisticApi logisticApi() {
        return new LogisticApi(new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build());
    }

    @Bean
    public ItemApi itemApi() {
        return new ItemApiImpl(new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build());
    }

    @Bean
    public ShopApi shopApi() {
        return new ShopApiImpl(new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build());
    }

    @Bean
    public PublicApi publicApi() {
        return new PublicApiImpl(new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build());
    }

    @Data
    public static class ExamineParamShopee {
        private String currency;
        private Integer name;
        private Integer description;
        private Integer minDescription;
        private Integer attributeValue;
        private Integer maxShipDays;
    }

    @Data
    public static class SyncBasicData {
        private Integer syncBasicDataThreadSize;
        private Boolean syncCategoryEnable;
        private Boolean syncAttributeEnable;
        private Integer refresh;
    }

    @Data
    public static class Qiniu {
        private String access;
        private String secret;
        private String bucket;
    }

    @Data
    public static class RocketMq {
        private String access;
        private String secret;
        private String addr;
        private String sendMsgTimeOutMillis;
        private Boolean enable;
        private String consumeThreadNums;
    }

    @Data
    public static class Alibaba {
        private String key;
        private String secret;
        private Boolean enable;
    }

    @Data
    public static class Shopee {
        private Boolean mqPublish;
        private Boolean mqPull;
        private Boolean mqPush;
        private Boolean unlist;
        private List<ExamineParamShopee> examineParamShopees;

        public ExamineParamShopee get(String currency) {
            if (currency == null) {
                return examineParamShopees.get(0);
            }

            for (ExamineParamShopee examineParamShopee : examineParamShopees) {
                if (examineParamShopee.getCurrency().equals(currency)) {
                    return examineParamShopee;
                }
            }
            return examineParamShopees.get(0);
        }
    }

    public Boolean getSyncChinaAdministrativeDivisionAddressRepository() {
        return syncChinaAdministrativeDivisionAddressRepository;
    }

    public void setSyncChinaAdministrativeDivisionAddressRepository(Boolean syncChinaAdministrativeDivisionAddressRepository) {
        this.syncChinaAdministrativeDivisionAddressRepository = syncChinaAdministrativeDivisionAddressRepository;
    }

    public Boolean getSyncShopOrderTimeLimit() {
        return SyncShopOrderTimeLimit;
    }

    public void setSyncShopOrderTimeLimit(Boolean syncShopOrderTimeLimit) {
        SyncShopOrderTimeLimit = syncShopOrderTimeLimit;
    }

    @Bean
    public  ApiExecutor getApiExecutor() {
        return new ApiExecutor(alibaba.getKey(), alibaba.getSecret());
    }


}
