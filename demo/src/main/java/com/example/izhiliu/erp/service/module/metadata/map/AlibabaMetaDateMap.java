package com.izhiliu.erp.service.module.metadata.map;

import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.product.param.*;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataMap;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.TaobaoMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.dto.*;
import com.izhiliu.feign.client.VnService;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
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
public class AlibabaMetaDateMap implements MetaDataMap<String, AlibabaProductProductInfo>, ApplicationRunner {

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Resource
    private VnService vnService;


    private static final Logger log = LoggerFactory.getLogger(AlibabaMetaDateMap.class);
    private static final Pattern PRODUCT_PATTERN = Pattern.compile("offer/([\\d]*)\\.html");
    public AlibabaProductProductInfoPlus map(MetaDataObject pageHtml,String args){
        isLogin1688(pageHtml.getHtml());
        final Document document = Jsoup.parse(pageHtml.getHtml());
        final String html = squeeze(pageHtml.getHtml());
        final AlibabaProductProductInfoPlus productInfo = new AlibabaProductProductInfoPlus();
        productInfo.setSubject(getSubject(document));
        productInfo.setImage(getMianImages(document));
        /*
         * 描述
         */
        if (Objects.nonNull(pageHtml.getContent())) {
            productInfo.setDescription(pageHtml.getContent());
        }
        if(Objects.nonNull(args)){
            productInfo.setProductID(Long.parseLong(ReUtil.get(PRODUCT_PATTERN,args,1)));
        }
        productInfo.setProductID(getProductId(html));
        AlibabaProductProductInfo alibabaOfferInfo = getAlibabaOfferInfo(productInfo.getProductID());
        if (CommonUtils.isNotBlank(alibabaOfferInfo)) {
            productInfo.setAttributes(alibabaOfferInfo.getAttributes());
        } else {
            productInfo.setAttributes(getAttributes(document));
        }
        productInfo.setReferencePrice(getPrice(document,pageHtml.getHtml()));
        productInfo.setShippingInfo(getShippingInfo(html));
        productInfo.setSkuInfos(getSkuInfosPlus(html, getPrice(productInfo),productInfo,pageHtml.getCollectController()));
        //<meta property="og:product:nick" content="name=广州市增城联谊手袋厂; url= //paste666.cn.1688.com">
        productInfo.setSupplierLoginId(getProviderWangwang(document));
        //这里先将公司名放进去
        productInfo.setSupplierUserId(getProviderName(document,pageHtml.getHtml()));
        productInfo.setCategoryID(getCategoryId(html));
        return productInfo;
    }

    private AlibabaProductProductInfo getAlibabaOfferInfo(Long productID) {
        if (CommonUtils.isBlank(productID)) {
            return null;
        }
        try {
            return vnService.getAlibabaOfferInfo(productID).getBody();
        } catch (Exception e) {
        }
        return null;
    }

    private Long getProductId(String html) {
        Pattern CATEGORY_ID = Pattern.compile("offerid:'([\\d]*)'");
        String s = ReUtil.get(CATEGORY_ID, html, 1);

        //20191203
        if(null == s){
            CATEGORY_ID = Pattern.compile(getValue("productId"));
            s = ReUtil.get(CATEGORY_ID, html, 1);
        }
        return StringUtils.isNotBlank(s)?Long.parseLong(s):0L;
    }

    private void isLogin1688(String html) {
        Pattern login = Pattern.compile("loginchina-wrapper");
        final String needLogin = ReUtil.get(login, html, 0);
        if (Objects.nonNull(needLogin)){
            throw  new  RuntimeException("传入的 页面需要登录才可以 采集 needLogin");
        }
    }

    @Override
    public AlibabaProductProductInfoPlus map(String... args) {
      throw   new  RuntimeException( );
    }

    private AlibabaProductProductImageInfo getMianImages(Document document) {
        List<String> images = document.select("#dt-tab img").stream().map(e -> ReUtil.replaceAll(e.attr("src"), "\\d+x\\d+", "800x800")).filter(e -> !e.contains("lazyload")).collect(Collectors.toList());
        final List<String> lazyload = document.select("#dt-tab img").stream().flatMap(
                element -> Stream.of(element.attr("data-lazy-src"),element.attr("src")).filter(StringUtils::isNotBlank)
        ).map(e -> ReUtil.replaceAll(e, "\\d+x\\d+", "800x800")).filter(e -> !e.contains("lazyload")).collect(Collectors.toList());
        images.addAll(lazyload);

        final AlibabaProductProductImageInfo imageInfo = new AlibabaProductProductImageInfo();

        imageInfo.setImages(images.stream().filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList()).toArray(new String[images.size()]));

        log.info("[alibaba-product] - image: {}", images);

        return imageInfo;
    }

    @Override
    public Long getCategoryId(String content) {
        Pattern CATEGORY_ID = Pattern.compile("categoryId:\\\"([\\d]*)\\\"");
        String s = ReUtil.get(CATEGORY_ID, content, 1);

        //20191203
        if(null == s){
            CATEGORY_ID = Pattern.compile(getValue("categoryId"));
            s = ReUtil.get(CATEGORY_ID, content, 1);
        }

        return StringUtils.isNotBlank(s)?Long.parseLong(s):0L;
    }
    public Double getPrice(AlibabaProductProductInfo productInfo) {

        final String[] split = productInfo.getReferencePrice().split("-");
        return Double.parseDouble(split.length > 1 ? split[1] : split[0]);
    }

    private String squeeze(String html) {
        return html.replaceAll("\\s*|\t|\r|\n", "");
    }

    private String getPrice(Document document, String arg) {
        final Elements select = document.select("meta[property=og:product:price");
        if(!CollectionUtils.isEmpty(select)){
            return  select.attr("content");
        }
        String price = ReUtil.get("'refPrice':'([\\d+\\.\\d+]*)([-]?)([\\d+\\.\\d+]*)'", arg, 1);
        if(Objects.isNull(price)){
            price = ReUtil.get(get("price"), arg, 1);
        }
        return price;
    }

    private String getProviderName(Document document, String arg){
        final Elements select = document.select("meta[property=og:product:nick");
        if(!CollectionUtils.isEmpty(select)){
            String content = select.attr("content");
            int i = content.indexOf("=");
            int i1 = content.indexOf(";");
            return content.substring(i+1,i1);
        }
        return ReUtil.get("'loginId':'([\\S]*)'", arg, 1);
    }

    private String getProviderWangwang(Document document){
        return document.select("div[id=usermidid").text();
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
    public static List<String> getDescImage(String detailedData) {
        return ReUtil.findAll("(//(img|cbu01|cbu02|cbu03|assets).alicdn.com/*/.+?\\.(jpg|png|gif|jpge))", detailedData, 1).stream().filter(AlibabaMetaDateMap::isDescImage).map(
                src -> {
                    if (!src.startsWith(TaobaoMetaDataConvert.HTTPS_PREFIX)) {
                    return  src = TaobaoMetaDataConvert.HTTPS_PREFIX + src;
                }
                return src;
                }
        ).collect(Collectors.toList());
    }
    public static List<String> getGreedyImage(String detailedData) {
        return new ArrayList<>(ReUtil.findAll("(https://.+?\\.(jpg|png|gif|jpge))", detailedData, 1));
    }

    private static boolean isDescImage(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        // 严格校验 非贪婪模式
        final boolean match = ReUtil.isMatch("//(img|cbu01).alicdn.com/*/.+?\\.(jpg|png|gif|jpge)", url);
        if (!match) {
            log.info("[filter-image]: {}", url);
        }
        return match;
    }

    private String getSubject(Document document) {
        final String subject = document.select("#mod-detail-title > h1").text();

        log.info("[alibaba-product] - subject: {}", subject);

        return subject;
    }

    /**
     * 获取变体JSON字符串
     */
    private AlibabaProductProductSKUInfo[] getSkuInfos(String content, Double defaultPrice) {
        final String skuString = ReUtil.get("\"sku\":(\\{.+)};iDetailData.allTagIds?", content, 1);
        if (skuString == null) {
            return null;
        }

        final JSONObject skuData = JSON.parseObject(skuString);
        final JSONArray skuProps = skuData.getJSONArray("skuProps");
        final Map<String, Object> skuMap = new TreeMap<>(skuData.getJSONObject("skuMap").getInnerMap());

        log.info("[skuInfo]: {}", skuData.toJSONString());

        final List<AlibabaProductProductSKUInfo> skuInfos = new ArrayList<>(skuMap.size());
        for (Map.Entry<String, Object> sku : skuMap.entrySet()) {
            final AlibabaProductProductSKUInfo skuInfo = new AlibabaProductProductSKUInfo();

            final String key = sku.getKey();
            final JSONObject value = JSON.parseObject(JSON.toJSONString(sku.getValue()));
            skuInfo.setPrice(null == value.getDouble("price")?defaultPrice:value.getDouble("price"));
            skuInfo.setSkuId(value.getLong("skuId"));
            skuInfo.setSpecId(value.getString("specId"));
//            skuInfo.setAmountOnSale(null == value.getInteger("canBookCount")?0:value.getInteger("canBookCount"));

            // 双SKU
            final String[] skuTier = key.split("&gt;");
            if (skuTier.length > 1) {
                final AlibabaProductSKUAttrInfo skuAttrInfo1 = new AlibabaProductSKUAttrInfo();
                skuAttrInfo1.setAttributeDisplayName(skuProps.getJSONObject(0).getString("prop"));
                skuAttrInfo1.setAttributeValue(skuTier[0]);
                skuAttrInfo1.setSkuImageUrl(skuProps.getJSONObject(0).getJSONArray("value").stream().map(JSONObject.class::cast).filter(v-> v.getString("name").equals(skuTier[0])).findFirst().map(v->v.getString("imageUrl")).orElse(""));

                final AlibabaProductSKUAttrInfo skuAttrInfo2 = new AlibabaProductSKUAttrInfo();
                skuAttrInfo2.setAttributeDisplayName(skuProps.getJSONObject(1).getString("prop"));
                skuAttrInfo2.setAttributeValue(skuTier[1]);

                skuInfo.setAttributes(new AlibabaProductSKUAttrInfo[]{skuAttrInfo1, skuAttrInfo2});
            } else {
                final AlibabaProductSKUAttrInfo skuAttrInfo1 = new AlibabaProductSKUAttrInfo();
                skuAttrInfo1.setAttributeDisplayName(skuProps.getJSONObject(0).getString("prop"));
                skuAttrInfo1.setAttributeValue(skuTier[0]);
                skuAttrInfo1.setSkuImageUrl(skuProps.getJSONObject(0).getJSONArray("value").stream().map(JSONObject.class::cast).filter(v-> v.getString("name").equals(skuTier[0])).findFirst().map(v->v.getString("imageUrl")).orElse(""));
                skuInfo.setAttributes(new AlibabaProductSKUAttrInfo[]{skuAttrInfo1});
            }

            skuInfos.add(skuInfo);
        }

        log.info("[alibaba-product] : skuInfos: {}", skuInfos.size());

        return skuInfos.toArray(new AlibabaProductProductSKUInfo[0]);
    }


    /**
     * 获取变体JSON字符串
     */
    private AlibabaProductProductSKUInfoPlus[] getSkuInfosPlus(String content, Double defaultPrice, AlibabaProductProductInfoPlus alibabaProductProductInfoPlus, MetaDataObject.CollectController collectController) {
        final String skuString = ReUtil.get(getValue("sku"), content, 1);
        if (skuString == null) {
            return null;
        }
        final JSONObject sku1 = JSON.parseObject(skuString);
        final JSONObject skuData = sku1.getJSONObject("sku");
        if (skuData == null) {
            return null;
        }
        final JSONArray skuProps = skuData.getJSONArray("skuProps");
        final JSONArray priceRange = skuData.getJSONArray("priceRange");
        if(Objects.nonNull(priceRange)){
            // todo   暂时不需要 采集批发价格
//            fillpPriceRange(priceRange,alibabaProductProductInfoPlus);
        }
//        final JSONArray priceRange = skuData.getJSONArray("priceRangeOriginal");
//        fillpPriceRange(priceRange,alibabaProductProductInfoPlus);
        final AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto = new AlibabaSkuMateDataMapDto();
        List<AlibabaProductSKUAttrInfoPlus> productSKUAttrInfoPluses = Stream.of(skuProps)
                .flatMap(objects -> objects.stream())
                .map(JSONObject.class::cast)
                .flatMap(jsonObject ->
                        jsonObject.getJSONArray("value").stream()
                                .map(JSONObject.class::cast)
                                .map(object -> {
                                    object.put("index", alibabaSkuMateDataMapDto.getNextCursor());
                                    AlibabaProductSKUAttrInfoPlus alibabaProductSKUAttrInfoPlus = new AlibabaProductSKUAttrInfoPlus();
                                    alibabaProductSKUAttrInfoPlus.setIndex(alibabaSkuMateDataMapDto.getNextCursor());
                                    alibabaProductSKUAttrInfoPlus.setAttributeValue(object.getString("name"));
                                    alibabaProductSKUAttrInfoPlus.setSkuImageUrl(object.getString("imageUrl"));
                                    alibabaProductSKUAttrInfoPlus.setAttributeDisplayName(jsonObject.getString("prop"));
                                    return alibabaProductSKUAttrInfoPlus;
                                })
                )
                .collect(Collectors.toList());
        final Map<String, AlibabaProductSKUAttrInfoPlus> collect = productSKUAttrInfoPluses.stream()
                .collect(Collectors.toMap(AlibabaProductSKUAttrInfoPlus::getAttributeValue, Function.identity(),  (alibabaProductSKUAttrInfoPlus, alibabaProductSKUAttrInfoPlus2) -> alibabaProductSKUAttrInfoPlus));

        //存在两层sku出现相同属性的情况 ex:自然色-自然色
        boolean noDisount = productSKUAttrInfoPluses.size() == collect.size();
        final Map<String, Object> skuMap = new TreeMap<>(skuData.getJSONObject("skuMap").getInnerMap());


        final List<AlibabaProductProductSKUInfoPlus> skuInfos = new ArrayList<>(skuMap.size());
        final boolean collectStock = collectController.isCollectStock();
        for (Map.Entry<String, Object> sku : skuMap.entrySet()) {
            final AlibabaProductProductSKUInfoPlus skuInfo = new AlibabaProductProductSKUInfoPlus();

            final String key = sku.getKey();
            final JSONObject value = JSON.parseObject(JSON.toJSONString(sku.getValue()));
            skuInfo.setPrice(null == value.getDouble("price")?defaultPrice:value.getDouble("price"));
            skuInfo.setSkuId(value.getLong("skuId"));
            skuInfo.setSpecId(value.getString("specId"));
            if(collectStock){
                skuInfo.setStock(value.getInteger("canBookCount"));
            }else{
                skuInfo.setStock(collectController.getStock());
            }
//            skuInfo.setAmountOnSale(null == value.getInteger("canBookCount")?0:value.getInteger("canBookCount"));

            // 双SKU
            final String[] skuTier = key.split("&gt;");
            final AlibabaProductSKUAttrInfoPlus[] alibabaProductSKUAttrInfos=new AlibabaProductSKUAttrInfoPlus[skuTier.length];
            for (int i = 0; i <skuTier.length; i++) {
                AlibabaProductSKUAttrInfoPlus alibabaProductSKUAttrInfoPlus = new AlibabaProductSKUAttrInfoPlus();
                if (noDisount) {
                    alibabaProductSKUAttrInfoPlus = collect.get(skuTier[i]);
                } else {
                    final String s = skuTier[i];
                    List<AlibabaProductSKUAttrInfoPlus> productSKUAttrInfoPlusList = productSKUAttrInfoPluses.stream()
                            .filter(e -> Objects.equals(e.getAttributeValue(), s))
                            .collect(Collectors.toList());
                    alibabaProductSKUAttrInfoPlus = productSKUAttrInfoPlusList.size() > i ? productSKUAttrInfoPlusList.get(i) : productSKUAttrInfoPlusList.get(0);
                }
                alibabaProductSKUAttrInfos[i] = alibabaProductSKUAttrInfoPlus;
            }
            skuInfo.setAttributes(alibabaProductSKUAttrInfos);
            skuInfos.add(skuInfo);
        }
        //   因为 不存在 2 个规格以上 所以可以提前进行 排序
        log.info("[alibaba-product] : skuInfos: {}", skuInfos.size());
        final int lengthc = skuInfos.iterator().next().getAttributes().length;
       return TianmaoMetaDateMap.sorted(skuInfos.stream(),lengthc)
                .collect(Collectors.toList()).toArray(new AlibabaProductProductSKUInfoPlus[0]);
    }

    private void fillpPriceRange(JSONArray priceRangeData, AlibabaProductProductInfoPlus alibabaProductProductInfoPlus) {
        PriceRange[] priceRanges = new PriceRange[priceRangeData.size()];
        int maxStock = 999;
        for (int i = priceRangeData.size() - 1; i >= 0; i--) {
            PriceRange productInfoPlus = new PriceRange();
            JSONArray jsonArray = (JSONArray) priceRangeData.get(i);
            final Integer integer = jsonArray.getInteger(0);
            productInfoPlus.setMin(integer);
            final float floatValue = jsonArray.getBigDecimal(1).floatValue();
            productInfoPlus.setPrice(floatValue);

            if (i != 0) {
                productInfoPlus.setMax(maxStock);
                maxStock = jsonArray.getInteger(0) - 1;
            } else {
                productInfoPlus.setMax(maxStock);
            }
            priceRanges[i] = productInfoPlus;
        }
        alibabaProductProductInfoPlus.setPriceRange(priceRanges);
    }

    /**
     * 获取属性值
     */
    private AlibabaProductProductAttribute[] getAttributes(Document document) {
        Elements select = document.select("#mod-detail-attributes > div.obj-content > table > tbody");
        if (CommonUtils.isBlank(select)) {
            return new AlibabaProductProductAttribute[]{};
        }
        final Element tbody = select.get(0);

        final List<AlibabaProductProductAttribute> attributes = new ArrayList<>(tbody.children().size() * 3);

        String key = "None";
        for (Element tr : tbody.children()) {
            for (Element td : tr.children()) {
                if (td.hasClass("de-value")) {
                    final AlibabaProductProductAttribute attribute = new AlibabaProductProductAttribute();
                    attribute.setAttributeName(key);
                    attribute.setValue(td.text());
                    attributes.add(attribute);
                } else {
                    key = td.text();
                }
            }
        }

        log.info("[alibaba-product] : attributes: {}", attributes.size());

        return attributes.toArray(new AlibabaProductProductAttribute[0]);
    }

    HashMap<String,String> stringStringHashMap =new HashMap<String,String>(){{

    }};

    private String get(String key){
        return stringStringHashMap.getOrDefault(key, "");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        stringStringHashMap.put("categoryId","\\\"categoryId\\\":([\\d]*)");
        stringStringHashMap.put("productId","\\\"offerid\\\":([\\d]*)");
        stringStringHashMap.put("sku","iDetailData=(\\{.+);iDetailData.allTagIds?");
        stringStringHashMap.put("price","refPrice:'([\\d+\\.\\d+]*)([-]?)([\\d+\\.\\d+]*)'");

        stringRedisTemplate.opsForHash().put("AlibabaMetaDateMap","sku", stringStringHashMap.get("sku"));
        stringRedisTemplate.opsForHash().put("AlibabaMetaDateMap","categoryId", stringStringHashMap.get("categoryId"));
        stringRedisTemplate.opsForHash().put("AlibabaMetaDateMap","productId", stringStringHashMap.get("productId"));
        stringRedisTemplate.opsForHash().put("AlibabaMetaDateMap","price", stringStringHashMap.get("price"));
    }

    public String  getValue(String key){
        final Object o = stringRedisTemplate.opsForHash().get("AlibabaMetaDateMap", key);
        if(Objects.isNull(o)){
            return  get(key);
        }
        return String.valueOf(o);
    }
}
