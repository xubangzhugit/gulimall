package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ShopeeProductAttributeValue;
import com.izhiliu.erp.service.item.dto.ShopeeProductAttributeValueDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity ShopeeProductAttributeValue and its DTO ShopeeProductAttributeValueDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface ShopeeProductAttributeValueMapper extends EntityMapper<ShopeeProductAttributeValueDTO, ShopeeProductAttributeValue> {



    default ShopeeProductAttributeValue fromId(Long id) {
        if (id == null) {
            return null;
        }
        ShopeeProductAttributeValue shopeeProductAttributeValue = new ShopeeProductAttributeValue();
        shopeeProductAttributeValue.setId(id);
        return shopeeProductAttributeValue;
    }
}
