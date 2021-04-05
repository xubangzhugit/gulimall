package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.dto.ChildCount;
import com.izhiliu.erp.service.item.dto.ProductSearchStrategyDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;


/**
 * Spring Data  repository for the ShopeeProduct entity.
 */
@Mapper
public interface ShopeeProductRepository extends BaseMapper<ShopeeProduct> {

    List<ShopeeProduct> shopProduct(@Param("parentId") Long parentId, @Param("shopId") Long shopId);

    List<ShopeeProduct> nodeProduct(@Param("parentId") Long parentId, @Param("platformNodeId") Long platformNodeId);

    List<ShopeeProduct> childs(@Param("parentId") Long parentId, @Param("childType") Integer childType);

    List<ShopeeProduct> listShopProductByParentId(@Param("parentId") Long parentId, @Param("publishShops") List<Long> publishShops, @Param("unpublishShops") List<Long> unpublishShops);

    List<ShopeeProduct> listByMetaDataId(@Param("metaDataId") String metaDataId);

    IPage<ShopeeProduct> pageByCategoryId(Page page, @Param("categoryId") Long categoryId);

    ShopeeProduct selectByShopeeItemId(@Param("shopeeItemId") Long shopeeItemId);

    List<String> findAllBySource(String loginId);

    IPage<ShopeeProduct> search(@Param("page") Page page, @Param("loginId") String loginId, @Param("query") ProductSearchStrategyDTO query);

    IPage<ShopeeProduct> searchV2(@Param("page") Page page, @Param("loginId") String loginId, @Param("parentLoginId") String parentLoginId, @Param("query") ProductSearchStrategyDTO query);

    IPage<ShopeeProduct> searchShopProductByLoginId(@Param("page") Page page, @Param("loginId") String loginId, @Param("query") ProductSearchStrategyDTO query);

    int deleteByItemId(Long itemId);

    List<Long> selectByDeleted(@Param("page") Page page);

    String getCurrencyByShopIdAndItemId(@Param("shopId") Long shopId, @Param("itemId") Long itemId);

    String getLoginId(Long id);

    String getCurrencyById(Long id);

    List<Long> shopIds(long productId);

    List<ShopeeProduct> getShops(Long productId);

    List<ShopeeProduct> getShopsByMate(Long productId);

    int childCount(Long productId);

    List<ChildCount> childCountBatch(@Param("productIds") List<Long> productIds);

    Long pending(@Param("login") String login);

    Long fail(@Param("login") String login);

    List<ShopeeProduct> findList(@Param("productIds") List<Long> productIds, @Param("login") String login, @Param("status") Integer status, @Param("shopItem") Boolean shopItem);

    List<ShopeeProduct> selectImagesByProductIds(@Param("itemIds") Set<Long> itemIds, @Param("login") String login);

    List<ShopeeProduct> findLists(@Param("productIds") List<Long> productIds, @Param("login") String login, @Param("statusList") List<Integer> statusList);

    int updateStatusByItemId(@Param("productId") Long productId, @Param("status") Integer status);


    int updateDescMediaByProductId(@Param("productId") Long productId);

    List<ShopeeProduct> listBatch(@Param("shopeeItemId") List<Long> shopeeItemId);

    int deleteByDeleted(@Param("productId") Long productId);

    Long getProductIdByItemId(Long itemId);

    List<Long> selectByDeletedSite(@Param("number") int number, @Param("size") int size);

    List<ShopeeProduct> selectLogisticInfo(@Param("collect") List<Long> collect);

    List<ShopeeProduct> findIfShopeeItem(@Param("ids") List<Long> ids );

    List<ShopeeProduct> findShopeeProductList(@Param("itmeIds") List<Long> itmeIds);

    List<Long> queryUnpublish(@Param("loginId") String loginId, @Param("unpublishShops") List<Long> unpublishShops);

    /**
     * 查询三个月未更新的站点商品
     *
     * @return
     */
    List<Long> selectNoUpdateProduct();

    /**
     * 删除
     * @param ids
     * @return
     */
    int deleteByIds(@Param("ids") List<Long> ids);

}
