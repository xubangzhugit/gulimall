package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ShopeeProductSku;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity ShopeeProductSku and its DTO ShopeeProductSkuDTO.
 */
public interface ShopeeProductSkuMapper extends EntityMapper<ShopeeProductSkuDTO, ShopeeProductSku> {



    default ShopeeProductSku fromId(Long id) {
        if (id == null) {
            return null;
        }
        ShopeeProductSku shopeeProductSku = new ShopeeProductSku();
        shopeeProductSku.setId(id);
        return shopeeProductSku;
    }
}
