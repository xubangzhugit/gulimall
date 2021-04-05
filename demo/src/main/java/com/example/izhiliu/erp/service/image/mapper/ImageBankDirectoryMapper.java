package com.izhiliu.erp.service.image.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.image.ImageBankAddress;
import com.izhiliu.erp.domain.image.ImageBankDirectory;
import com.izhiliu.erp.domain.item.ProductMetaData;
import com.izhiliu.erp.service.image.dto.ImageBankAddressDto;
import com.izhiliu.erp.service.image.dto.ImageBankDirectoryDto;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity ProductMetaData and its DTO ProductMetaDataDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface ImageBankDirectoryMapper extends EntityMapper<ImageBankDirectoryDto, ImageBankDirectory> {



    default ProductMetaData fromId(String id) {
        if (id == null) {
            return null;
        }
        ProductMetaData productMetaData = new ProductMetaData();
        productMetaData.setId(id);
        return productMetaData;
    }
}
