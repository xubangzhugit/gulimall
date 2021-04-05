package com.izhiliu.erp.repository.image;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.izhiliu.erp.domain.image.ImageBankDirectory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Seriel
 * @create 2019-08-27 10:04
 **/
@Mapper
public interface ImageBankDirectoryRepository extends BaseMapper<ImageBankDirectory> {
    List<ImageBankDirectory> selectByLoginid(@Param("currentLogin") String currentLogin, @Param("parentId") Long parentId);
}
