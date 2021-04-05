package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.Platform;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.dto.BatchEditProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/9 15:36
 */
public interface BatchEditProductMapper extends EntityMapper<BatchEditProductDTO, ShopeeProduct> {

    default Platform fromId(Long id) {
        if (id == null) {
            return null;
        }
        Platform platform = new Platform();
        platform.setId(id);
        return platform;
    }

    default List<ShopeeProductDTO> toShopeeProductDTO(List<BatchEditProductDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        final List<ShopeeProductDTO> list = new ArrayList<>(dtoList.size());
        list.addAll(dtoList);
        return list;
    }
}
