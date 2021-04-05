package com.izhiliu.erp.service.item.mapper.list;

import com.izhiliu.erp.domain.item.ProductMetaData;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM;
import com.izhiliu.core.domain.common.EntityMapper;

/**
 * describe: 产品源数据 转换成 商品列表项
 * <p>
 *
 * @author cheng
 * @date 2019/1/9 13:23
 */
public interface ProductMetaDataListMapper extends EntityMapper<ProductListVM, ProductMetaData> {
}
