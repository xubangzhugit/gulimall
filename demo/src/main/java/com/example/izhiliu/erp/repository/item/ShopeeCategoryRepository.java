package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.item.ShopeeCategory;
import com.izhiliu.erp.service.item.dto.ShopeeCategoryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * Spring Data  repository for the ShopeeCategory entity.
 */
@Mapper
public interface ShopeeCategoryRepository extends BaseMapper<ShopeeCategory> {

    IPage<ShopeeCategory> pageByPlatformNodeId(Page page, @Param("platformNodeId") Long platformNodeId);

    IPage<ShopeeCategory> pageByPlatformNodeIdOrderBySortDesc(Page page, @Param("platformNodeId") Long platformNodeId);

    IPage<ShopeeCategory> pageByTierAndPlatformNode(Page page, @Param("tier") Integer tier, @Param("platformNodeId") Long platformNodeId);

    IPage<ShopeeCategory> pageByParentId(Page page, @Param("keyword") String keyword, @Param("parentId") Long parentId, @Param("platformNodeId") Long platformNodeId);

    ShopeeCategoryDTO findByPlatformNodeAndParentIdAndShopeeCategoryId(@Param("platformNodeId") Long platformNodeId, @Param("parentId") Long parentId, @Param("shopeeCategoryId") Long shopeeCategoryId);

    ShopeeCategoryDTO findByPlatformNodeIdAndShopeeCategoryId(@Param("platformNodeId") Long platformNodeId, @Param("shopeeCategoryId") Long shopeeCategoryId);

    IPage<ShopeeCategory> pageByTierAndPlatformNodeAndChineseIsNull(Page page, @Param("tier") int tier, @Param("platformNodeId") long platformNodeId);

    /**
     * 把 "Other" 类目放到最底部
     *
     * @param platformNodeId
     * @return
     */
    int handleOtherToLast(long platformNodeId);

    ShopeeCategory findDeletedByPlatformNodeAndShopeeCategoryId(@Param("platformNodeId") long platformNodeId, @Param("shopeeCategoryId") long shopeeCategoryId);

    void deleted(long id);

    int resume(long id);

    int invalidCategory(@Param("platformNodeId") Long platformNodeId, @Param("shopeeCategoryId") long shopeeCategoryId);
}
