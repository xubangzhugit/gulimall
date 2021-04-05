package com.izhiliu.erp.service.item.dto;

import com.izhiliu.erp.web.rest.item.vm.VariationVM;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/9 11:20
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatchEditProductDTO extends ShopeeProductDTO {
    private static final long serialVersionUID = 5954602683076699324L;

    private VariationVM variationWrapper;
    private List<ShopeeProductAttributeValueDTO> attributeValues;
}
