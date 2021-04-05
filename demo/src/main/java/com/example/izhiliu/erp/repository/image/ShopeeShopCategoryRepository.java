package com.izhiliu.erp.repository.image;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.item.ShopeeShopCategory;
import com.izhiliu.erp.domain.item.ShopeeShopCategoryItem;
import com.izhiliu.erp.service.image.result.ShopCategorySelect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Seriel
 * @create 2019-08-27 10:04
 **/
@Mapper
public interface ShopeeShopCategoryRepository extends BaseMapper<ShopeeShopCategory> {

    int queryProductCountByshopCategoryId(@Param("shopCategoryId") Long shopCategoryId);

    void replace(@Param("collect") List<ShopeeShopCategoryItem> collect, @Param("object") ShopeeShopCategoryItem object);

    int removeProductByshopCategoryId(@Param("shopCategoryId") Long shopCategoryId);

    IPage<ShopeeShopCategory> searchProductByCurrentUser(@Param("page") Page page, @Param("loginId") String loginId, @Param("query") ShopCategorySelect productSearchStrategy);

    int removeShopCategoryByShopId(@Param("shopId") Long shopId);

    int deleteItemByShopCategoryIdAndItems(@Param("shopCategoryId") Long shopCategoryId, @Param("productIds") List<Long> productIds,@Param("loginId") String loginId);

    List<Long> selectProductIdsByCategoryId(@Param("page") Page page,@Param("shopCategoryId") Long categoryId);

    IPage<Long> queryProductIdByshopCategoryId(@Param("page") Page page, @Param("loginId") String currentUserLogin, @Param("cateagoryId") Long cateagoryId, @Param("productItemId") Long productItemId);
}
