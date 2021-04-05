package com.izhiliu.erp.service.discount.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.discount.ShopeeDiscountItem;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemDTO;
import org.mapstruct.Mapper;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:20
 */
@Mapper(componentModel = "spring", uses = {})
public interface DiscountItemMapper extends EntityMapper<ShopeeDiscountItemDTO, ShopeeDiscountItem> {


}
