package com.izhiliu.erp.service.module.metadata.map;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataMap;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductSKUInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductSKUAttrInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaSkuMateDataMapDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * describe: 1688前端采集 ==> 源数据(转成一键铺货的数据结构)
 * <p>
 *
 * @author cheng
 * @date 2019/2/27 15:27
 */
@Component
@Slf4j
public abstract class AbstractAlibabaProductMetaDateMap implements MetaDataMap<MetaDataObject, AlibabaProductProductInfoPlus> {


    protected final String squeeze(String html) {
        return html.replaceAll("\\s*|\t|\r|\n", "");
    }

    /**
     * 获取变体JSON字符串
     */
    protected  List<AlibabaProductProductSKUInfoPlus> getSkuInfos(String content, Document document, Double defaultPrice){
        return  null;
    };

    @Override
    @Deprecated
    public AlibabaProductProductInfoPlus map(MetaDataObject[] t) {
          throw new RuntimeException(" Deprecated");
    }


    public abstract AlibabaProductProductInfoPlus map(MetaDataObject t);

    /**
     *
     * @param defaultPrice
     * @param valItemInfo
     * @param alibabaSkuMateDataMapDto
     * @return
     */
    protected abstract List<AlibabaProductProductSKUInfoPlus> getAlibabaProductSKUInfos(Double defaultPrice, JSONObject valItemInfo, AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto);


    protected void fillAlibabaProductSKUAttrInfos(AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto, AlibabaProductProductSKUInfoPlus skuProp) {
        int index = 0;
        final AlibabaProductSKUAttrInfoPlus[] alibabaProductSKUAttrInfos;
        //  如果没有提前填充好 sku 的属性就 开始填充 如果填充过了的 跳过
        if(Objects.isNull(skuProp.getAttributes())){
            final String specId = skuProp.getSpecId();
            final Map<String, AlibabaProductSKUAttrInfoPlus> skuAttrInfos = alibabaSkuMateDataMapDto.getSkuAttrInfos();
            String[] split = specId.split(";");
            split = clean(split);
            alibabaProductSKUAttrInfos = new AlibabaProductSKUAttrInfoPlus[split.length];
            for (String value : split) {
                if (StringUtils.isNotBlank(value)) {
                    final AlibabaProductSKUAttrInfoPlus alibabaProductSKUAttrInfo = skuAttrInfos.get(value);
                    if (Objects.nonNull(alibabaProductSKUAttrInfo)) {
                        alibabaProductSKUAttrInfos[index] = alibabaProductSKUAttrInfo;
                        ++index;
                    }
                }
            }
        }else{
            alibabaProductSKUAttrInfos =  skuProp.getAttributes();
            index = alibabaProductSKUAttrInfos.length;
        }

        if(alibabaSkuMateDataMapDto.get()){
            sortByExistImage(alibabaSkuMateDataMapDto.getImageIndexName(), alibabaProductSKUAttrInfos);
        }
        //  判断是否是  超多规格  就是  2个以上  就合并
        if (index > 2) {
            mergeExtraSkuAttrInfo(skuProp, alibabaProductSKUAttrInfos);
        } else {
            skuProp.setAttributes(alibabaProductSKUAttrInfos);
        }
        skuProp.setIndexPlus();
    }

    /**
     *    合并多余的sku 属性信息
     * @param skuProp
     * @param alibabaProductSKUAttrInfos
     */
    protected void mergeExtraSkuAttrInfo(AlibabaProductProductSKUInfoPlus skuProp, AlibabaProductSKUAttrInfoPlus[] alibabaProductSKUAttrInfos) {
        AlibabaProductSKUAttrInfoPlus[] productSKUAttrInfo = new AlibabaProductSKUAttrInfoPlus[2];
        AlibabaProductSKUAttrInfoPlus alibabaProductSKUAttrInfo = Arrays.asList(alibabaProductSKUAttrInfos).stream().filter(Objects::nonNull).skip(1).reduce((o1, o2) -> {
            if (!o1.getAttributeDisplayName().contains(o2.getAttributeDisplayName())) {
                o1.setAttributeDisplayName(o1.getAttributeDisplayName() + "+" + o2.getAttributeDisplayName());
            }
            if (!o1.getAttributeName().contains(o2.getAttributeName())) {
                o1.setAttributeName(o1.getAttributeName() + "+" + o2.getAttributeName());
            }
            if (!o1.getAttributeValue().contains(o2.getAttributeValue())) {
                o1.setAttributeValue(o1.getAttributeValue() + "+" + o2.getAttributeValue());
            }
            return o1;
        }).get();

        if (Objects.nonNull(alibabaProductSKUAttrInfo)) {
            productSKUAttrInfo[1] = alibabaProductSKUAttrInfo;
        }
        productSKUAttrInfo[1] = alibabaProductSKUAttrInfo;
        productSKUAttrInfo[0] = alibabaProductSKUAttrInfos[0];
        skuProp.setAttributes(productSKUAttrInfo);
    }

    /**
     *    根據第一個排序
     * @param alibabaProductSKUAttrInfos
     */
    protected void sortByExistImage(String imageIndexName, AlibabaProductSKUAttrInfoPlus[] alibabaProductSKUAttrInfos) {
           if(alibabaProductSKUAttrInfos.length == 0 ){
               return;
           }
            ///   Attributes 排序
            final AlibabaProductSKUAttrInfoPlus attribute = alibabaProductSKUAttrInfos[0];
           if(StringUtils.isNotBlank(attribute.getSkuImageUrl())){return;}
            //   如果第一个的图片不是为空的化 就转换位置
            for (int i = 1; i < alibabaProductSKUAttrInfos.length; i++) {
                    if(Objects.equals(alibabaProductSKUAttrInfos[i].getAttributeDisplayName(),imageIndexName)){
                    alibabaProductSKUAttrInfos[0] = alibabaProductSKUAttrInfos[i];
                    alibabaProductSKUAttrInfos[i] = attribute;
                    return;
                }
            }
    }

    /**
     *    根据解析的json  初始化一个产品SkuInfo
     * @param defaultPrice
     * @param sku
     * @return
     */
    protected  AlibabaProductProductSKUInfoPlus initAlibabaProductProductSKUInfo(Double defaultPrice, Map.Entry<String, Object> sku) {
        return  null;
    };

    /**
     *   抓取sku 属性信息
     * @param alibabaSkuMateDataMapDto
     */
    protected  void grabSkuAttrInfo(AlibabaSkuMateDataMapDto alibabaSkuMateDataMapDto, Element element){
        return;
    };

    /**
     *   清空 非(空的数据)条件的 数据
     * @param v
     * @return
     */
    public final static String[] clean(final String[] v) {
        int r, w;
        final int n = r = w = v.length;
        while (r > 0) {
            final String s = v[--r];
            if (StringUtils.isNotBlank(s)) {
                v[--w] = s;
            }
        }
        return Arrays.copyOfRange(v, w, n);
    }


   public  AlibabaProductProductSKUInfoPlus[] discountPriceAndStockHandler(List<AlibabaProductProductSKUInfoPlus> skuProps,JSONObject discount, MetaDataObject.CollectController collectController){
       if(Objects.isNull(skuProps)){
           return  new AlibabaProductProductSKUInfoPlus[]{};
       }
       //  处理折扣价格 以及库存
       if(Objects.nonNull(discount)){
           try {
               fillDiscountPriceAndStock(skuProps,discount,collectController);
           } catch (Exception e) {
               log.error(e.getMessage(),e);
               log.error("解析折扣价 或者  库存出现错误");
           }
       }
       return skuProps.toArray(new AlibabaProductProductSKUInfoPlus[skuProps.size()]);
   }

    public abstract void fillDiscountPriceAndStock(AlibabaProductProductInfoPlus skuProps, JSONObject originalDiscount, MetaDataObject.CollectController collectController);
    public abstract void fillDiscountPriceAndStock(List<AlibabaProductProductSKUInfoPlus> skuProps, JSONObject originalDiscount, MetaDataObject.CollectController collectController);
}
