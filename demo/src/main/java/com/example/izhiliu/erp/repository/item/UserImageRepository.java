package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.item.UserImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * Spring Data  repository for the ShopeeProductImage entity.
 */
@Mapper
public interface UserImageRepository extends BaseMapper<UserImage> {

    IPage<UserImage> pageByLoginId(Page page, @Param("productId") Long productId);

    Long selectImageSizeByUserId(@Param("currentLogin") String currentLogin);
}
