package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ItemCategoryMap;
import com.izhiliu.erp.service.item.dto.ItemCategoryMapDTO;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity Platform and its DTO PlatformDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface CategoryMapMapper extends EntityMapper<ItemCategoryMapDTO, ItemCategoryMap> {



    default ItemCategoryMap fromId(Long id) {
        if (id == null) {
            return null;
        }
        ItemCategoryMap platform = new ItemCategoryMap();
        platform.setId(id);
        return platform;
    }
}
