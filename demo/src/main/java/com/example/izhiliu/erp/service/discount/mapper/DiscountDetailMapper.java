package com.izhiliu.erp.service.discount.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.discount.ShopeeDiscountDetail;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountDetailDTO;
import org.mapstruct.Mapper;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:20
 */
@Mapper(componentModel = "spring", uses = {})
public interface DiscountDetailMapper extends EntityMapper<ShopeeDiscountDetailDTO, ShopeeDiscountDetail> {

}
