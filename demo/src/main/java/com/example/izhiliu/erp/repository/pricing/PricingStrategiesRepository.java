package com.izhiliu.erp.repository.pricing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.pricing.PricingStrategies;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PricingStrategiesRepository extends BaseMapper<PricingStrategies> {
    IPage<PricingStrategies> selectListByplatform(@Param("page") Page<PricingStrategies> page, @Param("login") String login, @Param("platformId") Integer platformId, @Param("platformNodeId") Integer platformNodeId);

    int selectCountByLoginAndPlatformAndPlatformNode(@Param("login") String login, @Param("platformId") Long platformId, @Param("platformNodeId") Long platformNodeId);

}
