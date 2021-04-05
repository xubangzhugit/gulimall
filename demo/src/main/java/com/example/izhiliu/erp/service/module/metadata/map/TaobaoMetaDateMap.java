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
public class TaobaoMetaDateMap extends AbstractAlibabaProductMetaDateMap {

    private static final Logger log = LoggerFactory.getLogger(TaobaoMetaDateMap.class);

    @Override
    public AlibabaProductProductInfoPlus map(MetaDataObject pageHtml) {
        final Document document = Jsoup.parse(pageHtml.getHtml());
        final String html = squeeze(pageHtml.getHtml());
        final AlibabaProductProductInfoPlus productInfo = new AlibabaProductProductInfoPlus();

        /*
         * 配置化是否需要描述里的图片
         */
        if (Objects.nonNull(pageHtml)) {
            productInfo.setDescription(pageHtml.getContent());
        }
        productInfo.setSubject(getSubject(document));
        productInfo.setImage(getImage(document));
        productInfo.setAttributes(getAttributes(pageHtml.getHtml(), document));
        productInfo.setProductID(getProductId2(document, html));
        productInfo.setReferencePrice(getPrice(document));
        productInfo.setShippingInfo(getShippingInfo(html));


        final List<AlibabaProductProductSKUInfoPlus> skuInfos = getSkuInfos(pageHtml.getHtml(), document, getPrice(productInfo));
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

    @Override
    public Long getCategoryId(String content) {
        Pattern CATEGORY_ID = Pattern.compile("data-catid=\\\"([\\d]*)\\\"");
        final String s = ReUtil.get(CATEGORY_ID, content, 1);
        return StringUtils.isNotBlank(s) ? Long.parseLong(s) : 0L;
    }

    protected Double getPrice(AlibabaProductProductInfoPlus productInfo) {
        final String[] split = productInfo.getReferencePrice().split("-");
        return Double.parseDouble(split.length > 1 ? split[1] : split[0]);
    }


    protected String getPrice(Document document) {
        // #J_StrPrice > em.tb-rmb-num
        return document.getElementById("J_StrPrice").getElementsByClass("tb-rmb-num").text();
    }


    private Long getProductId2(Document document, String content) {
        final Element j_pine = document.getElementById("J_Pine");
        if (Objects.nonNull(j_pine)) {
            String productId = j_pine.attr("data-itemid");
            if (StringUtils.isNoneBlank(productId)) {
                return Long.parseLong(productId);
            }
        }
        Pattern PRODUCT_ID = Pattern.compile("data-itemid=\\\"([\\d]*)\\\"");
        return Long.parseLong(ReUtil.get(PRODUCT_ID, content, 1));
    }


    private String getSellerId(String content) {
        Pattern PRODUCT_ID = Pattern.compile("data-sellerid=\\\"([\\d]*)\\\"");
        return ReUtil.get(PRODUCT_ID, content, 1);
    }

    private String getProviderID(String content) {
        Pattern PRODUCT_ID = Pattern.compile("data-shopid=\\\"([\\d]*)\\\"");
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


    private String getSubject(Document document) {
        String subject = document.select("#J_Title > h3").text();

        log.info("[alibaba-product] - subject: {}", subject);

        return subject;
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
        /**
         *  default https://img.alicdn.com/imgextra/i2/725677994/O1CN01Kw9FTZ28vIhclUmmD_!!0-item_pic.jpg_60x60q90.jpg
         *  [ _ ]  as to : https://img.alicdn.com/imgextra/i2/725677994/O1CN01Kw9FTZ28vIhclUmmD_!!0-item_pic.jpg
         *  add [ _400x400.jpg] : https://img.alicdn.com/imgextra/i2/725677994/O1CN01Kw9FTZ28vIhclUmmD_!!0-item_pic.jpg_400x400.jpg
         *
         */
        src = src.substring(0, src.lastIndexOf("_")) + "_800x800.jpg";
        if (!src.endsWith(TaobaoMetaDataConvert.JPG_PREFIX)) {
            src = src.substring(0, src.lastIndexOf(".")) + TaobaoMetaDataConvert.JPG_PREFIX;
        }
        return src;
    }

    /**
     * 多拿点图片
     *
     * @param document
     * @return
     */
    private AlibabaProductProductImageInfo getImage(Document document) {
        List<String> images = document.getElementsByClass("tb-gallery").stream()
                .flatMap(element -> Stream.of(element.getElementsByTag("img").toArray()))
                .map(e -> getSrc((Element) e))
                .collect(Collectors.toList());
        final AlibabaProductProductImageInfo imageInfo = new AlibabaProductProductImageInfo();

        imageInfo.setImages(images.stream().filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList()).toArray(new String[images.size()]));

        log.info("[alibaba-product] - image: {}", images);

        return imageInfo;
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
        final Element j_isku = document.getElementById("J_isku");
        if(Objects.isNull(j_isku)){
            return null;
        }
        //  处理属性值
        final Elements j_prop = document.getElementById("J_isku").getElementsByClass("J_Prop");
        //  解析获取对应的属性值
        for (Element element : j_prop) {
            grabSkuAttrInfo(alibabaSkuMateDataMapDto, element);
        }

        //  处理sku
        List<AlibabaProductProductSKUInfoPlus> skuProps = Collections.EMPTY_LIST;
        final String skuString = ReUtil.get("\'sku\', (\\{.+)}*\'desc\'", content, 0);
        if (skuString == null) {
            return null;
        }
        final String substring = skuString.substring(skuString.indexOf("{"), skuString.lastIndexOf("}") + 1).replace("\n", "").replace(" ", "");
        final JSONObject skuData = JSONObject.parseObject(substring);
        JSONObject valItemInfo = skuData.getJSONObject("valItemInfo");
        if (valItemInfo.size() > 0) {
            skuProps = getAlibabaProductSKUInfos(defaultPrice, valItemInfo, alibabaSkuMateDataMapDto);
        }

        log.info("[alibaba-product] : skuInfos: {}", skuProps.size());
        if (log.isDebugEnabled()) {
            log.debug("[alibaba-product] : skuInfos: {}", JSONObject.toJSONString(skuProps, SerializerFeature.PrettyFormat));
        }

        return skuProps;
    }

    @Override
    public void fillDiscountPriceAndStock(List<AlibabaProductProductSKUInfoPlus> skuProps, JSONObject originalDiscount, MetaDataObject.CollectController collectController) {
//        final String discountString = originalDiscount.substring(originalDiscount.indexOf("(")+1, originalDiscount.lastIndexOf(")")).replace("\n", "").replace(" ", "");
//        final JSONObject jsonObject = JSONObject.parseObject(originalDiscount).getJSONObject("data");
        originalDiscount = originalDiscount.getJSONObject("data");
        //  价格
        final JSONObject promoData = originalDiscount.getJSONObject("promotion").getJSONObject("promoData");
//        final Double defPrice = promoData.getJSONArray("def").getJSONObject(0).getDouble("price");

        //  库存
        final JSONObject sku = originalDiscount.getJSONObject("dynStock").getJSONObject("sku");

        skuProps.forEach(alibabaProductProductSKUInfoPlus -> {
//            boolean isDedPrice = true;
            final String specId = alibabaProductProductSKUInfoPlus.getSpecId();

            if (Objects.nonNull(specId)) {
                if (collectController.isCollectDiscount()) {
                    //   处理 价格
                    final JSONArray jsonArray = promoData.getJSONArray(specId);
                    if (Objects.nonNull(jsonArray)) {
//                        isDedPrice = false;
                        //  兼容以前的 代码
//                        alibabaProductProductSKUInfoPlus.setOriginalPrice(alibabaProductProductSKUInfoPlus.getPrice());
                        alibabaProductProductSKUInfoPlus.setPrice(jsonArray.getJSONObject(0).getDouble("price"));
                    }
                }
                if (collectController.isCollectStock()) {
                    //   处理  库存
                    final JSONObject skuJsonArray = sku.getJSONObject(specId);
                    if (Objects.nonNull(skuJsonArray)) {
                        alibabaProductProductSKUInfoPlus.setStock(skuJsonArray.getInteger("stock"));
                    }
                } else {
                    alibabaProductProductSKUInfoPlus.setStock(collectController.getStock());
                }
            }

//            if(isDedPrice){
//                alibabaProductProductSKUInfoPlus.setPrice(defPrice);
//            }
        });
    }

    @Override
    public void fillDiscountPriceAndStock(AlibabaProductProductInfoPlus alibabaProductProductInfoPlus, JSONObject originalDiscount, MetaDataObject.CollectController collectController) {
        if (Objects.nonNull(originalDiscount)) {
            originalDiscount = originalDiscount.getJSONObject("data");
            //  价格
            final JSONObject promoData = originalDiscount.getJSONObject("promotion").getJSONObject("promoData");
            if (collectController.isCollectDiscount()) {
                //   处理 价格
                final JSONArray jsonArray = promoData.getJSONArray("def");
                if (Objects.nonNull(jsonArray)) {
//                        isDedPrice = false;
                    //  兼容以前的 代码
                    alibabaProductProductInfoPlus.setReferencePrice(jsonArray.getJSONObject(0).getString("price"));
                }
            }
            if (collectController.isCollectStock()) {
                //   处理  库存
                    alibabaProductProductInfoPlus.setStock(originalDiscount.getJSONObject("dynStock").getInteger("stock"));
            } else {
                alibabaProductProductInfoPlus.setStock(collectController.getStock());
            }
        }
    }

            /**
             *
             * @param defaultPrice
             * @param valItemInfo
             * @param alibabaSkuMateDataMapDto
             * @return
             */
            @Override
            protected List<AlibabaProductProductSKUInfoPlus> getAlibabaProductSKUInfos (Double defaultPrice, JSONObject
            valItemInfo, AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto){
                final ArrayList<AlibabaProductProductSKUInfoPlus> skuMap = Stream.of(valItemInfo)
                        .parallel()
                        .filter(Objects::nonNull)
                        .map(jsonObject -> jsonObject.getJSONObject("skuMap").getInnerMap())
                        .filter(Objects::nonNull)
                        .flatMap(skuMaps -> skuMaps.entrySet().stream())
                        .map(sku -> initAlibabaProductProductSKUInfo(defaultPrice, sku))
                        .peek(skuProp -> {
                            fillAlibabaProductSKUAttrInfos(alibabaSkuMateDataMapDto, skuProp);
                        })
                        .collect(Collectors.collectingAndThen(
                                Collectors.toCollection(() -> new TreeSet<>(
                                        Comparator.comparing(TianmaoMetaDateMap::filter).thenComparingInt(AlibabaProductProductSKUInfoPlus::getIndex)
                                )), ArrayList::new));
                final int length = skuMap.iterator().next().getAttributes().length;
                return TianmaoMetaDateMap.sorted(skuMap.stream(), length).collect(Collectors.toList());
            }


            /**
             *    根据解析的json  初始化一个产品SkuInfo
             * @param defaultPrice
             * @param sku
             * @return
             */
            @Override
            protected AlibabaProductProductSKUInfoPlus initAlibabaProductProductSKUInfo (Double
            defaultPrice, Map.Entry < String, Object > sku){
                final AlibabaProductProductSKUInfoPlus skuInfo = new AlibabaProductProductSKUInfoPlus();
                final String key = sku.getKey();
                final JSONObject value = (JSONObject) sku.getValue();
                skuInfo.setPrice(null == value.getDouble("price") ? defaultPrice : value.getDouble("price"));
                skuInfo.setSkuId(value.getLong("skuId"));
                skuInfo.setSpecId(key);
                return skuInfo;
            }

            /**
             *   抓取sku 属性信息
             * @param alibabaSkuMateDataMapDto
             */
            @Override
            protected void grabSkuAttrInfo (AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto, Element element){
                final String text = element.getElementsByClass("tb-property-type").text();
                final Elements elementsByClass = element.getElementsByTag("li");
                //                skuAttrInfo1.getSkuImageUrl()
                for (int i = 0; i < elementsByClass.size(); i++) {
                    Element element1 = elementsByClass.get(i);
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
                        skuAttrInfo1.setSkuImageUrl(source + "_800x800.jpg");
                    }
                    skuAttrInfo1.setIndex(alibabaSkuMateDataMapDto.getNextCursor());
                    if (log.isDebugEnabled()) {
                        log.debug("{}", JSON.toJSONString(skuAttrInfo1));
                    }
                    alibabaSkuMateDataMapDto.getSkuAttrInfos().put(value, skuAttrInfo1);
                }
            }


            /**
             * 获取属性值
             */
            protected AlibabaProductProductAttribute[] getAttributes (String documentText, Document document){
                List<AlibabaProductProductAttribute> attributes;
                try {
                    attributes = getAlibabaProductAttributes2(documentText);
                } catch (Exception e) {
//            e.printStackTrace();
                    attributes = getAlibabaProductAttributes1(document);
                }

                log.info("[alibaba-product] : attributes: {}", attributes.size());

                return attributes.toArray(new AlibabaProductProductAttribute[attributes.size()]);
            }

            private List<AlibabaProductProductAttribute> getAlibabaProductAttributes1 (Document document){
                List<AlibabaProductProductAttribute> attributes;
                final Element tbody = document.getElementById("attributes");
                Elements li = tbody.getElementsByTag("li");

                attributes = new ArrayList<>(tbody.children().size() * 3);

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
                return attributes;
            }

            private List<AlibabaProductProductAttribute> getAlibabaProductAttributes2 (String document){
                //  init
                int i = 0;
                String skuString = ReUtil.get("g_config.spuStandardInfo = (\\{.+)}*", document, 0);
                skuString = skuString.substring(0, skuString.indexOf("</script>"));
                skuString = skuString.replace("\n", "").replace(" ", "");
                final int beginIndex = skuString.indexOf("{");

                JSONObject jsonObject = null;
                skuString = skuString.substring(beginIndex, skuString.indexOf("};"));
                try {
                    final JSONObject jsonObject1 = JSONObject.parseObject(skuString);
                    if (Objects.nonNull(jsonObject1)) {
                        jsonObject = jsonObject1;
                    }
                } catch (Exception e) {

                }
                Objects.requireNonNull(jsonObject, "消息抓取失败");
                List<AlibabaProductProductAttribute> attributes = Stream.of(jsonObject)
                        .filter(Objects::nonNull)
                        .flatMap(jsonObject1 -> Stream.of(jsonObject1.getJSONArray("spuGroupInTab"), jsonObject1.getJSONArray("spuOtherInTab")))
                        .filter(Objects::nonNull)
                        .flatMap(jsonObject1 -> jsonObject1.stream())
                        .filter(Objects::nonNull)
                        .flatMap(o -> ((JSONObject) o).getJSONArray("spuStandardInfoUnits").stream())
                        .map(o -> {
                            JSONObject jsonObject1 = ((JSONObject) o);
                            final AlibabaProductProductAttribute attribute = new AlibabaProductProductAttribute();
                            attribute.setAttributeName(jsonObject1.getString("propertyName"));
                            attribute.setValue(jsonObject1.getString("valueName"));
                            return attribute;
                        })
                        .collect(Collectors.toList());
                return attributes;
            }
        }
