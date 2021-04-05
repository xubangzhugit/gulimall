package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ProductMetaData;

import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity ProductMetaData and its DTO ProductMetaDataDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface ProductMetaDataMapper extends EntityMapper<ProductMetaDataDTO, ProductMetaData> {



    default ProductMetaData fromId(String id) {
        if (id == null) {
            return null;
        }
        ProductMetaData productMetaData = new ProductMetaData();
        productMetaData.setId(id);
        return productMetaData;
    }
}
