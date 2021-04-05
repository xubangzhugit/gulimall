package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.izhiliu.erp.domain.item.ShopeeProductExtraInfo;
import com.izhiliu.erp.service.item.dto.ShopeeProductExtraInfoDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * Spring Data  repository for the ShopeeProductDesc entity.
 */
@Mapper
public interface ShopeeProductExtraInfoRepository extends BaseMapper<ShopeeProductExtraInfo> {

    ShopeeProductExtraInfo selectByProductId(@Param("productId") Long productId);

    int updateByProductId(@Param("shopeeProductMedia") ShopeeProductExtraInfo shopeeProductMedia);

    List<ShopeeProductExtraInfo> selectMainFinalInfoByProductId(@Param("productIds") List<Long> productIds);
}
