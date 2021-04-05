package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;

import java.util.List;

/**
 * Mapper for the entity ShopeeProduct and its DTO ShopeeProductDTO.
 */
public interface ShopeeProductMapper extends EntityMapper<ShopeeProductDTO, ShopeeProduct> {


    float ZERO = -1F;

    default ShopeeProduct fromId(Long id) {
        if (id == null) {
            return null;
        }
        ShopeeProduct shopeeProduct = new ShopeeProduct();
        shopeeProduct.setId(id);
        return shopeeProduct;
    }

    List<ShopeeProductDTO> toUrlDTO(List<ShopeeProduct> entityList);

    ShopeeProduct toEntityV2(ShopeeProductDTO dto);

    ShopeeProductDTO toDtoV2(ShopeeProduct entity);

    List<ShopeeProduct> toEntityV2(List<ShopeeProductDTO> dtoList);

    List<ShopeeProductDTO> toDtoV2(List<ShopeeProduct> entityList);

}
