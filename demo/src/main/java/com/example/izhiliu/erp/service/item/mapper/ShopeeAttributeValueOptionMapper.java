package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ShopeeAttributeValueOption;
import com.izhiliu.erp.service.item.dto.ShopeeAttributeValueOptionDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity ShopeeAttributeValueOption and its DTO ShopeeAttributeValueOptionDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface ShopeeAttributeValueOptionMapper extends EntityMapper<ShopeeAttributeValueOptionDTO, ShopeeAttributeValueOption> {



    default ShopeeAttributeValueOption fromId(Long id) {
        if (id == null) {
            return null;
        }
        ShopeeAttributeValueOption shopeeAttributeValueOption = new ShopeeAttributeValueOption();
        shopeeAttributeValueOption.setId(id);
        return shopeeAttributeValueOption;
    }
}
