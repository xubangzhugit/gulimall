package com.izhiliu.erp.web.rest.item.param;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/7/14 16:20
 */
@Data
public class PushToShopeeTaskQO extends BaseTaskQO {
    public static final String ITEM_INFO = "item_info";
    public static final String ITEM_IMAGE = "item_image";
    public static final String ITEM_PRICE = "item_price";
    public static final String ITEM_STOCK = "item_stock";
    public static final String ITEM_LOGISTICS = "item_logistics";
    public static final String ITEM_ALL = "item_all";

    private String login;

    private List<ShopProductParam> params;
    /**
     * 推送类型
     * item_image：商品图片
     * item_info：基本信息
     * item_price：价格
     * item_stock：库存
     * item_logistics：物流信息
     * item_all： 全部更新
     */
    @NotEmpty
    private List<String> pushType;

}
