package com.izhiliu.erp.repository.discount;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.izhiliu.erp.domain.discount.ShopeeDiscountItem;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountDetailDTO;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemDTO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountItemCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:25
 */
@Mapper
public interface DiscountItemRepository extends BaseMapper<ShopeeDiscountItem> {

    /**
     * 通过discountId和login删除折扣商品
     * @param login
     * @param discountId
     */
    boolean deleteByLoginAndDiscountId(@Param("login") String login, @Param("discountId") String discountId);

    /**
     * 批量删除
     * @param login
     * @param discountIds
     * @return
     */
    boolean deleteByLoginAndDiscountIds(@Param("login") String login, @Param("discountIds") List<String> discountIds);

    /**
     * 批量插入
     * @param list
     * @return
     */
    boolean insertBatch(List<ShopeeDiscountItemDTO> list);

    /**
     * 获取折扣商品数量
     * @param login
     * @param discountIds
     * @return
     */
    List<DiscountItemCount> getItemCount(@Param("login") String login, @Param("discountIds") List<String> discountIds);

}
