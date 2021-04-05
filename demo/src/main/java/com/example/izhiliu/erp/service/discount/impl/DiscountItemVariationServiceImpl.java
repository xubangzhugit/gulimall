package com.izhiliu.erp.service.discount.impl;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.izhiliu.core.domain.common.BaseServiceNoLogicImpl;
import com.izhiliu.erp.common.BatchSaveExecutor;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.domain.discount.ShopeeDiscountItemVariation;
import com.izhiliu.erp.repository.discount.DiscountItemVariationRepository;
import com.izhiliu.erp.service.discount.DiscountItemVariationService;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemVariationDTO;
import com.izhiliu.erp.service.discount.mapper.DiscountItemVariationMapper;
import com.izhiliu.erp.web.rest.discount.qo.DiscountItemQO;
import com.izhiliu.open.shopee.open.sdk.api.discount.result.GetDiscountDetailResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:17
 */
@Service
@Slf4j
public class DiscountItemVariationServiceImpl extends BaseServiceNoLogicImpl<ShopeeDiscountItemVariation, ShopeeDiscountItemVariationDTO,
        DiscountItemVariationRepository, DiscountItemVariationMapper> implements DiscountItemVariationService {

    @Resource
    private BatchSaveExecutor batchSaveExecutor;

    @Override
    public Boolean syncVariations(DiscountItemQO qo) {
        final String login = qo.getLogin();
        final String discountId = qo.getDiscountId();
        List<GetDiscountDetailResult.Item> syncItems = qo.getSyncItems();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(discountId)) {
            return false;
        }
        repository.deleteByDiscountId(login, discountId);
        List<ShopeeDiscountItemVariationDTO> collect = syncItems.stream()
                .flatMap(e -> {
                    List<GetDiscountDetailResult.Variations> variations = e.getVariations();
                    if (CommonUtils.isBlank(variations)) {
                        return null;
                    }
                    return variations.stream().map(f -> {
                        ShopeeDiscountItemVariationDTO dto = new ShopeeDiscountItemVariationDTO();
                        dto.setVariationId(f.getVariationId());
                        dto.setVariationName(f.getVariationName());
                        dto.setVariationOriginalPrice((long) (f.getVariationOriginalPrice() * 100));
                        dto.setVariationPromotionPrice((long) (f.getVariationPromotionPrice() * 100));
                        dto.setVariationStock(f.getVariationStock());
                        dto.setLogin(login);
                        dto.setDiscountId(discountId);
                        dto.setItemId(e.getItemId());
                        return dto;
                    });
                })
                .filter(CommonUtils::isNotBlank)
                .collect(Collectors.toList());
        if (CommonUtils.isNotBlank(collect)) {
            batchSaveExecutor.getDiscountItemVariationResult(collect);
        }
        return true;
    }

    @Override
    public Boolean deleteByDiscountIds(String login, List<String> discountIds) {
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(discountIds)) {
            return false;
        }
        return repository.deleteByDiscountIds(login, discountIds);
    }

    @Override
    public List<ShopeeDiscountItemVariationDTO> listByDiscountIdAndLogin(String login, List<String> discountIds) {
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(discountIds)) {
            return new ArrayList<>();
        }
        List<ShopeeDiscountItemVariationDTO> list = list(new QueryWrapper<ShopeeDiscountItemVariation>()
                .eq("login", login)
                .in("discount_id", discountIds));
        return list;
    }
}
