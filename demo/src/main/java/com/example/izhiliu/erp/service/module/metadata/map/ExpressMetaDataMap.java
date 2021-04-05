package com.izhiliu.erp.service.module.metadata.map;

import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataMap;
import com.izhiliu.erp.service.module.metadata.convert.ExpressMetaDataConvert;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/4 9:53
 */
@Component
public class ExpressMetaDataMap implements MetaDataMap<String, ExpressMetaDataConvert.ExpressMetaData> {

    private static final Logger log = LoggerFactory.getLogger(ExpressMetaDataMap.class);


    @Override
    public ExpressMetaDataConvert.ExpressMetaData map(String... t) {
        //html页面
        String html = t[0];

        //详情描述
        String descDetail = t[1];

        try {
            JSONObject jsonAliexpress = JSON.parseObject(ReUtil.get(Pattern.compile("\\{\"actionModule\":(.*)\"site\":\"[\\w]*\"\\}\\}"), html, 0));

            if (null != jsonAliexpress) {
                final ExpressMetaDataConvert.ExpressMetaData oldAliexpress = getOldAliexpress(descDetail, jsonAliexpress);
                oldAliexpress.setCategoryId(getCategoryId(html));
                oldAliexpress.setProductId(getProductId2(html));
                return oldAliexpress;
            }
        } catch (Exception e) {
            log.error(" collect old aliexpress error", e);
        }

        Document document = Jsoup.parse(html);

        ExpressMetaDataConvert.ExpressMetaData data = new ExpressMetaDataConvert.ExpressMetaData(PlatformEnum.ALIEXPRESS.getCode(), null, null, null);

        data.setName(getName(document));
        data.setPrice(getPrice(document));
        data.setSold(getSold(document));
        data.setAttributes(getAttributes(document));
        data.setSkuAttributes(getSkuAttributes(document));
        data.setSkuInfos(getSkuInfos(html));
        data.setImages(getMianImages(document));
        data.setMianImages(getMianImages(document));
        data.setDescImages(getDescImages(descDetail));
        data.setCategoryId(getCategoryId(html));
        data.setProductId(getProductId2(html));

        List<ExpressMetaDataConvert.ExpressMetaData.SkuAttribute> skuAttributes = data.getSkuAttributes();
        fillSkuIndex(skuAttributes, data.getSkuInfos());
        data.setSkuInfos(data.getSkuInfos().stream()
                .distinct()
                .collect(Collectors.toList()));


        return data;
    }

    private List<String> getDescImages(String descDetail) {
        List<String> images = collectImages(descDetail);
        return images.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getMianImages(Document document) {
        List<String> images = collectImages(document.select("#j-image-thumb-list").toString());
//        images.addAll(collectImages(document.select("#j-product-description").toString()));
        //check if https or http
        return images.stream().map(imgUrl -> {
            if (!imgUrl.startsWith("http")) {
                return "https:" + imgUrl;
            } else {
                return imgUrl;
            }
        }).collect(Collectors.toList());
    }

    @Override
    public Long getCategoryId(String content) {
        Pattern CATEGORY_ID = Pattern.compile("\\\"categoryId\\\":([\\d]*)");
        final String s = ReUtil.get(CATEGORY_ID, content, 1);
        return StringUtils.isNotBlank(s) ? Long.parseLong(s) : 0L;
    }

    private Long getProductId2(String content) {
        Pattern PRODUCT_ID = Pattern.compile("\\\"productId\\\":([\\d]*)");
        return Long.parseLong(ReUtil.get(PRODUCT_ID, content, 1));
    }


    private ExpressMetaDataConvert.ExpressMetaData getOldAliexpress(String descDetail, JSONObject jsonAliexpress) {
        ExpressMetaDataConvert.ExpressMetaData data = new ExpressMetaDataConvert.ExpressMetaData(PlatformEnum.ALIEXPRESS.getCode(), null, null, null);

        data.setName(jsonAliexpress.getJSONObject("titleModule").getString("subject"));
        data.setPrice(ReUtil.get(Pattern.compile("\\d+[.]*[\\d]*"), jsonAliexpress.getJSONObject("priceModule").getString("formatedPrice"), 0));
        data.setSold(jsonAliexpress.getJSONObject("titleModule").getInteger("tradeCount"));
        data.setAttributes(getAttributes(descDetail,jsonAliexpress));
        //  假设没有获取到属性的话就尝试手动去获取
        if(Objects.isNull(data.getDescription())){
            data.setDescription(getDescription(descDetail));
        }
        data.setSkuAttributes(getSkuAttributes(jsonAliexpress));
        data.setSkuInfos(getSkuInfos(jsonAliexpress));
        data.setImages(getMianImagesForJson(jsonAliexpress));
        data.setMianImages(getMianImagesForJson(jsonAliexpress));
        data.setDescImages(getDescImages(descDetail));

        List<ExpressMetaDataConvert.ExpressMetaData.SkuAttribute> skuAttributes = data.getSkuAttributes();
        fillSkuIndex(skuAttributes, data.getSkuInfos());
        data.setSkuInfos(data.getSkuInfos().stream()
                .distinct()
                .collect(Collectors.toList()));
        return data;
    }

    private String getDescription(String descDetail) {
        final Elements span = Jsoup.parse(descDetail).getElementsByTag("span");
        if (!CollectionUtils.isEmpty(span)) {
           String description  =   span.stream().map(element -> element.text()).collect(Collectors.joining("\t\n"));
           return description;
        }
        return  "";
    }

    private List<String> getMianImagesForJson(JSONObject jsonAliexpress) {
        List<String> images = jsonAliexpress.getJSONObject("imageModule").getJSONArray("imagePathList").toJavaList(String.class);
        return images.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private void fillSkuIndex(List<ExpressMetaDataConvert.ExpressMetaData.SkuAttribute> skuAttributes, List<ExpressMetaDataConvert.ExpressMetaData.SkuInfo> skuInfos) {
        for (int i = 0; i < skuInfos.size(); i++) {
            ExpressMetaDataConvert.ExpressMetaData.SkuInfo skuInfo = skuInfos.get(i);
            String skuPropIds = skuInfo.getSkuPropIds();
            if (StringUtils.isBlank(skuPropIds)) {
                return;
            }

            String[] propIds = skuPropIds.split(",");

            List<Integer> indexs = new ArrayList<>(2);
            for (int x = 0; x < propIds.length; x++) {
                long propId = Long.parseLong(propIds[x]);
                List<ExpressMetaDataConvert.ExpressMetaData.SkuAttribute.Option> options = skuAttributes.get(x).getOptions();
                for (int y = 0; y < options.size(); y++) {
                    if (options.get(y).getId().equals(propId)) {
                        indexs.add(y);
                        break;
                    }
                }
            }
            skuInfo.setIndexs(indexs);
        }
    }

    private List<String> getImages(String detailed, Document document) {
        List<String> images = getImages(document);
        images.addAll(collectImages(detailed));
        return images.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getImages(String detailed, JSONObject jsonObject) {
        List<String> images = jsonObject.getJSONObject("imageModule").getJSONArray("imagePathList").toJavaList(String.class);
        images.addAll(collectImages(detailed));
        return images.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private static int getSold(Document document) {
        try {
            return Integer.parseInt(ReUtil.getGroup0("\\d+", document.select("#j-order-num").text()));
        } catch (Exception e) {
            return 0;
        }
    }

    private static List<ExpressMetaDataConvert.ExpressMetaData.Attribute> getAttributes(String descDetail, JSONObject jsonObject) {
        final JSONArray jsonArray = jsonObject.getJSONObject("specsModule").getJSONArray("props");
        if (!CollectionUtils.isEmpty(jsonArray)) {
            return jsonArray.stream().map(prop -> new ExpressMetaDataConvert.ExpressMetaData.Attribute(((JSONObject) prop).getString("attrName"), ((JSONObject) prop).getString("attrValue"))).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static List<ExpressMetaDataConvert.ExpressMetaData.Attribute> getAttributes(Document document) {
        return document.select(".product-property-list").get(0).children().stream()
                .map(e -> {
                    return new ExpressMetaDataConvert.ExpressMetaData.Attribute(
                            e.select(".propery-title").get(0).text(),
                            e.select(".propery-des").get(0).text());
                })
                .collect(Collectors.toList());
    }

    private String getPrice(Document document) {
        return document.select("#j-sku-price").get(0).text().replace(" ", "");
    }

    private String getName(Document document) {
        return document.select(".product-name").get(0).text();
    }

    private List<String> getImages(Document document) {
        List<String> images = collectImages(document.select("#j-image-thumb-list").toString());
        images.addAll(collectImages(document.select("#j-product-description").toString()));
        //check if https or http
        return images.stream().map(imgUrl -> {
            if (!imgUrl.startsWith("http")) {
                return "https:" + imgUrl;
            } else {
                return imgUrl;
            }
        }).collect(Collectors.toList());
    }

    private List<String> collectImages(String html) {
        return ReUtil.findAllGroup1("src=\"(.+?)\"", html).stream()
                .map(e -> ReUtil.replaceAll(e, "_\\d+x\\d+\\.jpg", ""))
                .map(imgUrl -> {
                    if (!imgUrl.startsWith("http")) {
                        return "https:" + imgUrl;
                    } else {
                        return imgUrl;
                    }
                })
                .collect(Collectors.toList());
    }

    private List<ExpressMetaDataConvert.ExpressMetaData.SkuInfo> getSkuInfos(String body) {
        return JSON.parseArray(ReUtil.getGroup1("skuProducts=(.+?]);", body), ExpressMetaDataConvert.ExpressMetaData.SkuInfo.class).stream()
                .map(this::filterVariationTier)
                .collect(Collectors.toList());
    }

    private List<ExpressMetaDataConvert.ExpressMetaData.SkuInfo> getSkuInfos(JSONObject jsonObject) {
        return JSON.parseArray(jsonObject.getJSONObject("skuModule").getJSONArray("skuPriceList").toJSONString(), ExpressMetaDataConvert.ExpressMetaData.SkuInfo.class).stream()
                .map(this::filterVariationTier)
                .collect(Collectors.toList());
    }

    /**
     * TODO 过滤2个以上的SKU
     *
     * @param e
     */
    private ExpressMetaDataConvert.ExpressMetaData.SkuInfo filterVariationTier(ExpressMetaDataConvert.ExpressMetaData.SkuInfo e) {
        String skuAttr = e.getSkuAttr();
        if (StringUtils.isBlank(skuAttr)) {
            return e;
        }

        String[] ids = skuAttr.split(";");
        if (ids.length > 2) {
            e.setSkuAttr(ids[0] + ";" + ids[1]);

            String[] propId = e.getSkuPropIds().split(",");
            e.setSkuPropIds(propId[0] + "," + propId[1]);

            log.warn("[丢弃SKU]: {}", ids[2]);
        }
        return e;
    }

    private List<ExpressMetaDataConvert.ExpressMetaData.SkuAttribute> getSkuAttributes(Document document) {
        Element productSkuInfo = document.getElementById("j-product-info-sku");

        List<ExpressMetaDataConvert.ExpressMetaData.SkuAttribute> skuAttributes = new ArrayList<>(5);
        for (final Element propertyItem : productSkuInfo.children()) {
            String skuAttributeName = propertyItem.select(".p-item-title").get(0).text();

            Element options = propertyItem.select(".p-item-main > ul").get(0);
            Long skuPropertyId = Long.parseLong(options.attr("data-sku-prop-id"));

            List<ExpressMetaDataConvert.ExpressMetaData.SkuAttribute.Option> ops = new ArrayList<>();
            for (final Element option : options.select("a")) {
                Long optionId = Long.parseLong(option.attr("data-sku-id"));
                String optionName = null;
                String skuPropertyImagePath = null;
                try {
                    optionName = option.select("img").get(0).attr("title");
                    skuPropertyImagePath = option.select("img").get(0).attr("bigpic");
                } catch (Exception e) {
                    optionName = option.select("span").get(0).text();
                }

                ops.add(new ExpressMetaDataConvert.ExpressMetaData.SkuAttribute.Option(optionId, optionName, skuPropertyImagePath));
            }
            skuAttributes.add(new ExpressMetaDataConvert.ExpressMetaData.SkuAttribute(skuPropertyId, skuAttributeName, ops));
        }
        return skuAttributes;
    }

    private List<ExpressMetaDataConvert.ExpressMetaData.SkuAttribute> getSkuAttributes(JSONObject jsonObject) {
        if (null == jsonObject.getJSONObject("skuModule").getJSONArray("productSKUPropertyList")) {
            return new ArrayList<>();
        }
        return jsonObject.getJSONObject("skuModule").getJSONArray("productSKUPropertyList").stream().map(JSONObject.class::cast).map(productSKUProperty -> {
            ExpressMetaDataConvert.ExpressMetaData.SkuAttribute skuAttribute = new ExpressMetaDataConvert.ExpressMetaData.SkuAttribute();
            skuAttribute.setId(productSKUProperty.getLong("skuPropertyId"));
            skuAttribute.setName(productSKUProperty.getString("skuPropertyName"));
            skuAttribute.setOptions(
                    productSKUProperty.getJSONArray("skuPropertyValues").stream().map(JSONObject.class::cast).map(skuPropertyValue -> {
                        ExpressMetaDataConvert.ExpressMetaData.SkuAttribute.Option ops = new ExpressMetaDataConvert.ExpressMetaData.SkuAttribute.Option();
                        ops.setId(skuPropertyValue.getLong("propertyValueId"));
                        ops.setOption(skuPropertyValue.getString("propertyValueDisplayName"));
                        ops.setSkuPropertyImagePath(skuPropertyValue.getString("skuPropertyImagePath"));
                        return ops;
                    }).collect(Collectors.toList())
            );
            return skuAttribute;
        }).collect(Collectors.toList());

    }
}
