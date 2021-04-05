package com.izhiliu.erp.service.item.module.convert;

import cn.hutool.core.util.StrUtil;
import com.alibaba.product.param.AlibabaProductSKUAttrInfo;
import com.izhiliu.erp.service.module.metadata.convert.TaobaoMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.convert.TianmaoMetaDataConvert;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductSKUAttrInfoPlus;
import org.springframework.stereotype.Component;

/**
 * describe: 1688 源数据转
 * <p>
 *
 * @author cheng
 * @date 2019/1/25 16:51
 */
@Component
public class TianmaoMetaConvertShopee extends AbstractAliMetaConvertShopee {


    @Override
    public String getImage(AlibabaProductSKUAttrInfoPlus attribute) {
        if (StrUtil.isBlank(attribute.getSkuImageUrl())) {
            return null;
        }
        return TianmaoMetaDataConvert.getImage(attribute.getSkuImageUrl());
    }


}
