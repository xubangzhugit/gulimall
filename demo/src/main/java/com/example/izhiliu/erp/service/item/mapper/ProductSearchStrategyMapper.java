package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ProductSearchStrategy;
import com.izhiliu.erp.domain.item.ShopeeAttribute;
import com.izhiliu.erp.service.item.dto.ProductSearchStrategyDTO;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/19 14:11
 */
public interface ProductSearchStrategyMapper extends EntityMapper<ProductSearchStrategyDTO, ProductSearchStrategy> {

    default ShopeeAttribute fromId(Long id) {
        if (id == null) {
            return null;
        }
        ShopeeAttribute shopeeAttribute = new ShopeeAttribute();
        shopeeAttribute.setId(id);
        return shopeeAttribute;
    }
}
