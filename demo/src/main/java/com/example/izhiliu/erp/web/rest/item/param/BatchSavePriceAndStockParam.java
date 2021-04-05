package com.izhiliu.erp.web.rest.item.param;

import com.izhiliu.core.common.ValidList;
import com.izhiliu.erp.web.rest.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/3/5 9:34
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatchSavePriceAndStockParam extends BaseRequest {
    private static final long serialVersionUID = -3524973634502102673L;

    @Valid
    private ValidList<Item> items;
    @Valid
    private ValidList<Variation> variations;

    @Data
    public static class Item implements Serializable {
        private static final long serialVersionUID = -433088055424902171L;
        @NotNull(message = "店铺ID不能为空")
        private Long shopId;
        @NotNull(message = "商品ID不能为空")
        private Long itemId;
        private Integer stock;
        private Float price;
    }

    @Data
    public static class Variation implements Serializable {
        private static final long serialVersionUID = 4398771164780128951L;
        @NotNull(message = "变体ID不能为空")
        private Long id;
        @NotNull(message = "店铺ID不能为空")
        private Long shopId;
        @NotNull(message = "商品ID不能为空")
        private Long itemId;
        @NotNull(message = "变异ID不能为空")
        private Long variationId;
        private Float price;
        private Integer stock;
    }
}
