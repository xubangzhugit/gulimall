package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ShopeeCategory;
import com.izhiliu.erp.service.item.dto.ShopeeCategoryDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity ShopeeCategory and its DTO ShopeeCategoryDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface ShopeeCategoryMapper extends EntityMapper<ShopeeCategoryDTO, ShopeeCategory> {



    default ShopeeCategory fromId(Long id) {
        if (id == null) {
            return null;
        }
        ShopeeCategory shopeeCategory = new ShopeeCategory();
        shopeeCategory.setId(id);
        return shopeeCategory;
    }
}
