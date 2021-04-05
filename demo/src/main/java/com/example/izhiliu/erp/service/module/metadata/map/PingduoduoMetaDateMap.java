package com.izhiliu.erp.service.module.metadata.map;

import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.product.param.AlibabaProductProductAttribute;
import com.alibaba.product.param.AlibabaProductProductImageInfo;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.TaobaoMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductSKUInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductSKUAttrInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaSkuMateDataMapDto;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * describe: 1688前端采集 ==> 源数据(转成一键铺货的数据结构)
 * <p>
 *
 * @author cheng
 * @date 2019/2/27 15:27
 */
@Component
public class PingduoduoMetaDateMap extends AbstractAlibabaProductMetaDateMap {

    private static final Logger log = LoggerFactory.getLogger(PingduoduoMetaDateMap.class);

    @Override
    public AlibabaProductProductInfoPlus map(MetaDataObject pageHtml) {
        String html = squeeze(pageHtml.getHtml());
        String content = ReUtil.get("window.rawData=\\{(.+)}};</script>", html, 0);
        html = null;
        content = content.substring(content.indexOf("=")+1);
        content = content.substring(0,content.indexOf(";</script>"));
        final JSONObject jsonObject = JSONObject.parseObject(content).getJSONObject("store").getJSONObject("initDataObj");
        content = null;
        final AlibabaProductProductInfoPlus productInfo = new AlibabaProductProductInfoPlus();
        /*
         * 描述
         */
        final JSONObject goods = jsonObject.getJSONObject("goods");

        final List<String> collect = goods.getJSONArray("detailGallery").stream().map(o -> ((JSONObject) o).getString("url")).collect(Collectors.toList());
        productInfo.setDescription(JSONArray.toJSONString(collect));
        productInfo.setSubject(goods.getString("goodsName"));
        productInfo.setImage(getImage(goods.getJSONArray("topGallery")));
        productInfo.setAttributes(getAttributes(goods));
        productInfo.setProductID(goods.getLong("goodsID"));
        productInfo.setReferencePrice(goods.getString("minNormalPrice"));
//        productInfo.setShippingInfo(getShippingInfo(html));
        productInfo.setSkuInfos(getSkuInfos(goods,goods.getDouble("minNormalPrice"), pageHtml.getCollectController()));
        //<meta property="og:product:nick" content="name=广州市增城联谊手袋厂; url= //paste666.cn.1688.com">
        productInfo.setSupplierLoginId(jsonObject.getJSONObject("mall").getString("mallID"));
//        //这里先将公司名放进去
        productInfo.setSupplierUserId(jsonObject.getJSONObject("mall").getString("mallName"));
        final String catID = goods.getString("catID");
        productInfo.setCategoryID(CommonUtils.isNotBlank(catID) ? Long.parseLong(catID) : 0L);
        return productInfo;
    }

    @Override
    protected List<AlibabaProductProductSKUInfoPlus> getAlibabaProductSKUInfos(Double defaultPrice, JSONObject valItemInfo, AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto) {
        throw  new RuntimeException("asa");
    }

    @Override
    public Long getCategoryId(String content) {
        Pattern CATEGORY_ID = Pattern.compile("categoryId:([\\d]*)");
        final String s = ReUtil.get(CATEGORY_ID, content, 1);
        return StringUtils.isNotBlank(s)?Long.parseLong(s):0L;
    }

    public Double getPrice(AlibabaProductProductInfoPlus productInfo) {
        final String[] split = productInfo.getReferencePrice().split("-");
        return Double.parseDouble(split.length > 1 ? split[1] : split[0]);
    }



    protected String getPrice(String html) {
        Pattern PRODUCT_ID = Pattern.compile("defaultItemPrice\\\":\\\"([\\d+\\.\\d+]*)([-]?)([\\d+\\.\\d+]*)\\\"");
        String s = ReUtil.get(PRODUCT_ID, html, 0);
        Pattern compile = Pattern.compile("(\\d*\\.\\d*)([-]?)(\\d+\\.\\d+)|(\\d*\\.\\d*)");
        String s1 = ReUtil.get(compile, s, 0);
        if(log.isDebugEnabled()){
            log.debug("  {} {} ",s,s1);
        }
        return  s1;
    }



    /**
     * 从详情拿到图片
     */
    public static List<String> getDImage(String detailedData) {
        return ReUtil.findAll("(https://.+?\\.(jpg|png))", detailedData, 1).stream().filter(PingduoduoMetaDateMap::isDImage).collect(Collectors.toList());
    }

    private static boolean isDImage(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        // 严格校验 非贪婪模式
        final boolean match = ReUtil.isMatch("https://(img|cbu01).alicdn.com/*/.+?\\.jpg", url);
        if (!match) {
            log.info("[filter-image]: {}", url);
        }
        return match;
    }

    private String getSubject(Document document) {
        String subject  = document.getElementById("J_DetailMeta").select("div.tb-detail-hd").text();
        log.info("[alibaba-product] - subject: {}", subject);
        return subject;
    }

    /**
     * 多拿点图片
     *
     * @param topGallery
     * @return
     */
    private AlibabaProductProductImageInfo getImage(JSONArray topGallery) {

        final AlibabaProductProductImageInfo imageInfo = new AlibabaProductProductImageInfo();

        List<String> collect = topGallery.stream()
                .map(Object::toString)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        String[] images = collect.stream().map(e -> {
            if (e.indexOf("url") >= 0) {
                PingduoduoImages pingduoduoImages = JSON.parseObject(e, PingduoduoImages.class);
                return pingduoduoImages.getUrl();
            }
            return e;
        }).filter(CommonUtils::isNotBlank).toArray(String[]::new);
        imageInfo.setImages(images);

        log.info("[alibaba-product] - image: {}", topGallery);

        return imageInfo;
    }

    private String getSrc(Element e) {
         String src = e.attr("src");
        if(StringUtils.isBlank(src)){
            src = e.attr("data-src");
        }
//        src = src.substring(src.indexOf(TaobaoMetaDataConvert.COM_PREFIX));
        if(!src.startsWith(TaobaoMetaDataConvert.HTTPS_PREFIX)){
            src = TaobaoMetaDataConvert.HTTPS_PREFIX+src;
        }
        src = src.substring(0, src.lastIndexOf("_")) + "_800x800.jpg";
        if(!src.endsWith(TaobaoMetaDataConvert.JPG_PREFIX)){
            src = src.substring(0,src.lastIndexOf("."))+ TaobaoMetaDataConvert.JPG_PREFIX;
        }
        return src;
    }

    private boolean isImage(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        return true;
    }

    /**
     * 获取跨境属性
     */
    private LinkedHashMap<String, String> getCrossBorderAttributeValueMap(Document document) {
        final Elements spans = document.select("#site_content > div.grid-main > div > div.mod.mod-offerDetailContext2.app-offerDetailContext2.mod-ui-not-show-title > div > div.m-content > div > div > div > div > div.widget-custom.offerdetail_ditto_otherAttr > div > div > div.detail-other-attr-content > div > dl > dd > span");
        if (spans == null || spans.size() == 0) {
            return null;
        }

        final LinkedHashMap<String, String> crossBorderAttributeValueMap = new LinkedHashMap<>((int) (Math.ceil(spans.size() / 0.75)));
        for (Element span : spans) {
            crossBorderAttributeValueMap.put(span.child(0).text(), span.child(1).text());
        }
        return crossBorderAttributeValueMap;
    }

    /**
     * 获取变体JSON字符串
     */
    protected AlibabaProductProductSKUInfoPlus[] getSkuInfos(JSONObject goodsObject, Double defaultPrice, MetaDataObject.CollectController collectController) {
        final AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto = new AlibabaSkuMateDataMapDto();
        //   todo  暂停
        List<AlibabaProductProductSKUInfoPlus> skuProps = Collections.EMPTY_LIST;

        skuProps = getAlibabaProductSKUInfos(defaultPrice, goodsObject, alibabaSkuMateDataMapDto,collectController);

        log.info("[alibaba-product] : skuInfos: {}", skuProps.size());
        if (log.isDebugEnabled()) {
            log.debug("[alibaba-product] : skuInfos: {}", JSONObject.toJSONString(skuProps, SerializerFeature.PrettyFormat));
        }
        return skuProps.toArray(new AlibabaProductProductSKUInfoPlus[skuProps.size()]);
    }


    @Override
    protected List<AlibabaProductProductSKUInfoPlus> getSkuInfos(String content, Document document, Double defaultPrice) {
        return null;
    }


    protected List<AlibabaProductProductSKUInfoPlus> getAlibabaProductSKUInfos(Double defaultPrice, JSONObject valItemInfo, AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto, MetaDataObject.CollectController collectController) {
        final ArrayList<AlibabaProductProductSKUInfoPlus> skuMap = Stream.of(valItemInfo)
                .parallel()
                .filter(Objects::nonNull)
                .flatMap(jsonObject -> jsonObject.getJSONArray("skus").stream())
                .map((Object sku) -> this.initAlibabaProductProductSKUInfo(defaultPrice, sku,alibabaSkuMateDataMapDto,collectController))
                .peek(skuProp -> fillAlibabaProductSKUAttrInfos(alibabaSkuMateDataMapDto, skuProp))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(
                                Comparator.comparing(PingduoduoMetaDateMap::filter).thenComparingInt(AlibabaProductProductSKUInfoPlus::getIndex)
                        )), ArrayList::new));
        final int length = skuMap.iterator().next().getAttributes().length;
        return PingduoduoMetaDateMap.sorted(skuMap.stream(),length).collect(Collectors.toList());

    }

    @Override
    public void fillDiscountPriceAndStock(AlibabaProductProductInfoPlus skuProps, JSONObject originalDiscount, MetaDataObject.CollectController collectController) {

    }

    @Override
    public void fillDiscountPriceAndStock(List<AlibabaProductProductSKUInfoPlus> skuProps, JSONObject originalDiscount, MetaDataObject.CollectController collectController) {

    }


    protected AlibabaProductProductSKUInfoPlus initAlibabaProductProductSKUInfo(Double defaultPrice, Object sku, AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto, MetaDataObject.CollectController collectController) {
        final JSONObject skuInfo = (JSONObject) sku;
        final AlibabaProductProductSKUInfoPlus skuInfoPlus = new AlibabaProductProductSKUInfoPlus();

        skuInfoPlus.setStock(collectController.isCollectStock()?skuInfo.getInteger("quantity"):collectController.getStock());
        final Double normalPrice = skuInfo.getDouble(collectController.isCollectDiscount()?"groupPrice":"normalPrice");

        skuInfoPlus.setSoldQuantity(skuInfo.getInteger("soldQuantity"));
        skuInfoPlus.setPrice(Objects.nonNull(normalPrice)?normalPrice:defaultPrice);
        skuInfoPlus.setSkuId( skuInfo.getLong("skuId"));
        final String image = skuInfo.getString("thumbUrl");

        final boolean[] isImage = {false};
        if(Objects.nonNull(image)){
            isImage[0] = true;
            alibabaSkuMateDataMapDto.set(true);
        }
        final JSONArray specs = skuInfo.getJSONArray("specs");
        final List<AlibabaProductSKUAttrInfoPlus> collect = specs.stream().map(spec -> {
            final AlibabaProductSKUAttrInfoPlus skuAttrInfo1 = new AlibabaProductSKUAttrInfoPlus();
            final JSONObject specInfo = (JSONObject) spec;
            skuAttrInfo1.setAttributeDisplayName(specInfo.getString("spec_key"));
            skuAttrInfo1.setAttributeValue(specInfo.getString("spec_value"));
            skuAttrInfo1.setAttributeName(specInfo.getString("spec_key_id"));
            if(isImage[0]){
                isImage[0] = false;
                skuAttrInfo1.setSkuImageUrl(image);
            }
            skuAttrInfo1.setIndex(alibabaSkuMateDataMapDto.getNextCursor());
            return  skuAttrInfo1;
        }).collect(Collectors.toList());
        skuInfoPlus.setSpecId(collect.stream().map(AlibabaProductSKUAttrInfoPlus::getAttributeName).collect(Collectors.joining(";")));
        skuInfoPlus.setAttributes(collect.toArray(new AlibabaProductSKUAttrInfoPlus[collect.size()]));
        return skuInfoPlus;
    }



    public static  String filter(AlibabaProductProductSKUInfoPlus keyExtractor){
        final AlibabaProductSKUAttrInfoPlus[] attributes = keyExtractor.getAttributes();
        StringBuffer stringBuffer=new StringBuffer(":");
        for (AlibabaProductSKUAttrInfoPlus attribute : attributes) {
            stringBuffer.append(attribute.getAttributeName());
        }
        return stringBuffer.toString();
    }

    public static  Stream<AlibabaProductProductSKUInfoPlus> sorted(Stream<AlibabaProductProductSKUInfoPlus> stream, int length){
        if(length == 0 ){
            return  stream;
        }else if (length == 1){
            return stream.sorted(Comparator.comparingInt(value -> value.getAttributes()[0].getIndex()));
        }else{
            return stream.sorted(Comparator.comparingInt(value -> value.getAttributes()[0].getIndex()))
                    .sorted(Comparator.comparingInt(value -> value.getAttributes()[1].getIndex()));
        }
    }


    /**
     * 获取属性值
     */
    private AlibabaProductProductAttribute[] getAttributes(JSONObject goodInfo ) {
        JSONArray goodsProperty =  goodInfo.getJSONArray("goodsProperty");

        final List<AlibabaProductProductAttribute> attributes = new ArrayList<>(goodsProperty.size());
        for (Object jsonObject : goodsProperty) {
            final JSONObject property = (JSONObject) jsonObject;
            final AlibabaProductProductAttribute attribute = new AlibabaProductProductAttribute();
            attribute.setAttributeName(property.getString("key"));
            attribute.setValue(property.getJSONArray("values").toString());
            attributes.add(attribute);
        }

        final String goodsDesc = goodInfo.getString("goodsDesc");
        if(Objects.nonNull(goodsDesc)){
            final AlibabaProductProductAttribute attribute = new AlibabaProductProductAttribute();
            attribute.setAttributeName("Desc");
            attribute.setValue(goodsDesc);
            attributes.add(attribute);
        }

        log.info("[alibaba-product] : attributes: {}", attributes.size());

        return attributes.toArray(new AlibabaProductProductAttribute[attributes.size()]);
    }


    @Data
    public static class PingduoduoImages {
        private String id;
        private String url;
    }
}
