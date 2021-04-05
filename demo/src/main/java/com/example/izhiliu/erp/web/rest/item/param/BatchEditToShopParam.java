package com.izhiliu.erp.web.rest.item.param;

import com.izhiliu.erp.service.item.dto.ShopeeProductAttributeValueDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.web.rest.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * describe: 批量保存到店铺参数
 * <p>
 *
 * @author cheng
 * @date 2019/2/13 13:55
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatchEditToShopParam extends BaseRequest {

    @NotEmpty(message = "至少要有一个商品ID")
    private List<Long> productIds;

    @NotEmpty(message = "至少要有一个店铺ID")
    private List<Long> shopIds;

    @NotNull(message = "类目ID不能为空")
    private Long categoryId;

    private List<ShopeeProductAttributeValueDTO> attributeValues;

    @NotEmpty(message = "至少要有一个物流渠道")
    private List<ShopeeProductDTO.Logistic> logistics;
}
