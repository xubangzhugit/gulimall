package com.izhiliu.erp.service.item;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.domain.item.BoostItem;
import com.izhiliu.erp.domain.item.ProductSearchStrategy;
import com.izhiliu.erp.service.item.dto.BoostItemDTO;
import com.izhiliu.erp.service.item.dto.BoostMQItems;
import com.izhiliu.erp.service.item.dto.ProductSearchStrategyDTO;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM_V21;

import java.util.List;


public interface BatchBoostItemService  extends IBaseService<BoostItem, BoostItemDTO> {

    /**
     *   批量设置定时置顶商品
     * @param longs   这个是productId 哦~
     * @return
     */
   Boolean timingTopping(List<Long> longs);

    /**
     *   批量删除定时置顶商品
     * @param deleteId   这个是productId 哦~
     * @return
     */
    Boolean deletetimingTopping(List<Long> deleteId);

     int boostItem();

     int boostItem(BoostMQItems item, int count);

    BoostItem findProductId(Long productId);
     List<BoostItem> selectPartBoostItemByProductIds(List<Long> productIds);

    IPage<ProductListVM_V21> searchShopBoostProductByCurrentUser(ProductSearchStrategyDTO productSearchStrategy);

    /**
     * 置顶商品2版
     */
    void boostItemV2();
}
