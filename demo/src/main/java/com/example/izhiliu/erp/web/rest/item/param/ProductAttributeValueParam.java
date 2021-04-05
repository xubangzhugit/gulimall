package com.izhiliu.erp.web.rest.item.param;

import com.izhiliu.erp.service.item.dto.ShopeeProductAttributeValueDTO;
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
 * @date 2019/1/14 9:36
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductAttributeValueParam extends BaseRequest {

    private List<ShopeeProductAttributeValueDTO> attributeValues;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    private Boolean shopee = false;
}
