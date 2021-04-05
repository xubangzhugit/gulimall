package com.izhiliu.erp.service.module.metadata.dto;

import com.alibaba.product.param.AlibabaProductSKUAttrInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 淘宝元数据sku  的 dto  帮助类
 *
 * @author Seriel
 * @create 2019-08-19 11:07
 **/
@Getter
@Setter
public class AlibabaProductSKUAttrInfoPlus extends AlibabaProductSKUAttrInfo {
    private Integer index;
    
}
