package com.izhiliu.erp.service.item.mapper.impl;

import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.domain.item.ShopeeProductSku;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.service.item.mapper.ShopeeProductSkuMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ShopeeProductSkuMapperImpl implements ShopeeProductSkuMapper {

    @Resource
    private ShopeeProductService shopeeProductService;

    @Override
    public ShopeeProductSku toEntity(ShopeeProductSkuDTO dto) {
        if (dto == null) {
            return null;
        }

        ShopeeProductSku entity = new ShopeeProductSku();

        entity.setId(dto.getId());
        entity.setFeature(dto.getFeature());
        entity.setGmtCreate(dto.getGmtCreate());
        entity.setGmtModified(dto.getGmtModified());
        entity.setSkuCode(dto.getSkuCode());
        entity.setStock(dto.getStock());
        entity.setImage(dto.getImage());
        entity.setSendOutTime(dto.getSendOutTime());
        entity.setSkuOptionOneIndex(dto.getSkuOptionOneIndex());
        entity.setSkuOptionTowIndex(dto.getSkuOptionTowIndex());
        entity.setProductId(dto.getProductId());
        entity.setShopeeVariationId(dto.getShopeeVariationId());
        entity.setCurrency(dto.getCurrency());
        entity.setSold(dto.getSold());
        entity.setDiscountId(dto.getDiscountId());
        entity.setDiscount(ShopeeUtil.apiInputPrice(dto.getDiscount(),dto.getCurrency()));
//        if(Objects.nonNull(dto.getPrice())&&Objects.nonNull(dto.getOriginalPrice())){
//            entity.setDiscount(ShopeeProductSkuUtil.discount(dto.getPrice(),dto.getOriginalPrice()));
//        }


           String currencyById =null;

        if (Objects.nonNull(dto.getPrice())) {
            if (StringUtils.isNotBlank(dto.getCurrency())) {
                entity.setPrice(ShopeeUtil.apiInputPrice(dto.getPrice(), dto.getCurrency()));
            } else {
                currencyById = shopeeProductService.getCurrencyById(dto.getProductId());
                entity.setPrice(ShopeeUtil.apiInputPrice(dto.getPrice(), currencyById));
            }
        } else {
            entity.setPrice(dto.getCollectPrice());
        }

        if(Objects.nonNull(dto.getOriginalPrice())){
            if (StringUtils.isNotBlank(dto.getCurrency())) {
                entity.setOriginalPrice(ShopeeUtil.apiInputPrice(dto.getOriginalPrice(), dto.getCurrency()));
            } else {
                if(Objects.isNull(currencyById)){
                    currencyById = shopeeProductService.getCurrencyById(dto.getProductId());
                }
                entity.setOriginalPrice(ShopeeUtil.apiInputPrice(dto.getOriginalPrice(), currencyById));
            }
        }

        return entity;
    }

    @Override
    public ShopeeProductSkuDTO toDto(ShopeeProductSku entity) {
        if (entity == null) {
            return null;
        }

        ShopeeProductSkuDTO dto = new ShopeeProductSkuDTO();

        dto.setId(entity.getId());
        dto.setSkuCode(entity.getSkuCode());
        dto.setCurrency(entity.getCurrency());
        dto.setStock(entity.getStock());
        dto.setImage(entity.getImage());
        dto.setSendOutTime(entity.getSendOutTime());
        dto.setGmtCreate(entity.getGmtCreate());
        dto.setGmtModified(entity.getGmtModified());
        dto.setFeature(entity.getFeature());
        dto.setSkuOptionOneIndex(entity.getSkuOptionOneIndex());
        dto.setSkuOptionTowIndex(entity.getSkuOptionTowIndex());
        dto.setProductId(entity.getProductId());
        dto.setShopeeVariationId(entity.getShopeeVariationId());
        dto.setSold(entity.getSold());
        dto.setDiscountId(entity.getDiscountId());

        dto.setPrice(ShopeeUtil.outputPrice(entity.getPrice(), entity.getCurrency()));
        if(Objects.nonNull(entity.getOriginalPrice())){
            dto.setOriginalPrice(ShopeeUtil.outputPrice(entity.getOriginalPrice(), entity.getCurrency()));
        }
        if(Objects.nonNull(entity.getDiscount())){
            dto.setDiscount(ShopeeUtil.outputPrice(entity.getDiscount(), entity.getCurrency()));
        }

        return dto;
    }

    @Override
    public List<ShopeeProductSku> toEntity(List<ShopeeProductSkuDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        List<ShopeeProductSku> list = new ArrayList<ShopeeProductSku>(dtoList.size());
        for (ShopeeProductSkuDTO shopeeProductSkuDTO : dtoList) {
            list.add(toEntity(shopeeProductSkuDTO));
        }

        return list;
    }

    @Override
    public List<ShopeeProductSkuDTO> toDto(List<ShopeeProductSku> entityList) {
        if (entityList == null) {
            return null;
        }

        List<ShopeeProductSkuDTO> list = new ArrayList<ShopeeProductSkuDTO>(entityList.size());
        for (ShopeeProductSku shopeeProductSku : entityList) {
            list.add(toDto(shopeeProductSku));
        }

        return list;
    }
}
