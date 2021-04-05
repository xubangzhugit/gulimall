package com.izhiliu.erp.service.module.metadata.map;

import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.product.param.*;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.TaobaoMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductSKUInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductSKUAttrInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaSkuMateDataMapDto;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
public class TianmaoMetaDateMap extends AbstractAlibabaProductMetaDateMap {

    private static final Logger log = LoggerFactory.getLogger(TianmaoMetaDateMap.class);

    @Override
    public AlibabaProductProductInfoPlus map(MetaDataObject pageHtml) {
        final Document document = Jsoup.parse(pageHtml.getHtml());
        final String html = squeeze(pageHtml.getHtml());
        final AlibabaProductProductInfoPlus productInfo = new AlibabaProductProductInfoPlus();

        /*
         * 描述
         */
        if (Objects.nonNull(pageHtml)) {
            productInfo.setDescription(pageHtml.getContent());
        }
        productInfo.setSubject(getSubject(document));
        productInfo.setImage(getImage(document));
        productInfo.setAttributes(getAttributes(document));
        productInfo.setProductID(getProductId2(document, html));
        productInfo.setReferencePrice(getPrice(html));
        productInfo.setShippingInfo(getShippingInfo(html));
         List<AlibabaProductProductSKUInfoPlus> skuInfos = Collections.EMPTY_LIST;
        if(isNotTakeOff(pageHtml.getHtml())){
            skuInfos = getSkuInfos(pageHtml.getHtml(), document, getPrice(productInfo));
        }
        if (CollectionUtils.isEmpty(skuInfos)) {
            fillDiscountPriceAndStock(productInfo, pageHtml.getDiscount(), pageHtml.getCollectController());
        }
        productInfo.setSkuInfos(
                discountPriceAndStockHandler(skuInfos
                        , pageHtml.getDiscount()
                        , pageHtml.getCollectController()));
        //<meta property="og:product:nick" content="name=广州市增城联谊手袋厂; url= //paste666.cn.1688.com">
        productInfo.setSupplierLoginId(getProviderID(html));
        //这里先将公司名放进去
        productInfo.setSupplierUserId(getSellerId(html));
        productInfo.setCategoryID(getCategoryId(html));
        return productInfo;
    }

    private boolean isNotTakeOff(String pageHtml) {
        Pattern CATEGORY_ID = Pattern.compile("\"auctionStatus\":\"([\\d]*)\"");
        final String s = ReUtil.get(CATEGORY_ID, pageHtml,1);
        if(Objects.nonNull(s)){
            final int i = Integer.parseInt(s);
            if( i != -2 ){
               return  true;
            }
        }
        return false;
    }

    @Override
    public Long getCategoryId(String content) {
        Pattern CATEGORY_ID = Pattern.compile("categoryId:([\\d]*)");
        final String s = ReUtil.get(CATEGORY_ID, content, 1);
        return StringUtils.isNotBlank(s) ? Long.parseLong(s) : 0L;
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
        if (log.isDebugEnabled()) {
            log.debug("  {} {} ", s, s1);
        }
        return s1;
    }


    private Long getProductId2(Document document, String content) {
//        String productId = document.getElementById("LineZing").attr("shopid");
//        if (StringUtils.isNoneBlank(productId)) {
//            return Long.parseLong(productId);
//        }
        Pattern PRODUCT_ID = Pattern.compile("itemId:\\\"([\\d]*)\\\"");
        final String s = ReUtil.get(PRODUCT_ID, content, 1);
        return StringUtils.isNotBlank(s)?Long.parseLong(s):0L;
    }

    private String getProviderId(String content) {
        Pattern PROVIDER_ID = Pattern.compile("'userId':'([\\d]*)',");
        return ReUtil.get(PROVIDER_ID, content, 1);
    }


    private String getSellerId(String content) {
        Pattern PRODUCT_ID = Pattern.compile("\\\"sellerId\\\":([\\d]*)");
        return ReUtil.get(PRODUCT_ID, content, 1);
    }

    private String getProviderID(String content) {
        Pattern PRODUCT_ID = Pattern.compile("shopid=\\\"([\\d]*)\\\"");
        return ReUtil.get(PRODUCT_ID, content, 1);
    }

    private AlibabaProductProductShippingInfo getShippingInfo(String content) {
        final String weights = ReUtil.get("offerWeightInf:(\\{.+?\\})", content, 1);
        if (weights != null) {
            final AlibabaProductProductShippingInfo shippingInfo = new AlibabaProductProductShippingInfo();
            shippingInfo.setUnitWeight(JSON.parseObject(weights).getDouble("weight"));
            return shippingInfo;
        }

        return null;
    }

    /**
     * 从详情拿到图片
     */
    public static List<String> getDImage(String detailedData) {
        return ReUtil.findAll("(https://.+?\\.(jpg|png))", detailedData, 1).stream().filter(TianmaoMetaDateMap::isDImage).collect(Collectors.toList());
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
        String subject = document.getElementById("J_DetailMeta").select("div.tb-detail-hd").text();
        log.info("[alibaba-product] - subject: {}", subject);
        return subject;
    }

    /**
     * 多拿点图片
     *
     * @param document
     * @return
     */
    private AlibabaProductProductImageInfo getImage(Document document) {
        List<String> images = document.getElementsByClass("tb-thumb-content").stream()
                .flatMap(element -> Stream.of(element.getElementsByTag("img").toArray()))
                .map(e -> getSrc((Element) e))
                .collect(Collectors.toList());

        final AlibabaProductProductImageInfo imageInfo = new AlibabaProductProductImageInfo();

        imageInfo.setImages(images.stream().filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList()).toArray(new String[images.size()]));

        log.info("[alibaba-product] - image: {}", images);

        return imageInfo;
    }

    private String getSrc(Element e) {
        String src = e.attr("src");
        if (StringUtils.isBlank(src)) {
            src = e.attr("data-src");
        }
//        src = src.substring(src.indexOf(TaobaoMetaDataConvert.COM_PREFIX));
        if (!src.startsWith(TaobaoMetaDataConvert.HTTPS_PREFIX)) {
            src = TaobaoMetaDataConvert.HTTPS_PREFIX + src;
        }
        src = src.substring(0, src.lastIndexOf("_")) + "_800x800.jpg";
        if (!src.endsWith(TaobaoMetaDataConvert.JPG_PREFIX)) {
            src = src.substring(0, src.lastIndexOf(".")) + TaobaoMetaDataConvert.JPG_PREFIX;
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
    @Override
    protected List<AlibabaProductProductSKUInfoPlus> getSkuInfos(String content, Document document, Double defaultPrice) {
        final AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto = new AlibabaSkuMateDataMapDto();
        final Elements j_prop = document.getElementsByClass("tm-sale-prop");
//        if(j_prop.isEmpty()){
//            throw  new RuntimeException("sku 属性采集失败");
//        }
        //  解析获取对
        j_prop.forEach(element -> {
            grabSkuAttrInfo(alibabaSkuMateDataMapDto, element);
        });
        //   todo  暂停
        List<AlibabaProductProductSKUInfoPlus> skuProps = Collections.EMPTY_LIST;
        String skuStringSku = ReUtil.get("\"skuList\":\\[(\\{.+)}*valLoginIndicator", content, 0);
        if (skuStringSku == null) {
            return null;
        }
        skuStringSku = "{" + skuStringSku.substring(0, skuStringSku.lastIndexOf(",")).replace("\n", "").replace(" ", "");
        final JSONObject skuObject = JSONObject.parseObject(skuStringSku);
        skuProps = getAlibabaProductSKUInfos(defaultPrice, skuObject, alibabaSkuMateDataMapDto);

        log.info("[alibaba-product] : skuInfos: {}", skuProps.size());
        if (log.isDebugEnabled()) {
            log.debug("[alibaba-product] : skuInfos: {}", JSONObject.toJSONString(skuProps, SerializerFeature.PrettyFormat));
        }

        return skuProps;
    }


    @Override
    public void fillDiscountPriceAndStock(List<AlibabaProductProductSKUInfoPlus> skuProps, JSONObject originalDiscount, MetaDataObject.CollectController collectController) {
//        final String discountString = originalDiscount.substring(originalDiscount.indexOf("(")+1, originalDiscount.lastIndexOf(")")).replace("\n", "").replace(" ", "");
//        final JSONObject jsonObject = JSONObject.parseObject(originalDiscount).getJSONObject("defaultModel");
        //  价格   天猫没有默认的 价格
        final JSONObject promoData = originalDiscount.getJSONObject("itemPriceResultDO").getJSONObject("priceInfo");

        ////  库存
        final JSONObject sku = originalDiscount.getJSONObject("inventoryDO").getJSONObject("skuQuantity");
//
        skuProps.forEach(alibabaProductProductSKUInfoPlus -> {
            final String skuId = alibabaProductProductSKUInfoPlus.getSkuId().toString();

            if (Objects.nonNull(skuId)) {
                if (collectController.isCollectDiscount()) {
                    final JSONObject jsonObject1 = promoData.getJSONObject(skuId);
                    if (Objects.nonNull(jsonObject1)) {
                        //  兼容以前的 代码
                        //alibabaProductProductSKUInfoPlus.setOriginalPrice(alibabaProductProductSKUInfoPlus.getPrice());
                        JSONArray promotionList = jsonObject1.getJSONArray("promotionList");
                        Double price = promotionList != null ? promotionList.getJSONObject(0).getDouble("price") : jsonObject1.getDouble("price");
                        alibabaProductProductSKUInfoPlus.setPrice(price);
//                        alibabaProductProductSKUInfoPlus.setPrice(jsonObject1.getJSONArray("promotionList").getJSONObject(0).getDouble("price"));
                    }
                }

                if (collectController.isCollectStock()) {
                    final JSONObject skuJsonArray = sku.getJSONObject(skuId);
                    if (Objects.nonNull(skuJsonArray)) {
                        alibabaProductProductSKUInfoPlus.setStock(skuJsonArray.getInteger("quantity"));
                    }
                } else {
                    alibabaProductProductSKUInfoPlus.setStock(collectController.getStock());
                }

            }
        });
    }

    @Override
    public void fillDiscountPriceAndStock(AlibabaProductProductInfoPlus alibabaProductProductInfoPlus, JSONObject originalDiscount, MetaDataObject.CollectController collectController) {
        if (Objects.nonNull(originalDiscount)) {
            //  价格   天猫没有默认的 价格
            final JSONObject promoData = originalDiscount.getJSONObject("itemPriceResultDO").getJSONObject("priceInfo");
            if (collectController.isCollectDiscount()) {
                final JSONObject jsonObject1 = promoData.getJSONObject("def");
                if (Objects.nonNull(jsonObject1)) {
                    //   处理 价格
                    if (Objects.nonNull(promoData)) {
                        final JSONArray promotionList = jsonObject1.getJSONArray("promotionList");
                        if(Objects.nonNull(promotionList)){
                            alibabaProductProductInfoPlus.setReferencePrice(promotionList.getJSONObject(0).getString("price"));
                        }else{
                            alibabaProductProductInfoPlus.setReferencePrice(jsonObject1.getString("price"));
                        }

                    }
                }
            }
            if (collectController.isCollectStock()) {
                //   处理  库存
                alibabaProductProductInfoPlus.setStock(originalDiscount.getJSONObject("inventoryDO").getInteger("totalQuantity"));
            } else {
                alibabaProductProductInfoPlus.setStock(collectController.getStock());
            }

        }

    }


    @Override
    protected List<AlibabaProductProductSKUInfoPlus> getAlibabaProductSKUInfos(Double defaultPrice, JSONObject valItemInfo, AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto) {
        final ArrayList<AlibabaProductProductSKUInfoPlus> skuMap = Stream.of(valItemInfo)
                .parallel()
                .filter(Objects::nonNull)
                .map(jsonObject -> jsonObject.getJSONObject("skuMap").getInnerMap())
                .filter(Objects::nonNull)
                .flatMap(skuMaps -> skuMaps.entrySet().stream())
                .map(sku -> initAlibabaProductProductSKUInfo(defaultPrice, sku))
                .peek(skuProp -> fillAlibabaProductSKUAttrInfos(alibabaSkuMateDataMapDto, skuProp))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(
                                Comparator.comparing(TianmaoMetaDateMap::filter).thenComparingInt(AlibabaProductProductSKUInfoPlus::getIndex)
                        )), ArrayList::new));
        final int length = skuMap.iterator().next().getAttributes().length;
        return TianmaoMetaDateMap.sorted(skuMap.stream(), length).collect(Collectors.toList());

    }


    @Override
    protected AlibabaProductProductSKUInfoPlus initAlibabaProductProductSKUInfo(Double defaultPrice, Map.Entry<String, Object> sku) {
        final AlibabaProductProductSKUInfoPlus skuInfo = new AlibabaProductProductSKUInfoPlus();
        final String key = sku.getKey();
        final JSONObject value = JSON.parseObject(JSON.toJSONString(sku.getValue()));
        skuInfo.setPrice(null == value.getDouble("price") ? defaultPrice : value.getDouble("price"));
        skuInfo.setSkuId(value.getLong("skuId"));
        skuInfo.setSpecId(key);
        return skuInfo;
    }

    @Override
    protected void grabSkuAttrInfo(AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto, Element element) {
        final String text = element.getElementsByClass("tb-metatit").text();
        final Elements elementsByClass = element.getElementsByTag("li");
        elementsByClass.forEach(element1 -> {
            final AlibabaProductSKUAttrInfoPlus skuAttrInfo1 = new AlibabaProductSKUAttrInfoPlus();
            skuAttrInfo1.setAttributeDisplayName(text);
            final String value = element1.attr("data-value");
            skuAttrInfo1.setAttributeName(value);
            final String span = element1.getElementsByTag("span").text();
            skuAttrInfo1.setAttributeValue(span);
            final String img = element1.getElementsByTag("a").iterator().next().attr("style");
            if (StringUtils.isNotBlank(img)) {
                if (!alibabaSkuMateDataMapDto.get()) {
                    alibabaSkuMateDataMapDto.set(true);
                    alibabaSkuMateDataMapDto.setImageIndexName(text);
                }
                final String source = img.substring(img.indexOf("(") + 1, img.lastIndexOf("_"));
                final String image = TaobaoMetaDataConvert.getImage(source);
                skuAttrInfo1.setSkuImageUrl(image + "_800x800.jpg");

            }
            skuAttrInfo1.setIndex(alibabaSkuMateDataMapDto.getNextCursor());
            if (log.isDebugEnabled()) {
                log.debug("{}", JSON.toJSONString(skuAttrInfo1));
            }
            alibabaSkuMateDataMapDto.getSkuAttrInfos().put(value, skuAttrInfo1);
        });
    }


    public static String filter(AlibabaProductProductSKUInfoPlus keyExtractor) {
        final AlibabaProductSKUAttrInfoPlus[] attributes = keyExtractor.getAttributes();
        StringBuffer stringBuffer = new StringBuffer(":");
        for (AlibabaProductSKUAttrInfoPlus attribute : attributes) {
            stringBuffer.append(attribute.getAttributeName());
        }
        return stringBuffer.toString();
    }

    public static Stream<AlibabaProductProductSKUInfoPlus> sorted(Stream<AlibabaProductProductSKUInfoPlus> stream, int length) {
        if (length == 0) {
            return stream;
        } else if (length == 1) {
            return stream.sorted(Comparator.comparingInt(value -> value.getAttributes()[0].getIndex()));
        } else {
            return stream.sorted(Comparator.comparingInt(value -> value.getAttributes()[0].getIndex()))
                    .sorted(Comparator.comparingInt(value -> value.getAttributes()[1].getIndex()));
        }
    }


    /**
     * 获取属性值
     */
    private AlibabaProductProductAttribute[] getAttributes(Document document) {
        final Element tbody = document.select("#attributes").get(0);
        Elements li = tbody.getElementsByTag("li");

        final List<AlibabaProductProductAttribute> attributes = new ArrayList<>(tbody.children().size() * 3);

        for (Element tr : li) {
            final AlibabaProductProductAttribute attribute = new AlibabaProductProductAttribute();
            final String text1 = tr.text();
            int indexOf = text1.indexOf(":");
            if (indexOf != -1) {
                final String key = text1.substring(0, indexOf);
                final String text = text1.substring(indexOf + 1);
                attribute.setAttributeName(key);
                attribute.setValue(text);
                attributes.add(attribute);
            }
        }

        log.info("[alibaba-product] : attributes: {}", attributes.size());

        return attributes.toArray(new AlibabaProductProductAttribute[attributes.size()]);
    }
}
