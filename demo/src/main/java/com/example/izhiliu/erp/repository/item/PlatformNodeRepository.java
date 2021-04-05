package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.item.PlatformNode;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;


/**
 * Spring Data  repository for the PlatformNode entity.
 */
@Mapper
public interface PlatformNodeRepository extends BaseMapper<PlatformNode> {

    IPage<PlatformNode> pageByPlatformId(Page page, @Param("platformId") Long platformId);

    PlatformNode findByCurrency(String currency);

    PlatformNode findByCode(String code);
}
