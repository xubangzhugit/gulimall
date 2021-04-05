package com.izhiliu.erp.service.module.metadata.dto;

import com.alibaba.product.param.AlibabaProductProductInfo;
import lombok.Data;

import java.util.List;


/**
 * @author Seriel
 * @create 2019-09-02 15:47
 **/
@Data
public class AlibabaProductProductInfoPlus  extends  AlibabaProductProductInfo{

    private AlibabaProductProductSKUInfoPlus[] skuInfos;
    private PriceRange[] priceRange;
    private PriceRange[] priceRangeOriginal;
    private int stock;
    private List<String> descImages;
}
