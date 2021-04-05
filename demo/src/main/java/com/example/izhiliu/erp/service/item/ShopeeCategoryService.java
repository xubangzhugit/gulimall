package com.izhiliu.erp.service.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.domain.item.ShopeeCategory;
import com.izhiliu.erp.service.item.dto.ShopeeCategoryDTO;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing ShopeeCategory.
 */
public interface ShopeeCategoryService extends IBaseService<ShopeeCategory, ShopeeCategoryDTO> {

    String CACHE_ONE = "shopee-category-one";
    String CACHE_LIST = "shopee-category-list";
    String CACHE_PAGE = "shopee-category-page";
    String CACHE_PAGE$ = "shopee-category-page$";

    /**
     * 根据站点ID获取类目列表
     *
     * @param platformNodeId
     * @param page
     * @return
     */
    IPage<ShopeeCategoryDTO> pageByPlatformNode(long platformNodeId, Page page);

    /**
     * 根据最低级类目追溯至最高级类目
     *
     * @param id
     * @return
     */
    List<ShopeeCategoryDTO> listByForebears(long id);

    /**
     * 根据层级和站点ID获取类目列表
     *
     * @param tier
     * @param platformNodeId
     * @param page
     * @return
     */
    IPage<ShopeeCategoryDTO> pageByTierAndPlatformNode(int tier, long platformNodeId, Page page);

    /**
     * 根据层级和站点ID查询未汉化的类目列表
     *
     * @param tier
     * @param platformNodeId
     * @param page
     * @return
     */
    IPage<ShopeeCategoryDTO> pageByTierAndPlatformNodeAndChineseIsNull(int tier, long platformNodeId, Page page);

    /**
     * 根据父级查询子类目列表
     *
     * @param keyword
     * @param id
     * @param platformNodeId
     * @param page
     * @return
     */
    IPage<ShopeeCategoryDTO> pageByChild(String keyword, long id, long platformNodeId, Page page);

    /**
     * 根据站点ID和父类目ID和名称查询类目
     *
     * @param platformNodeId
     * @param parentId
     * @param shopeeCategoryId
     * @return
     */
    Optional<ShopeeCategoryDTO> findByPlatformNodeAndParentIdAndShopeeCategoryId(long platformNodeId, long parentId, long shopeeCategoryId);

    /**
     * 根据站点ID和虾皮类目ID查询类目
     *
     * @param platformNodeId
     * @param shopeeCategoryId
     * @return
     */
    Optional<ShopeeCategoryDTO> findByPlatformNodeAndShopeeCategory(long platformNodeId, long shopeeCategoryId);

    Optional<ShopeeCategoryDTO> findDeletedByPlatformNodeAndShopeeCategory(long platformNodeId, long shopeeCategoryId);

    boolean resume(long id);

    void deleted(long id);

    int handleOtherToLast(long platformNodeId);

    boolean invalidCategory(Long platformNodeId, long shopeeCategoryId);
}
