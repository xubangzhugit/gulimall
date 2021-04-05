package com.izhiliu.erp.web.rest.item.param;

import com.izhiliu.erp.web.rest.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/22 18:58
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SaveOrPublishToShopParam extends BaseRequest {

    /**
     * 保存 or 发布到的店铺
     */
    @NotEmpty(message = "至少要有一个店铺ID")
    private List<Long> shopIds;

    /**
     * 源商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * true 发布 false 保存
     */
    private Boolean type = false;

    private boolean async = false;

    /**
     * 是否勾选删除
     * 0：勾选，1：未勾选
     */
    private int deleted;

    /**
     * 店铺折扣
     */
    private List<ShopDiscount> shopDiscounts;

    @Data
    public static class ShopDiscount {
        private Long shopId;
        private String discountId;
    }

}
