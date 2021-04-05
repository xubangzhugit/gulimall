package com.izhiliu.erp.service.module.metadata.map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.product.param.AlibabaProductProductImageInfo;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.common.JsoupUtils;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductSKUInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductSKUAttrInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaSkuMateDataMapDto;
import com.izhiliu.erp.service.module.metadata.origin.E7OriginSKuInfo;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author: louis
 * @Date: 2020/7/20 14:36
 */
@Component
@Slf4j
public class E7MetaDataMap extends AbstractAlibabaProductMetaDateMap {

    @Override
    public AlibabaProductProductInfoPlus map(MetaDataObject pageHtml) {
        MetaDataObject.CollectController collectController = pageHtml.getCollectController();
        boolean collectDiscount = collectController.isCollectDiscount();
        String html = pageHtml.getHtml();
        AlibabaProductProductInfoPlus productInfo = new AlibabaProductProductInfoPlus();
        final Document document = Jsoup.parse(html);
        productInfo.setSubject(JsoupUtils.getMetaBy(document, "og:title"));
        /**
         *  处理图片
         */
        setToImage(pageHtml, productInfo);

        /**
         * 设置描述
         */
        setToDescription(productInfo, document);

        /**
         * 处理sku信息
         */
        setToSkuInfo(collectDiscount, productInfo, document);

        /**
         * 设置产品id
         */
        setToProductId(productInfo, document);


        /**
         * 设置类目
         */
        setToCategory(productInfo, document);

        /**
         * 设置供应商
         */
        setToSupplier(productInfo, html);

        /**
         * 设置价格
         */
        setToPrice(collectDiscount, productInfo, document);

        return productInfo;
    }

    private void setToPrice(boolean collectDiscount, AlibabaProductProductInfoPlus productInfo, Document document) {
        String attribute = collectDiscount ? "og:product:price" : "og:product:orgprice";
        String price = JsoupUtils.getMetaBy(document, attribute);
        productInfo.setReferencePrice(price);
    }

    private void setToSupplier(AlibabaProductProductInfoPlus productInfo, String html) {
        String wangwang = JsoupUtils.getSubStr(html, "v=3&groupid=0&s=1&charset=utf-8&site=cntaobao&groupid=0&s=1&uid=", "\"class=\"icon-zwd\"");
        String supplierName = JsoupUtils.getSubStr(html, "<div class=\"shop-head-name\"><span>", "</span>");
        productInfo.setSupplierLoginId(wangwang);
        productInfo.setSupplierUserId(supplierName);
    }

    private void setToCategory(AlibabaProductProductInfoPlus productInfo, Document document) {
        String categoryName = JsoupUtils.getMetaBy(document, "og:product:category");
        Long categoryId = Long.valueOf(categoryName.hashCode());
        productInfo.setCategoryName(categoryName);
        productInfo.setCategoryID(categoryId);
    }

    private void setToDescription(AlibabaProductProductInfoPlus productInfo, Document document) {
        //详情描述
        String description = document.select(".details-right-content").select(".details-right-content-item")
                .stream()
                .map(Element::text)
                .collect(Collectors.joining("\n"));

        productInfo.setDescription(description);
        //详情图片
        Elements select = document.select(".details-right-allTB-image-container").select("img[src]");
        List<String> descImages = select.stream()
                .map(e -> e.attr("src"))
                .distinct()
                .collect(Collectors.toList());
        productInfo.setDescImages(descImages);
    }

    private void setToProductId(AlibabaProductProductInfoPlus productInfo, Document document) {
        String productId = JsoupUtils.getScriptBy(document, "O.ITEM_ID = '", "';").replace("';", "");
        productInfo.setProductID(Long.valueOf(productId));
    }

    /**
     * 设置sku信息
     * @param collectDiscount
     * @param productInfo
     * @param document
     */
    private void setToSkuInfo(boolean collectDiscount, AlibabaProductProductInfoPlus productInfo, Document document) {
        //宝贝详情属性，ex: 颜色分类 : 酒红色 黑色 宝蓝色 藕色
        Map<String, String> itemAttribute = new HashMap<>();
        Arrays.stream(productInfo.getDescription().split("\n"))
                .forEach(f -> {
                    String[] split = f.split(":");
                    itemAttribute.put(split[0], split[1]);
                });
        String group = JsoupUtils.getScriptBy(document, "O.ITEM_SKU=", "}]}");
        E7OriginSKuInfo originSKuInfo = JSON.parseObject(group, E7OriginSKuInfo.class);
        final AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto = new AlibabaSkuMateDataMapDto();
        List<E7OriginSKuInfo.Sku> sku = originSKuInfo.getSku();
        if (CommonUtils.isNotBlank(sku)) {
            //非单品
            AlibabaProductProductSKUInfoPlus[] skuInfo = sku.stream().map(e -> {
                AlibabaProductProductSKUInfoPlus productProductSKUInfoPlus = new AlibabaProductProductSKUInfoPlus();
                BigDecimal price = collectDiscount ? e.getPrice2() : e.getPrice();
                productProductSKUInfoPlus.setPrice(price.doubleValue());
                productProductSKUInfoPlus.setStock(e.getNum());
                String[] split = e.getProperties().split(";");
                AlibabaProductSKUAttrInfoPlus[] alibabaProductSKUAttrInfoPluses = Arrays.stream(split).map(f -> {
                    AlibabaProductSKUAttrInfoPlus attrInfoPlus = new AlibabaProductSKUAttrInfoPlus();
                    attrInfoPlus.setAttributeValue(f);
                    attrInfoPlus.setIndex(alibabaSkuMateDataMapDto.getNextCursor());
                    Arrays.stream(e.getPropertiesName().split(";"))
                            .filter(x -> x.indexOf(f) >= 0)
                            .findFirst()
                            .ifPresent(x -> {
                                String attributeName = x.replace(f, "").replace(":", "");
                                attrInfoPlus.setAttributeName(attributeName);
                                attrInfoPlus.setAttributeValue(attributeName);
                                itemAttribute.entrySet().stream()
                                        .filter(entry -> Arrays.stream(entry.getValue().split(" ")).anyMatch(attributeName::equals))
                                        .findFirst()
                                        .ifPresent(y -> attrInfoPlus.setAttributeDisplayName(y.getKey()));
                            });
                    attrInfoPlus.setSkuImageUrl(e.getImgUrl());
                    return attrInfoPlus;
                }).toArray(AlibabaProductSKUAttrInfoPlus[]::new);
                productProductSKUInfoPlus.setAttributes(alibabaProductSKUAttrInfoPluses);
                return productProductSKUInfoPlus;
            }).toArray(AlibabaProductProductSKUInfoPlus[]::new);
            String[] images = sku.stream().map(E7OriginSKuInfo.Sku::getImgUrl)
                    .filter(CommonUtils::isNotBlank)
                    .distinct()
                    .toArray(String[]::new);
            AlibabaProductProductImageInfo alibabaProductProductImageInfo = new AlibabaProductProductImageInfo();
            if (CommonUtils.isNotBlank(images)) {
                alibabaProductProductImageInfo.setImages(images);
            } else {
                alibabaProductProductImageInfo.setImages(productInfo.getImage().getImages());
            }

            //设置sku信息
            productInfo.setSkuInfos(skuInfo);
            //设置图片信息
            productInfo.setImage(alibabaProductProductImageInfo);

        }
    }

    /**
     * 获取图片
     * @param html
     * @param collectController
     * @param productInfo
     */
    private void setToImage(MetaDataObject metaDataObject, AlibabaProductProductInfoPlus productInfo) {
        String content = metaDataObject.getContent();
        if (CommonUtils.isBlank(content)) {
            return;
        }
        String[] images = JSON.parseArray(content, String.class)
                .stream().toArray(String[]::new);
        AlibabaProductProductImageInfo imageInfo = new AlibabaProductProductImageInfo();
        imageInfo.setImages(images);
        productInfo.setImage(imageInfo);
    }

    @Override
    protected List<AlibabaProductProductSKUInfoPlus> getAlibabaProductSKUInfos(Double defaultPrice, JSONObject valItemInfo, AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto) {
        return null;
    }

    @Override
    public void fillDiscountPriceAndStock(AlibabaProductProductInfoPlus skuProps, JSONObject originalDiscount, MetaDataObject.CollectController collectController) {

    }

    @Override
    public void fillDiscountPriceAndStock(List<AlibabaProductProductSKUInfoPlus> skuProps, JSONObject originalDiscount, MetaDataObject.CollectController collectController) {

    }

    @Override
    public Long getCategoryId(String content) {
        return null;
    }
}
