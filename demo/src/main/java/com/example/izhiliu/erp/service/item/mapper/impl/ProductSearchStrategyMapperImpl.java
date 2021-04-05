package com.izhiliu.erp.service.item.mapper.impl;

import cn.hutool.core.util.StrUtil;
import com.izhiliu.erp.domain.item.ProductSearchStrategy;
import com.izhiliu.erp.service.item.dto.ProductSearchStrategyDTO;
import com.izhiliu.erp.service.item.mapper.ProductSearchStrategyMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/19 14:12
 */
@Component
public class ProductSearchStrategyMapperImpl implements ProductSearchStrategyMapper {

    @Override
    public ProductSearchStrategy toEntity(ProductSearchStrategyDTO dto) {
        if (dto == null) {
            return null;
        }

        final ProductSearchStrategy entity = new ProductSearchStrategy();
        entity.setId(dto.getId());
        entity.setLoginId(dto.getLoginId());
        entity.setField(dto.getField());
        entity.setName(dto.getName());
        entity.setPublishShops(dto.getPublishShops());
        entity.setSubAccounts(dto.getSubAccounts());
        entity.setUnpublishShops(dto.getUnpublishShops());
        entity.setSource(dto.getSource());
        entity.setVariationTier(dto.getVariationTier());
        entity.setKeyword(dto.getKeyword());
        entity.setType(dto.getType());
        entity.setLocalStatus(dto.getLocalStatus());
        entity.setRemoteStatus(dto.getRemoteStatus());
        entity.setRecently(dto.getRecently());
        entity.setUrecently(dto.getUrecently());
        entity.setOnline(dto.getOnline());

        if (StrUtil.isNotBlank(dto.getStartDate()) && StrUtil.isNotBlank(dto.getEndDate())) {
            entity.setStartDate(LocalDateTime.parse(dto.getStartDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.systemDefault()).toInstant());
            entity.setEndDate(LocalDateTime.parse(dto.getEndDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.systemDefault()).toInstant());
        }

        if (StrUtil.isNotBlank(dto.getUstartDate()) && StrUtil.isNotBlank(dto.getUendDate())) {
            entity.setUstartDate(LocalDateTime.parse(dto.getUstartDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.systemDefault()).toInstant());
            entity.setUendDate(LocalDateTime.parse(dto.getUendDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.systemDefault()).toInstant());
        }

        return entity;
    }

    @Override
    public ProductSearchStrategyDTO toDto(ProductSearchStrategy entity) {
        if (entity == null) {
            return null;
        }

        final ProductSearchStrategyDTO dto = new ProductSearchStrategyDTO();
        dto.setId(entity.getId());
        dto.setLoginId(entity.getLoginId());
        dto.setField(entity.getField());
        dto.setName(entity.getName());
        dto.setPublishShops(entity.getPublishShops());
        dto.setSubAccounts(entity.getSubAccounts());
        dto.setUnpublishShops(entity.getUnpublishShops());
        dto.setSource(entity.getSource());
        dto.setVariationTier(entity.getVariationTier());
        dto.setKeyword(entity.getKeyword());
        dto.setLocalStatus(entity.getLocalStatus());
        dto.setRemoteStatus(entity.getRemoteStatus());
        dto.setRecently(entity.getRecently());
        dto.setUrecently(entity.getUrecently());
        dto.setOnline(entity.getOnline());

        if (entity.getStartDate() != null && entity.getEndDate() != null) {
            dto.setStartDate(LocalDateTime.ofInstant(entity.getStartDate(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            dto.setEndDate(LocalDateTime.ofInstant(entity.getEndDate(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (entity.getUstartDate() != null && entity.getUendDate() != null) {
            dto.setUstartDate(LocalDateTime.ofInstant(entity.getUstartDate(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            dto.setUendDate(LocalDateTime.ofInstant(entity.getUendDate(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return dto;
    }

    @Override
    public List<ProductSearchStrategy> toEntity(List<ProductSearchStrategyDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        final List<ProductSearchStrategy> entitys = new ArrayList<>(dtoList.size());
        for (ProductSearchStrategyDTO dto : dtoList) {
            entitys.add(toEntity(dto));
        }
        return entitys;
    }

    @Override
    public List<ProductSearchStrategyDTO> toDto(List<ProductSearchStrategy> entityList) {
        if (entityList == null) {
            return null;
        }

        final List<ProductSearchStrategyDTO> dtos = new ArrayList<>(entityList.size());
        for (ProductSearchStrategy entity : entityList) {
            dtos.add(toDto(entity));
        }
        return dtos;
    }
}
