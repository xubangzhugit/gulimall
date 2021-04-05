package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.Platform;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.web.rest.item.vm.BatchEditProductVM;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/13 20:10
 */
public interface BatchEditProductMapperOld extends EntityMapper<BatchEditProductVM, ShopeeProduct> {

    default Platform fromId(Long id) {
        if (id == null) {
            return null;
        }
        Platform platform = new Platform();
        platform.setId(id);
        return platform;
    }
}
