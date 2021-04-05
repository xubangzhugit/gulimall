package com.izhiliu.erp.service.image;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.izhiliu.erp.domain.image.ImageBankAddress;
import com.izhiliu.erp.service.image.dto.ImageBankAddressDto;
import com.izhiliu.erp.service.image.dto.ImageBankCapacityCacheObejct;
import com.izhiliu.erp.web.rest.image.param.ImageBankAddressCondition;
import com.izhiliu.erp.web.rest.image.param.MovingPictureCondition;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ImageBankAddressService  extends CustomizeBaseService<ImageBankAddress, ImageBankAddressDto> {
    ImageBankAddressDto insert(ImageBankAddressDto aDto);

    IPage<ImageBankAddressDto> selectByLoginid(ImageBankAddressCondition imageBankAddressCondition);

    void movingAndManagementPicture(MovingPictureCondition movingPicture);

    ImageBankCapacityCacheObejct selectImageSizeByUserId();

    Long selectImageSizeByUserId(String s);

    void remove(List<Long> deleteId);
}
