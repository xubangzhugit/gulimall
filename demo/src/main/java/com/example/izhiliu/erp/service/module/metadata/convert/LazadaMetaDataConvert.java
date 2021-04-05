package com.izhiliu.erp.service.module.metadata.convert;

import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.core.common.constant.PlatformNodeEnum;
import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataConvert;
import lombok.Data;
import lombok.experimental.Accessors;
import net.logstash.logback.encoder.org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/3/29 19:27
 */
@Component
public class LazadaMetaDataConvert implements MetaDataConvert<LazadaMetaDataConvert.LazadaMetaData> {

    @Override
    public ProductMetaDataDTO collect(LazadaMetaData lazadaProduct) {
        ProductMetaDataDTO product = new ProductMetaDataDTO();
        product.setLoginId(lazadaProduct.getLoginId());
        product.setPlatform(PlatformEnum.LAZADA.getName());
        product.setPlatformId(PlatformEnum.LAZADA.getCode().longValue());

        PlatformNodeEnum node = PlatformNodeEnum.match(product.getPlatformId(), lazadaProduct.getLink());
        product.setPlatformNodeId(node.id);
        product.setCurrency(node.currency);
        product.setCollectTime(Instant.now());
        product.setDescImages(lazadaProduct.getDescImages());
        product.setMainImages(lazadaProduct.getMainImages());
        product.setUrl(lazadaProduct.getLink());
        product.setName(lazadaProduct.getTitle());
        product.setImages(lazadaProduct.getImages());
        product.setJson(JSON.toJSONString(lazadaProduct));
        product.setDescription(getDescription(lazadaProduct));
        product.setCategoryId(lazadaProduct.getCategoryId());
        product.setProductId(lazadaProduct.getProductId());
        return product;
    }

    public String getDescription(LazadaMetaData lazadaProduct) {
        if (StringUtils.isNotBlank(lazadaProduct.getContent())) {
            try {
                JSONObject components = JSON.parseObject(lazadaProduct.getContent())
                    .getJSONObject("result")
                    .getJSONObject("components");

                Map<String, Object> innerMap = components.getInnerMap();

                List<Map.Entry<String, Object>> maps = innerMap.entrySet().stream()
                    .filter(e -> ReUtil.isMatch("[0-9]+", e.getKey()))
                    .collect(Collectors.toList());

                /*
                 * 获取到所有的 文本数据
                 */
                List<String> texts = maps.stream()
                    .map(e -> JSON.parseObject(JSON.toJSONString(e.getValue())).getJSONObject("formData"))
                    .filter(e -> null != e.get("schema"))
                    .map(e -> e.getJSONObject("schema").getJSONArray("children"))
                    .flatMap(Collection::stream)
                    .filter(e -> "text".equals(JSON.parseObject(JSON.toJSONString(e)).getString("type")))
                    .map(e -> JSON.parseObject(JSON.toJSONString(e)).getString("text"))
                    .collect(Collectors.toList());

                /*
                 * html 数据追加在尾部
                 */
                texts.addAll(maps.stream()
                    .map(e -> JSON.parseObject(JSON.toJSONString(e.getValue())).getJSONObject("formData"))
                    .filter(e -> null == e.get("schema") && null != e.get("html"))
                    .map(e -> toPlainText(e.getString("html")))
                    .collect(Collectors.toList()));

                lazadaProduct.setContent(String.join("\r\n\r\n", texts));
            } catch (Exception e) {
                lazadaProduct.setContent(null);
            }
        }

        StringBuilder desc = new StringBuilder();
        if (null != lazadaProduct.getHighlights() && lazadaProduct.getHighlights().size() > 0) {
            desc.append(String.join("\r\n\r\n", lazadaProduct.getHighlights()) + "\r\n\r\n\r\n");
        }
        if (StringUtils.isNotBlank(lazadaProduct.getContent())) {
            desc.append(lazadaProduct.getContent() + "\r\n\r\n\r\n");
        }
        if (StringUtils.isNotBlank(lazadaProduct.getDesc())) {
            desc.append(toPlainText(lazadaProduct.getDesc()));
        }
        return desc.toString();
    }

    public static String toPlainText(final String html) {
        if (html == null) {
            return "";
        }

        final Document document = Jsoup.parse(html);
        final Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
        document.outputSettings(outputSettings);
        document.select("br").append("\\n");
        document.select("p").prepend("\\n");
        document.select("p").append("\\n");
        final String newHtml = document.html().replaceAll("\\\\n", "\n");
        final String plainText = Jsoup.clean(newHtml, "", Whitelist.none(), outputSettings);
        return StringEscapeUtils.unescapeHtml(plainText.trim())
            .replaceAll("\t", "")
            .replaceAll("(\r?\n(\\s*\r?\n)+)", "\r\n");
    }

    @Data
    @Accessors(chain = true)
    public static class LazadaMetaData {
        /**
         * 商品主图
         */
        private List<String> mainImages;

        /**
         * 详情图
         */
        private List<String> descImages;

        private String title;
        private String link;
        private String desc;
        private String content;
        private Long categoryId;
        private Long productId;

        private List<String> images;
        private List<SkuAttribute> skuAttributes;
        private List<SkuInfo> skuInfos;
        private List<Sku> skus;
        private String json;
        private String loginId;
        private List<String> highlights;

        @Data
        public static class Sku {
            private String itemId;
            private String sellerId;
            private String innerSkuId;
            private String requestParams;
            private String pageId;
            private String pagePath;
            private String propPath;
            private String cartSkuId;
            private Long skuId;
            private String cartItemId;
        }

        @Data
        public static class SkuAttribute {
            private String name;
            private boolean needLabel;
            private String pid;
            private List<String> image;
            private String type;
            private String values;
            private List<String> options;
        }

        @Data
        public static class SkuInfo {
            private Long skuId;
            private Long categoryId;
            private String image;
            private Integer stock;
            private Price price;
            private Core core;
            private List<Integer> indexs;

            @Data
            public static class Core {
                private String country;
                private String currencyCode;
                private String language;
                private String layoutType;
            }

            @Data
            public static class Price {
                private String discount;
                private PriceInfo originalPrice;
                private PriceInfo salePrice;

                @Data
                public static class PriceInfo {
                    private String text;
                    private Double value;
                }
            }
        }
    }
}
