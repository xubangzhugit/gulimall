package com.izhiliu.erp.service.item.mapper.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.ShopeeProductAttributeValueService;
import com.izhiliu.erp.service.item.ShopeeProductSkuService;
import com.izhiliu.erp.service.item.dto.BatchEditProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.mapper.BatchEditProductMapper;
import com.izhiliu.erp.service.item.mapper.ShopeeProductMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/9 15:35
 */
@Component
public class BatchEditProductMapperImpl implements BatchEditProductMapper {

    @Resource
    private ShopeeProductMapper shopeeProductMapper;

    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;

    @Resource
    private ShopeeProductAttributeValueService shopeeProductAttributeValueService;

    @Override
    public ShopeeProduct toEntity(BatchEditProductDTO dto) {
        return shopeeProductMapper.toEntity(dto);
    }

    @Override
    public BatchEditProductDTO toDto(ShopeeProduct entity) {
        if (entity == null) {
            return null;
        }

        final BatchEditProductDTO dto = new BatchEditProductDTO();
        dto.setCategoryId(entity.getCategoryId());
        dto.setPlatformId(entity.getPlatformId());
        dto.setPlatformNodeId(entity.getPlatformNodeId());

        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setSkuCode(entity.getSkuCode());
        dto.setShopId(entity.getShopId());
        dto.setSendOutTime(entity.getSendOutTime());

        dto.setCurrency(entity.getCurrency());
        dto.setVWeight(ShopeeUtil.outputWeight(entity.getWeight()));

        if (!StrUtil.isBlank(entity.getImages())) {
            dto.setImages(JSON.parseArray(entity.getImages(), String.class).stream().filter(StrUtil::isNotBlank).collect(toList()));
        }
        if (!StrUtil.isBlank(entity.getLogistics())) {
            dto.setLogistics(JSON.parseArray(entity.getLogistics(), ShopeeProductDTO.Logistic.class));
        }

        dto.setVariationTier(entity.getVariationTier());
        dto.setVariationWrapper(shopeeProductSkuService.variationByProduct(entity.getId()));
        dto.setAttributeValues(shopeeProductAttributeValueService.selectByProduct(entity.getId()));
        return dto;
    }

    @Override
    public List<ShopeeProduct> toEntity(List<BatchEditProductDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        final List<ShopeeProduct> list = new ArrayList<>(dtoList.size());
        for (BatchEditProductDTO dto : dtoList) {
            list.add(toEntity(dto));
        }

        return list;
    }

    @Override
    public List<BatchEditProductDTO> toDto(List<ShopeeProduct> entityList) {
        if (entityList == null) {
            return null;
        }

        final List<BatchEditProductDTO> list = new ArrayList<>(entityList.size());
        for (ShopeeProduct entity : entityList) {
            list.add(toDto(entity));
        }

        return list;
    }
}
