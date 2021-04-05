package com.izhiliu.erp.service.discount;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateResult;
import com.izhiliu.erp.domain.pricing.PricingStrategies;
import com.izhiliu.erp.service.image.CustomizeBaseService;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;
import com.izhiliu.erp.service.discount.dto.PricingStrategiesDto;
import org.springframework.http.ResponseEntity;

import java.util.List;


public interface PricingStrategiesService extends CustomizeBaseService<PricingStrategies,PricingStrategiesDto> {
    IPage<PricingStrategiesDto> selectListByplatform(Integer platformId, Integer platformNodeId);


    ResponseEntity save(PricingStrategiesDto pricingStrategiesDto, String  login);

    List<PlatformNodeDTO> platformNodesAsCurrency();


     float   pricing(PricingStrategiesDto pricingStrategies, float price, float weight, CurrencyRateResult currencyRateResult);

}
