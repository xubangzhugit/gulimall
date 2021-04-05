package com.izhiliu.erp.service.item.mapper;


import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ShopeeProductExtraInfo;
import com.izhiliu.erp.service.item.dto.ShopeeProductExtraInfoDto;
import org.mapstruct.Mapper;


/**
 * Mapper for the entity ShopeeProductMedia and its DTO ShopeeProductMediaDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface ShopeeProductExtraInfoMapper extends EntityMapper<ShopeeProductExtraInfoDto, ShopeeProductExtraInfo> {


    default Float mapFloat(Integer images) {
        if (null == images) {
            return null;
        }
        return images.floatValue() / 10;
    }

    default Integer mapFloat(Float images) {
        if (null == images) {
            return null;
        }
        return Float.valueOf(images * 10).intValue();

    }


}
