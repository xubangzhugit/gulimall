package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ShopeeSkuAttribute;
import com.izhiliu.erp.service.item.dto.ShopeeSkuAttributeDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity ShopeeSkuAttribute and its DTO ShopeeSkuAttributeDTO.
 */
public interface ShopeeSkuAttributeMapper extends EntityMapper<ShopeeSkuAttributeDTO, ShopeeSkuAttribute> {


    default ShopeeSkuAttribute fromId(Long id) {
        if (id == null) {
            return null;
        }
        ShopeeSkuAttribute shopeeSkuAttribute = new ShopeeSkuAttribute();
        shopeeSkuAttribute.setId(id);
        return shopeeSkuAttribute;
    }
}
