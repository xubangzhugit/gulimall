package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.item.BoostItem;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.dto.BoostItemDTO;
import com.izhiliu.erp.service.item.dto.ProductSearchStrategyDTO;
import com.izhiliu.erp.service.item.impl.BatchBoostItemServiceImpl;
import com.izhiliu.erp.web.rest.common.PageRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/15 15:34
 */
@Mapper
public interface BoostItemRepository extends BaseMapper<BoostItem> {

    int deleteBatchProductIds(@Param("productIds") Collection<Long> productIds);

    /**
     * 只查询部分属性出来
     *
     * @param productIds
     * @return
     */
    List<BoostItem> selectPartBoostItemByProductIds(@Param("productIds") Collection<Long> productIds);

    List<BoostItem> getDistinctLogin(@Param("page") BatchBoostItemServiceImpl.MyPage page);

    void batchStatusByIds(@Param("collect") List<Long> collect, @Param("status") int status);


    Integer getDistinctShopIdCount();


    Integer getCountByLogin(@Param("login") String login, @Param("shopId") Long shopId, @Param("statue") int status);

    BoostItem findProductId(@Param("productId") Long productId);

    List<BoostItem> selectListByLoginAndStatus(@Param("boostItemPage") Page<BoostItem> boostItemPage, @Param("shopId") Long shopId, @Param("login") String login, @Param("status") byte status);

    int deleteByDeleted(@Param("productId") Long productId);

    IPage<ShopeeProduct> selectPartBoostItemByShopId(@Param("page") IPage page, @Param("currentLogin") String currentLogin, @Param("boostStatus") Integer status, @Param("query") ProductSearchStrategyDTO query);

    void updateByLoginId(@Param("loginId") String loginId,@Param("oldStatus") Integer oldStatus, @Param("status") int status);

    List<BoostItem> selectBoostItemByShopId(@Param("page") BatchBoostItemServiceImpl.MyPage page, @Param("shopId") Long shopId,@Param("status") Integer status);

    List<Long> getDistinctShopIds();

    int updateBoostStatusForUnBoostItem(@Param("shopId")Long shopId,@Param("itemIds") List<Long> itemIds);

    List<BoostItem> findAvailableBoostItem(@Param("shopId")Long shopId, @Param("availableCount")int availableCount);

    int updateBoostingStatusForBoostedItem(@Param("shopId")Long shopId, @Param("itemIds")List<Long> boostedItemIds);
}
