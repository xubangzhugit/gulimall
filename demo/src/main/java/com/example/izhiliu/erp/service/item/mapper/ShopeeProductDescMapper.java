package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ShopeeProductDesc;
import com.izhiliu.erp.domain.item.ShopeeSkuAttribute;
import com.izhiliu.erp.service.item.dto.ShopeeProductDescDTO;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity ShopeeProductDesc and its DTO ShopeeProductDescDTO.
 */
@Mapper(componentModel = "spring", uses = {})

public interface ShopeeProductDescMapper extends EntityMapper<ShopeeProductDescDTO, ShopeeProductDesc> {

}
