package com.izhiliu.erp.service.module.metadata.map;

import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.izhiliu.erp.service.module.metadata.convert.LazadaMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataMap;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

/**
 * 来赞达前台页面数据采集
 *  非接口请求数据, 数据存放在 'app.run(...)' 里, 在源代码底部可以看到
 *
 *  1. 从 app.run(...) 里取出 JSON 数据串.
 *  2. json里面有很多不需要的数据, 转成 JSON 对象后 'json.data.root.fields' 拿到真正 core 的数据.

 * <p>
 * 商品基本信息存放在 product
 *  product
 *      |—— link  链接
 *      |—— title 标题
 *      |—— desc  描述
 *
 * <p>
 * 商品属性是跟商品SKU走的,即多个SKU的属性可能不一样
 *  specifications
 *      |—— 0000001
 *          |—— features
 *              |—— key:value
 * <p>
 * 商品SKU有点复杂,涉及到多个属性:
 *  productOption
 *      |—— skuBase
 *          |—— properties[]        存放SKU属性
 *              |—— name
 *              |—— pid
 *              |—— type                   0 or 1 标识两个SKU属性的前后顺序
 *              |—— values[]
 *                  |—— image
 *                  |—— name
 *                  |—— vid
 *          |—— skus[]              两个SKU属性组合在一起
 *              |—— propPath (pid:vid:pid:vid)
 *              |—— skuId
 *  skuInfos
 *      |—— skuId
 *          |—— categoryId
 *          |—— image
 *          |—— stock
 *          |—— price
 *              |—— discount        折扣
 *              |—— originalPrice   原价
 *              |—— salePrice       促销价
 *
 * 不存在索引的概念,来赞达的SKU属性值都是单独存储的,由 id 区分
 *
 * @author cheng
 * @date 2019/3/28 19:10
 */
@Component
public class LazadaMetaDataMap implements MetaDataMap<String, LazadaMetaDataConvert.LazadaMetaData> {

    @Override
    public LazadaMetaDataConvert.LazadaMetaData map(String... t) {
        JSONObject data = JSON.parseObject(t[0]);
        JSONObject json = data.getJSONObject("product");

        LazadaMetaDataConvert.LazadaMetaData lazadaProduct = new LazadaMetaDataConvert.LazadaMetaData();
        lazadaProduct.setContent(t[1]);
        lazadaProduct.setTitle(json.getString("title"));
        lazadaProduct.setLink(json.getString("link"));
        lazadaProduct.setDesc(json.getString("desc"));
        lazadaProduct.setHighlights(getHighlights(json.getString("highlights")));
        final List<String> mianImages = getMianImages(data);
        final List<String> descImages = getDescImages(lazadaProduct.getContent());
        lazadaProduct.setImages(mianImages);
        lazadaProduct.getImages().addAll(descImages);
        lazadaProduct.setDescImages(descImages);
        lazadaProduct.setMainImages(mianImages);

        JSONObject skuBase = data.getJSONObject("productOption").getJSONObject("skuBase");
        lazadaProduct.setSkuAttributes(getSkuAttribute(skuBase));
        lazadaProduct.setSkus(getSkus(skuBase));
        lazadaProduct.setSkuInfos(getSkuInfos(data, lazadaProduct));
        lazadaProduct.setCategoryId(getCategoryId(t[0]));
        lazadaProduct.setProductId(getProductId2(t[0]));
        return lazadaProduct;
    }

    private List<String> getDescImages(String desc) {
        List<String> images = ReUtil.findAll("(https://([\\w+-]*)\\.slatic\\.net/\\w+?/\\w+?\\.(jpg|jpeg|png))", desc, 1);
        return Objects.nonNull(images)?images.stream()
                .distinct()
                .collect(toList()):new ArrayList<>();
    }

    private List<String> getMianImages(JSONObject data) {
        List<String> images =ReUtil.findAll("\"poster\":\"(.+?\\.(jpg|jpeg|png))\"", data.getString("skuGalleries"), 1).stream().map(url->{
            if (url.startsWith("//")){
                return "https:"+url;
            }else {
                return url;
            }
        }).distinct().collect(toList());

        return images.stream()
                .distinct()
                .collect(toList());
    }

    @Override
    public Long getCategoryId(String content) {
        Pattern CATEGORY_ID = Pattern.compile("\\\"categoryId\\\":\\\"([\\d]*)\\\"");
        final String s = ReUtil.get(CATEGORY_ID, content, 1);
        return StringUtils.isNotBlank(s)?Long.parseLong(s):0L;
    }

    private Long getProductId2( String content) {
        Pattern PRODUCT_ID = Pattern.compile("\\\"itemId\\\":\\\"([\\d]*)\\\"");
        return Long.parseLong(ReUtil.get(PRODUCT_ID, content, 1));
    }

    private static List<LazadaMetaDataConvert.LazadaMetaData.Sku> getSkus(JSONObject skuBase) {
        return JSON.parseArray(skuBase.getString("skus"), LazadaMetaDataConvert.LazadaMetaData.Sku.class);
    }

    private static List<LazadaMetaDataConvert.LazadaMetaData.SkuAttribute> getSkuAttribute(JSONObject skuBase) {
        return JSON.parseArray(skuBase.getString("properties"), LazadaMetaDataConvert.LazadaMetaData.SkuAttribute.class);
    }

    private static List<String> getHighlights(String highlights) {
        if(Objects.isNull(highlights)){
            return  null;
        }
        Document doc = Jsoup.parse(highlights);
        return doc.select("li").stream().map(Element::text).collect(toList());
    }

    private static List<String> getImages(JSONObject data, String desc) {
        List<String> images = ReUtil.findAll("src=\"https:(//my-live-01.slatic.net/original/.+?\\.jpg)", desc, 1);
        if (null == images) {
            images = new ArrayList<>();
        }
        images.addAll(ReUtil.findAll("poster\":\"(.+?\\.jpg)", data.getString("skuGalleries"), 1).stream().map(url->{
            if (url.startsWith("//")){
                return "https:"+url;
            }else {
                return url;
            }
        }).collect(toList()));

        return images.stream()
            .distinct()
            .collect(toList());
    }

    private static List<LazadaMetaDataConvert.LazadaMetaData.SkuInfo> getSkuInfos(JSONObject data, LazadaMetaDataConvert.LazadaMetaData lazadaProduct) {
        JSONObject skuInfos = data.getJSONObject("skuInfos");
        if (null == skuInfos || 0 == skuInfos.size()) {
            return null;
        }

        List<LazadaMetaDataConvert.LazadaMetaData.SkuAttribute> skuAttributes = lazadaProduct.getSkuAttributes();

        return skuInfos.getInnerMap().entrySet().stream().map(e -> {
                long skuId = Long.parseLong(e.getKey());
                if (0 == skuId) {
                    return null;
                }

                JSONObject jsonSkuInfo = JSON.parseObject(JSON.toJSONString(e.getValue()));

                LazadaMetaDataConvert.LazadaMetaData.SkuInfo skuInfo =new LazadaMetaDataConvert.LazadaMetaData.SkuInfo();
                skuInfo.setSkuId(skuId);
                skuInfo.setCategoryId(jsonSkuInfo.getLong("categoryId"));
                skuInfo.setCore(jsonSkuInfo.getObject("core", LazadaMetaDataConvert.LazadaMetaData.SkuInfo.Core.class));
                skuInfo.setImage(jsonSkuInfo.getString("image"));
                skuInfo.setStock(jsonSkuInfo.getInteger("stock"));
                skuInfo.setPrice(jsonSkuInfo.getObject("price", LazadaMetaDataConvert.LazadaMetaData.SkuInfo.Price.class));

                if (null == skuAttributes || skuAttributes.size() == 0) {
                    return skuInfo;
                }
                Optional<LazadaMetaDataConvert.LazadaMetaData.Sku> sku = lazadaProduct.getSkus().stream().filter(lp -> skuId == lp.getSkuId()).findFirst();
                if (!sku.isPresent()) {
                    return null;
                }
                String[] props = sku.get().getPropPath().split(";");

                if (props.length == 1) {
                    String[] oneIds = props[0].split(":");

                    Optional<LazadaMetaDataConvert.LazadaMetaData.SkuAttribute> oneExist = skuAttributes.stream().filter(sa -> sa.getPid().equals(oneIds[0])).findFirst();
                    if (!oneExist.isPresent()) {
                        return null;
                    }

                    skuInfo.setIndexs(Arrays.asList(
                        positionIndex(oneIds[1], oneExist.get())
                    ));
                } else {
                    String[] oneIds = props[0].split(":");
                    String[] twoIds = props[1].split(":");

                    Optional<LazadaMetaDataConvert.LazadaMetaData.SkuAttribute> oneExist = skuAttributes.stream().filter(sa -> sa.getPid().equals(oneIds[0])).findFirst();
                    if (!oneExist.isPresent()) {
                        return null;
                    }

                    Optional<LazadaMetaDataConvert.LazadaMetaData.SkuAttribute> twoExist = skuAttributes.stream().filter(sa -> sa.getPid().equals(twoIds[0])).findFirst();
                    if (!twoExist.isPresent()) {
                        return null;
                    }

                    skuInfo.setIndexs(Arrays.asList(
                        positionIndex(oneIds[1], oneExist.get()),
                        positionIndex(twoIds[1], twoExist.get())
                    ));
                }

                return skuInfo;
            })
            .filter(Objects::nonNull)
            .collect(toList());
    }

    private static Integer positionIndex(String pid, LazadaMetaDataConvert.LazadaMetaData.SkuAttribute skuAttribute) {
        JSONArray values = JSON.parseArray(skuAttribute.getValues());
        if (1 == values.size() && null != values.getJSONObject(0).getString("value")) {
            values = values.getJSONObject(0).getJSONArray("value");
        }

        List<String> options = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            options.add(values.getJSONObject(i).getString("name"));
        }
        skuAttribute.setOptions(options);

        List<String> images = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            images.add(values.getJSONObject(i).getString("image"));
        }
        skuAttribute.setImage(images);

        for (int i = 0; i < values.size(); i++) {
            if (pid.equals(values.getJSONObject(i).getString("vid"))) {
                return i;
            }
        }
        return null;
    }
}
