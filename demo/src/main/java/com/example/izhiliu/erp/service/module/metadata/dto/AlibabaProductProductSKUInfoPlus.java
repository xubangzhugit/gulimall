package com.izhiliu.erp.service.module.metadata.dto;


import com.alibaba.product.param.AlibabaProductProductSKUInfo;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Seriel
 * @create 2019-09-02 15:40
 **/
@Getter
@Setter
public class AlibabaProductProductSKUInfoPlus  extends AlibabaProductProductSKUInfo {

    private AlibabaProductSKUAttrInfoPlus[] attributes;

    private Double price;

    private Double originalPrice;

    private Integer soldQuantity;

    private int stock;


    private  int index = 0;


    public void  setIndexPlus(){
        for (AlibabaProductSKUAttrInfoPlus attribute : this.getAttributes()) {
            index = index + attribute.getIndex();
        }
    }
}
