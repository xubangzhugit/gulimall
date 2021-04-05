package com.izhiliu.erp.service.module.metadata.dto;

import com.alibaba.product.param.AlibabaProductSKUAttrInfo;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Element;

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
public class AlibabaSkuMateDataMapDto {


    private AtomicBoolean existImg = new AtomicBoolean(false);

    private MetaDataObject.CollectController collectController = MetaDataObject.COLLECT_CONTROLLER;

    private String imageIndexName;

    private Map<String,AlibabaProductSKUAttrInfoPlus> skuAttrInfos = new HashMap<>();

    public final boolean get() {
        return existImg.get();
    }

    public final void set(boolean existImg) {
        this.existImg.set(existImg);
    }


    private  int  cursor = 0;


    public Integer getNextCursor() {
        cursor = cursor + 1;
        return cursor;
    }
}
