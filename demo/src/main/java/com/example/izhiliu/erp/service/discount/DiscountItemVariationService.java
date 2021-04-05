package com.izhiliu.erp.service.discount;

import com.izhiliu.core.domain.common.BaseService;
import com.izhiliu.erp.domain.discount.ShopeeDiscountItemVariation;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemVariationDTO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountItemQO;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:11
 */
public interface DiscountItemVariationService extends BaseService<ShopeeDiscountItemVariation, ShopeeDiscountItemVariationDTO> {

    /**
     * 同步折扣商品sku信息
     * @param qo
     * @return
     */
    Boolean syncVariations(DiscountItemQO qo);

    /**
     * 通过折扣id批量删除
     * @param login
     * @param discountIds
     * @return
     */
    Boolean deleteByDiscountIds(String login, List<String> discountIds);

    /**
     * 通过折扣id获取折扣商品sku信息
     * @param login
     * @param discountIds
     * @return
     */
    List<ShopeeDiscountItemVariationDTO> listByDiscountIdAndLogin(String login, List<String> discountIds);
}
