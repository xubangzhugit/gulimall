package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.UserImage;
import com.izhiliu.erp.service.item.dto.UserImageDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity ShopeeProductImage and its DTO ShopeeProductImageDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface UserImageMapper extends EntityMapper<UserImageDTO, UserImage> {



    default UserImage fromId(Long id) {
        if (id == null) {
            return null;
        }
        UserImage shopeeProductImage = new UserImage();
        shopeeProductImage.setId(id);
        return shopeeProductImage;
    }
}
