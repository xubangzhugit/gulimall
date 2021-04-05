package com.izhiliu.erp.web.rest.item.vm;

import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import lombok.Data;

/**
 * describe: 批量编辑所需的商品模型
 * <p>
 *
 * @author cheng
 * @date 2019/2/13 13:46
 */
@Data
public class BatchEditProductVM extends ShopeeProductDTO {

    private VariationVM variationWrapper;
}
