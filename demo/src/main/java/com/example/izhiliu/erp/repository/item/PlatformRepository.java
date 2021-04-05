package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.izhiliu.erp.domain.item.Platform;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * Spring Data  repository for the Platform entity.
 */
@Mapper
public interface PlatformRepository extends BaseMapper<Platform> {

    Platform selectByName(@Param("name") String name);
}
