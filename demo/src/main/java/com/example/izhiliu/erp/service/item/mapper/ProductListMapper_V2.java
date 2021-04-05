package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM_V2;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM_V21;

import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/19 15:19
 */
public interface ProductListMapper_V2 extends EntityMapper<ProductListVM_V2, ShopeeProduct> {
    List<ProductListVM_V2> toProductListV2(List<ShopeeProduct> entityList);

    ProductListVM_V2 toProductListV2(ShopeeProduct entity);

    List<ProductListVM_V21> toProductListV3(List<ShopeeProduct> records);
}
