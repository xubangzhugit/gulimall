package com.izhiliu.erp.repository.discount;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.izhiliu.erp.domain.discount.ShopeeDiscountItemVariation;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemVariationDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/8/11 15:03
 */
@Mapper
public interface DiscountItemVariationRepository extends BaseMapper<ShopeeDiscountItemVariation> {

    /**
     * 通过折扣id删除sku信息
     * @param login
     * @param discountId
     * @return
     */
    boolean deleteByDiscountId(@Param("login") String login, @Param("discountId") String discountId);

    /**
     * 批量删除sku信息
     * @param login
     * @param list
     * @return
     */
    boolean deleteByDiscountIds(@Param("login") String login, @Param("discountIds") List<String> discountIds);

    /**
     * 批量新增
     * @param list
     * @return
     */
    boolean insertBatch(List<ShopeeDiscountItemVariationDTO> list);
}
