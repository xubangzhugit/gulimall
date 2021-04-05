package com.izhiliu.erp.service.module.metadata.convert;

import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.izhiliu.core.Exception.NetworkConnectionException;
import com.izhiliu.core.Exception.ShopeeApiException;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.core.common.constant.PlatformNodeEnum;
import com.izhiliu.core.util.RegexUtil;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import com.izhiliu.erp.service.module.metadata.basic.BaseMetaData;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataConvert;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.izhiliu.core.common.constant.ShopeeConstant.*;

/**
 * describe: Shopee 源数据转换器
 * <p>
 *
 * @author cheng
 * @date 2019/1/9 16:42
 */
@Component
public class ShopeeMetaDataConvert implements MetaDataConvert<ShopeeMetaDataConvert.ShopeeMetaData> {

    private static final Logger log = LoggerFactory.getLogger(ShopeeMetaDataConvert.class);

    @Resource
    private CollectExec collect;

    @Override
    public ProductMetaDataDTO collect(ShopeeMetaData data) {
        return getProductMetaData(fill(data));
    }

    public ProductMetaDataDTO collect(String url, String loginId) {
        return getProductMetaData(fill(collect.request(url, loginId)));
    }

    private ShopeeMetaData fill(ShopeeMetaData data) {
        final UrlInfo info = ExtractUtil.extract(data.getUrl());
        final Integer platformNodeId = ShopeeUtil.nodeId(info.getSuffix()).intValue();
        if (null != info.getItemId() && RegexUtil.isNumeric(info.getItemId())) {
            data.setItemId(Long.parseLong(info.getItemId()));
        } else {
            data.setItemId(-1l);
        }
        data.setShopId(info.getShopId());
        data.setSuffix(info.getSuffix());
        data.setPlatformNodeId(platformNodeId);
        return data;
    }

    private ProductMetaDataDTO getProductMetaData(ShopeeMetaData data) {
        final String json = data.getJson();
        final String url = data.getUrl();
        final String suffix = data.getSuffix();
        final Integer platformNodeId = data.getPlatformNodeId();
        final String loginId = data.getLoginId();

        final JSONObject item = JSON.parseObject(json).getJSONObject("item");
        final String name = item.getString("name");
        final String description = item.getString("description");
        final String currency = item.getString("currency");
        final MetaDataObject.CollectController collectController = data.getCollectController();
        final Long price = ShopeeUtil.collectInputPrice(collectController.isCollectDiscount()?item.getLong("price"):item.getLong("price_before_discount"),currency);
        final Integer sold = item.getInteger("sold");

        final List<String> images = item.getJSONArray("images").toJavaList(String.class)
                .stream().map(e -> fillImageStrategy(data, suffix, e)).collect(Collectors.toList());

        final ProductMetaDataDTO metaData = new ProductMetaDataDTO();
        metaData.setUrl(url);
        metaData.setName(name);
        metaData.setDescription(description);
        metaData.setPrice(price);
        metaData.setCurrency(currency);
        metaData.setSold(sold);
        metaData.setCollectTime(Instant.now());
        metaData.setImages(images);

        metaData.setPlatformId(PlatformEnum.SHOPEE.getCode().longValue());
        metaData.setPlatform(PlatformEnum.SHOPEE.getName());
        metaData.setPlatformNodeId(platformNodeId.longValue());
        metaData.setSuffix(suffix);
        metaData.setJson(json);
        metaData.setProductId(data.getItemId());

        metaData.setLoginId(loginId);
        metaData.setKey(loginId + "@" + data.getPlatformId() + "@" + data.getShopId() + "@" + data.getItemId());
        final JSONArray categories = item.getJSONArray("categories");
        categories.stream()
                .filter( o -> !CollectionUtils.isEmpty(((JSONObject) o))).filter(o1 -> ((JSONObject) o1).getBoolean("no_sub")).map(p -> ((JSONObject) p).getLong("catid")).findFirst().ifPresent(catId -> {
            metaData.setCategoryId(catId);
        });
        /*
         * 采集人
         */
        return metaData;
    }

    /**
     * 填充图片地址
     */
    private String fillImageStrategy(ShopeeMetaData data, String suffix, String e) {
        if (ReUtil.contains(RE_TW_SPARE, data.getUrl()) || ReUtil.contains(RE_TW_SPARE_OTHER, data.getUrl())) {
            return IMAGE_PATH_TW_SPARE.replace(FILL_IMG_KEY, e);
        } else {
            return IMAGE_PATH.replace(FILL_SUFFIX, suffix).replace(FILL_IMG_KEY, e);
        }
    }

    /**
     * 提取数据
     */
    @Data
    public static class ExtractUtil {

        private static final Pattern RE_SUFFIX = Pattern.compile("shopee.([a-zA-Z.]*)/");
        private static final Pattern RE_SUFFIX_ELSE = Pattern.compile("https://([a-zA-Z.]*).xiapibuy.com/");
        private static final Pattern RE_ITEM_AND_SHOP = Pattern.compile("\\.(\\d+)\\.(\\d+)");
        private static final Pattern RE_ITEM_AND_SHOP_OTHER = Pattern.compile("\\/(\\d+)\\/(\\d+)");

        public static UrlInfo extract(String url) {
            final UrlInfo info = new UrlInfo();
            if (null != ReUtil.get(RE_ITEM_AND_SHOP, url, 1)) {
                info.setShopId(ReUtil.get(RE_ITEM_AND_SHOP, url, 1));
                info.setItemId(ReUtil.get(RE_ITEM_AND_SHOP, url, 2));
            } else {
                info.setShopId(ReUtil.get(RE_ITEM_AND_SHOP_OTHER, url, 1));
                info.setItemId(ReUtil.get(RE_ITEM_AND_SHOP_OTHER, url, 2));
            }
            fillSuffixStrategy(url, info);

            log.info("[url] : {}", url);
            log.info("[suffix] : {}", info.getSuffix());
            log.info("[shopId] : {}", info.getShopId());
            log.info("[itemId] : {}", info.getItemId());
            return info;
        }


        /**
         * 填充后缀策略
         */
        private static void fillSuffixStrategy(String url, UrlInfo info) {
            if (!ReUtil.contains(RE_URL, url)) {

                if (ReUtil.contains(RE_TW_SPARE, url)) {
                    info.setSuffix(PlatformNodeEnum.SHOPEE_TW.suffix);
                    info.setUrl(url);
                } else {
                    info.setSuffix(ReUtil.get(RE_SUFFIX, url, 1));
                }
            } else {
                if (ReUtil.contains(Pattern.compile(PlatformNodeEnum.SHOPEE_MY_OTHER.url), url)) {
                    info.setSuffix(PlatformNodeEnum.SHOPEE_MY_OTHER.suffix);
                } else if (ReUtil.contains(Pattern.compile(PlatformNodeEnum.SHOPEE_ID_OTHER.url), url)) {
                    info.setSuffix(PlatformNodeEnum.SHOPEE_ID_OTHER.suffix);
                } else if (ReUtil.contains(Pattern.compile(PlatformNodeEnum.SHOPEE_TH_OTHER.url), url)) {
                    info.setSuffix(PlatformNodeEnum.SHOPEE_TH_OTHER.suffix);
                }else if (ReUtil.contains(Pattern.compile(PlatformNodeEnum.SHOPEE_BR_OTHER.url), url)) {
                    info.setSuffix(PlatformNodeEnum.SHOPEE_BR_OTHER.suffix);
                }  else {
                    info.setSuffix(ReUtil.get(RE_SUFFIX_ELSE, url, 1));
                }
            }
        }
    }

    @Data
    public static class UrlInfo {
        private String suffix;
        private String itemId;
        private String shopId;
        private String url;
    }

    /**
     * Shopee 采集器
     */
    @Component
    public static class CollectExec {
        private static final Logger log = LoggerFactory.getLogger(CollectExec.class);
        private static final int TIME_OUT = 5000;

        /**
         * 需要指定三个参数
         * <p>
         * {SITE}       : 站点 my\id
         * {ITEM_ID}    : 产品
         * {SHOP_ID}    : 店铺
         */
        public ShopeeMetaData request(String url, String loginId) {
            final UrlInfo param = fillParam(url);
            final String api = getItemInfoApi(param);

            log.info("[request-url] : {}", api);

            int retryCount = 3;
            String response;
            while (true) {
                try {
                    response = HttpRequest.get(api).timeout(TIME_OUT).execute().body();
                    break;
                } catch (HttpException e) {
                    if (retryCount == 0) {
                        log.error("[请求异常] {}, {}", api, e);
                        throw new NetworkConnectionException("network.connection.exception", api);
                    }
                    retryCount--;
                }
            }

            log.info("[request-api] : {}", api);
            log.info("[response-body] : {}", response);

            if (!response.contains("item")) {
                log.error("[collect-shopee-item] : API 返回了无法解析的数据");
                throw new ShopeeApiException("collect error");
            }

            return new ShopeeMetaData(PlatformEnum.SHOPEE.getCode(), response, url, loginId,null);
        }

        private String getItemInfoApi(UrlInfo itemInfoParam) {
            return fillApiStrategy(itemInfoParam);
        }

        /**
         * 填充API
         */
        private String fillApiStrategy(UrlInfo itemInfoParam) {
            if (itemInfoParam.getUrl() != null) {
                return API_GET_SHOPEE_ITEM_DETAILED_TW_SPARE
                        .replace(FILL_ITEM_ID, itemInfoParam.getItemId())
                        .replace(FILL_SHOP_ID, itemInfoParam.getShopId());
            } else {
                return API_GET_SHOPEE_ITEM_DETAILED
                        .replace(FILL_SUFFIX, itemInfoParam.getSuffix()).
                                replace(FILL_ITEM_ID, itemInfoParam.getItemId()).
                                replace(FILL_SHOP_ID, itemInfoParam.getShopId());
            }
        }

        private UrlInfo fillParam(String pageUrl) {
            return ExtractUtil.extract(pageUrl);
        }
    }

    /**
     * describe: Shopee 源数据
     * <p>
     *
     * @author cheng
     * @date 2019/1/22 11:39
     */
    @Data
    public static class ShopeeMetaData extends BaseMetaData {

        private Integer platformNodeId;
        private String shopId;
        private String suffix;
        private MetaDataObject.CollectController collectController;

        public ShopeeMetaData(Integer platformId, String json, String url, String loginId,MetaDataObject.CollectController collectController) {
            super(platformId, json, loginId, url);
            this.collectController = collectController;
        }
    }
}
