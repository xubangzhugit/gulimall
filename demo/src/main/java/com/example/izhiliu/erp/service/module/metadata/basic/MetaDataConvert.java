package com.izhiliu.erp.service.module.metadata.basic;

import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;

/**
 * describe: 源数据转换器
 * <p>
 *
 * @author cheng
 * @date 2019/1/9 16:41
 */
public interface MetaDataConvert<T> {

    ProductMetaDataDTO collect(T input);
}
