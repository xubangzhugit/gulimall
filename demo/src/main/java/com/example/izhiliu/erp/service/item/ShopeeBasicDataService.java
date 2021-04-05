package com.izhiliu.erp.service.item;

import com.izhiliu.erp.service.item.dto.ShopeeAttributeDTO;
import com.izhiliu.erp.web.rest.item.param.LogisticsQueryQO;
import com.izhiliu.erp.web.rest.item.result.LogisticsVO;
import com.izhiliu.open.shopee.open.sdk.entity.logistics.LogisticsResult;

import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/3/4 9:47
 */
public interface ShopeeBasicDataService {

    /**
     * 根据Shopee类目ID获取属性与属性值
     *
     * @param categoryId     类目ID
     * @param platformNodeId 站点ID
     * @return
     */
    List<ShopeeAttributeDTO> getAllAttributeByCategoryId(Long categoryId, Long platformNodeId);

    List<ShopeeAttributeDTO> getAllAttributeByCategoryIdReCache(Long categoryId, Long platformNodeId);

    /**
     * 获取多个店铺的物流渠道并集
     *
     * @param shopIds
     * @return
     */
    List<LogisticsResult.LogisticsBean> getLogistics(List<Long> shopIds);

    /**
     * 获取多个店铺物流渠道，按店铺分组
     * @param qo
     * @return
     */
    List<LogisticsVO> getLogisticsV3(LogisticsQueryQO qo);

    boolean refreshLogistics(List<Long> shopId);

    boolean refreshAllAttributeByCategoryIdReCache(Long categoryId, Long platformNodeId);
}
