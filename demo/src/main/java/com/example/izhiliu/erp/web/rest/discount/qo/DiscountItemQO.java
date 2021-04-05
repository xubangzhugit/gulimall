package com.izhiliu.erp.web.rest.discount.qo;

import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemDTO;
import com.izhiliu.erp.web.rest.item.param.BaseItemQO;
import com.izhiliu.open.shopee.open.sdk.api.discount.param.DiscountVariation;
import com.izhiliu.open.shopee.open.sdk.api.discount.result.GetDiscountDetailResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Locale;

/**
 * @Author: louis
 * @Date: 2020/8/4 15:20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class DiscountItemQO extends BaseItemQO {
    public static final String TYPE_ADD = "add";
    public static final String TYPE_DELETE = "delete";
    public static final String TYPE_MODIFY = "modify";

    private String discountId;
    private List<Item> items;
    private List<GetDiscountDetailResult.Item> syncItems;
    private Boolean needToClean;
    private List<ShopeeDiscountItemDTO> originItems;
    private Boolean needToSyncItem;
    private String taskId;
    private Locale locale;


    @Data
    public static class Item {
        private Long shopeeItemId;
        private long variationId;
        private Long shopId;
        private float itemPromotionPrice;
        private int purchaseLimit;
        private List<DiscountVariation> variations;
        private String type;
    }
}
