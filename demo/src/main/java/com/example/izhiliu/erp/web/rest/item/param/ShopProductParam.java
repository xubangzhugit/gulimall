package com.izhiliu.erp.web.rest.item.param;

import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.web.rest.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Locale;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/15 13:35
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShopProductParam extends BaseRequest {

    @NotNull(groups = {ProductAndShop.class}, message = "商品ID不能为空")
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(groups = {ItemAndShop.class}, message = "虾皮商品ID不能为空")
    @NotNull(message = "商品ID不能为空")
    private Long shopeeItemId;

    @NotNull(groups = {ProductAndShop.class, ItemAndShop.class}, message = "店铺ID不能为空")
    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    public interface ProductAndShop {
    }

    public interface ItemAndShop {
    }

    private ShopeeProductDTO product;

    private String taskId;

    private Locale locale;

}
