package com.izhiliu.erp.repository.image;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.image.ImageBankAddress;
import com.izhiliu.erp.service.image.dto.ImageBankAddressDto;
import com.izhiliu.erp.web.rest.image.param.ImageBankAddressCondition;
import com.izhiliu.erp.web.rest.image.param.MovingPictureCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Seriel
 * @create 2019-08-27 10:04
 **/
@Mapper
public interface ImageBankAddressRepository extends BaseMapper<ImageBankAddress> {

    IPage<ImageBankAddress> selectByLoginid(@Param("page") Page<ImageBankAddress> page, @Param("condition") ImageBankAddressCondition condition, @Param("currentLogin") String currentLogin);

    int movingImage(@Param("movingPicture") MovingPictureCondition movingPicture, @Param("currentLogin") String currentLogin);

    Long selectImageSizeByUserId(@Param("currentLogin") String currentLogin);

}
