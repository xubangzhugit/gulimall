package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.izhiliu.erp.domain.item.ItemCategoryMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * Spring Data  repository for the ShopeeCategory entity.
 */
@Mapper
public interface CategoryMapRepository extends BaseMapper<ItemCategoryMap> {


    /**
     * @param platformId       dst_platform_id
     * @param platformNodeId   dst_platfrom_node_id
     * @param srcCategoryId    src_categroy_id
     * @param shopeeCategoryId dst_categroy_id
     * @return
     */
    ItemCategoryMap selectByObj(@Param("platformId") Long platformId, @Param("platformNodeId") Long platformNodeId, @Param("srcCategoryId") Long srcCategoryId, @Param("shopeeCategoryId") Long shopeeCategoryId);

    /**
     * @param platformId       dst_platform_id
     * @param platformNodeId   dst_platfrom_node_id
     * @param srcCategoryId    src_categroy_id
     * @param shopeeCategoryId dst_categroy_id
     * @return
     */
    int deleteByObj(@Param("platformId") Long platformId, @Param("platformNodeId") Long platformNodeId, @Param("srcCategoryId") Long srcCategoryId, @Param("shopeeCategoryId") Long shopeeCategoryId);


    int updateSuccessCountById(@Param("id") Long id);

    ItemCategoryMap selectOneByObj(@Param("mateDataplatform") Long mateDataplatform, @Param("mateDataplatformNodeId") Long mateDataplatformNodeId, @Param("mateDataCategoryId") Long mateDataCategoryId, @Param("productPlatform") Long productPlatform, @Param("productPlatformNodeId") Long productPlatformNodeId);
}
