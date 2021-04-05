package com.izhiliu.erp.service.discount.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.core.util.Constants;
import com.izhiliu.erp.config.BodyValidStatus;
import com.izhiliu.erp.config.module.currency.CurrencyRateApiImpl;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateResult;
import com.izhiliu.erp.domain.item.PlatformNode;
import com.izhiliu.erp.domain.pricing.PricingStrategies;
import com.izhiliu.erp.repository.pricing.PricingStrategiesRepository;
import com.izhiliu.erp.service.item.PlatformNodeService;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;
import com.izhiliu.erp.service.discount.PricingStrategiesService;
import com.izhiliu.erp.service.discount.dto.PricingStrategiesDto;
import com.izhiliu.erp.service.discount.mapper.PricingStrategiesMapper;
import com.izhiliu.erp.util.SnowflakeGenerate;
import com.izhiliu.erp.web.rest.errors.DataNotFoundException;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PricingStrategiesServiceImpl extends IBaseServiceImpl<PricingStrategies, PricingStrategiesDto, PricingStrategiesRepository, PricingStrategiesMapper> implements PricingStrategiesService {

    @Resource
    PlatformNodeService platformNodeService;

    @Resource
    SnowflakeGenerate snowflakeGenerate;


    @Resource
    CurrencyRateApiImpl currencyRateApi;


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PricingStrategiesDto checke(Long id) {
        final Optional<PricingStrategiesDto> boostItemDTO1 = this.find(id);
        final PricingStrategiesDto boostItemDTO2 = boostItemDTO1.orElseThrow(() -> new IllegalOperationException("illegal.operation.exception", true));
        final String currentUserLogin = SecurityUtils.currentLogin();
        if (!boostItemDTO2.getLogin().equals(currentUserLogin) && !currentUserLogin.equals(Constants.ANONYMOUS_USER)) {
            throw new IllegalOperationException("illegal.operation.exception", true);
        }
        return boostItemDTO2;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<PricingStrategiesDto> checke(List<Long> ids) {
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public IPage<PricingStrategiesDto> selectListByplatform(Integer platformId, Integer platformNodeId) {
        final String login = SecurityUtils.currentLogin();
        final IPage<PricingStrategiesDto> map = toDTO(repository.selectListByplatform(new Page(1, 10), login, platformId, platformNodeId));
        return map;
    }


    @Override
    public ResponseEntity save(PricingStrategiesDto dto, String login) {
        final Long platformNodeId = dto.getPlatformNodeId();
        int size = repository.selectCountByLoginAndPlatformAndPlatformNode(login, dto.getPlatformId(), platformNodeId);
        String title = null;
        if (size < 7) {
            dto.setId(getId());
            dto.setLogin(login);
            dto.setToCurrency(platformNodeService.find(platformNodeId).orElseThrow(() -> new DataNotFoundException("data.not.found.exception.platform_node_id.not.found", new String[]{platformNodeId + ""})).getCurrency());
            return ResponseEntity.ok(super.save(dto));
        } else {
            title = "您在该站点 该平台 的 模板数量已达上限";
        }
        return ResponseEntity.status(500).body(
                BodyValidStatus.builder()
                        .code("500")
                        .title(title)
                        .field("parameter_check_exception")
                        .type("Validated").build());
    }


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long getId() {
        return snowflakeGenerate.nextId();
    }


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<PlatformNodeDTO> platformNodesAsCurrency() {
        final IPage<PlatformNodeDTO> page = platformNodeService.page(new Page<PlatformNode>(1, 10));
        final List<PlatformNodeDTO> records = page.getRecords();
        records.forEach(platformNodeDTO -> clean(platformNodeDTO));
        return records;
    }

    //    首重价格 + ((商品重量 - 首重重量) / 续重单位重量) * 每续重单位价格
    //    计算公式：(运费成本 + 商品成本) * (1 + 利润率 + 其他费用)
//    @Override
//    public float pricing(PricingStrategiesDto pricingStrategies, float price, float weight, CurrencyRateResult currencyRateResult) {
//        final Optional<PricingStrategiesDto> pricingStrategiesDto = this.find(pricingId);
//        if (pricingStrategiesDto.isPresent()) {
//            final PricingStrategiesDto pricingStrategies = pricingStrategiesDto.get();
//            final CurrencyRateResult currencyRateResult = currencyRateApi.currencyConvert(pricingStrategies.getFromCurrency(), pricingStrategies.getToCurrency());
//            return doPricing(pricingStrategies, price, weight,currencyRateResult);
//        }
//        return price;
//    }


    @Override
    public float pricing(PricingStrategiesDto pricingStrategies, float price, float weight, CurrencyRateResult currencyRateResult) {
        final  float  freightCost ;
        final  float  firstWeight ;
        float  sumProfit  =  1 ;

        if(pricingStrategies.getIsFirstWeight()){
            firstWeight  =  0f;
        }else{
            firstWeight = pricingStrategies.getFirstWeight();
        }

        if(weight < firstWeight){
            freightCost = pricingStrategies.getFirstPrice();
        }else{
            freightCost = pricingStrategies.getFirstPrice() + ((weight - firstWeight) / pricingStrategies.getContinuedWeight()) * pricingStrategies.getContinuedWeightPrice();
        }

        if (Objects.nonNull(pricingStrategies.getProfit())) {
            sumProfit = sumProfit + pricingStrategies.getProfit();
        }
        if (Objects.nonNull(pricingStrategies.getOtherPrice())) {
            sumProfit = sumProfit + pricingStrategies.getOtherPrice();
        }
        if (Objects.nonNull(pricingStrategies.getDiscount())) {
            sumProfit = sumProfit + pricingStrategies.getDiscount();
        }

        final float productCost = (freightCost + price) * sumProfit;

        return new BigDecimal(productCost).multiply(currencyRateResult.getRate()).setScale(2,BigDecimal.ROUND_HALF_UP).floatValue();
    }


    private void clean(PlatformNodeDTO platformNodeDTO) {
        platformNodeDTO.setId(null);
        platformNodeDTO.setUrl(null);
        platformNodeDTO.setLanguage(null);
        platformNodeDTO.setGmtCreate(null);
        platformNodeDTO.setGmtModified(null);
        platformNodeDTO.setFeature(null);
        platformNodeDTO.setPlatformId(null);
        platformNodeDTO.setStatus(null);
        platformNodeDTO.setLastSyncTime(null);
        platformNodeDTO.setGlobalLanguage(null);
    }
}
