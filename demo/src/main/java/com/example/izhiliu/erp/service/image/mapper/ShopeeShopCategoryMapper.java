package com.izhiliu.erp.service.image.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ProductMetaData;
import com.izhiliu.erp.domain.item.ShopeeShopCategory;
import com.izhiliu.erp.service.image.dto.ShopeeShopCategoryDTO;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity ProductMetaData and its DTO ProductMetaDataDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface ShopeeShopCategoryMapper extends EntityMapper<ShopeeShopCategoryDTO,ShopeeShopCategory> {



    default ProductMetaData fromId(String id) {
        if (id == null) {
            return null;
        }
        ProductMetaData productMetaData = new ProductMetaData();
        productMetaData.setId(id);
        return productMetaData;
    }
}
