package com.izhiliu.erp.service.item.mapper.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.mapper.ShopeeProductMapper;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import com.izhiliu.erp.util.FeatrueUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Component
public class ShopeeProductMapperImpl implements ShopeeProductMapper {

    @Resource
    private ShopeeProductService shopeeProductService;

    @Autowired
    @Qualifier(value = "handleProductExceptionInfo")
    private HandleProductExceptionInfo messageSource;

    @Override
    public ShopeeProduct toEntity(ShopeeProductDTO dto) {
        if (dto == null) {
            return null;
        }

        ShopeeProduct entity = new ShopeeProduct();

        entity.setId(dto.getId());
        entity.setName(dto.getName());
        //迁移到了 详情表
//        entity.setDescription(dto.getDescription());
        entity.setCollectUrl(dto.getCollectUrl());
        entity.setCollect(dto.getCollect());
        entity.setSourceUrl(dto.getSourceUrl());
        entity.setPrice(dto.getPrice());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setCurrency(dto.getCurrency());
        entity.setStock(dto.getStock());
        entity.setSold(dto.getSold());
        entity.setSkuCode(dto.getSkuCode());
        entity.setWeight(dto.getWeight());
        entity.setLength(dto.getLength());
        entity.setWidth(dto.getWidth());
        entity.setHeight(dto.getHeight());
        entity.setSendOutTime(dto.getSendOutTime());
        entity.setGmtCreate(dto.getGmtCreate());
        entity.setGmtModified(dto.getGmtModified());
        entity.setShopeeItemId(dto.getShopeeItemId());
        entity.setParentId(dto.getParentId());
        entity.setCategoryId(dto.getCategoryId());
        entity.setShopeeCategoryId(dto.getShopeeCategoryId());
        entity.setVariationTier(dto.getVariationTier());
        entity.setOldVariationTier(dto.getOldVariationTier());
        entity.setPlatformNodeId(dto.getPlatformNodeId());
        entity.setPlatformId(dto.getPlatformId());
        entity.setShopId(dto.getShopId());
        entity.setStatus(dto.getStatus());
        entity.setShopeeItemStatus(dto.getShopeeItemStatus());
        entity.setLoginId(dto.getLoginId());
        entity.setMetaDataId(dto.getMetaDataId());
        entity.setOldPlatformId(dto.getOldPlatformId());
        entity.setOldPlatformNodeId(dto.getOldPlatformNodeId());
        entity.setType(dto.getType());
        entity.setOnlineUrl(dto.getOnlineUrl());
        entity.setWarning(dto.getWarning());
        entity.setCrossBorder(dto.getCrossBorder());
        entity.setFeature(dto.getFeature());


        if (dto.getVPrice() != null && (dto.getVPrice() != 0 || dto.getVPrice() != ZERO)) {
            entity.setPrice(ShopeeUtil.apiInputPrice(dto.getVPrice(), dto.getCurrency()));
        }
        if (dto.getMaxPrice() != null && (dto.getMaxPrice() != 0 || dto.getMaxPrice() != ZERO)) {
            entity.setMaxPrice(ShopeeUtil.apiInputPrice(dto.getMaxPrice(), dto.getCurrency()));
        }
        if (dto.getMinPrice() != null && (dto.getMinPrice() != 0 || dto.getMinPrice() != ZERO)) {
            entity.setMinPrice(ShopeeUtil.apiInputPrice(dto.getMinPrice(), dto.getCurrency()));
        }

        if (dto.getVWeight() != null) {
            entity.setWeight(ShopeeUtil.inputWeight(dto.getVWeight()));
        }

        /*
         * TODO 若不判空每次Update时极有可能被置空，迁移到了 媒体表

         */
//        if (dto.getImages() != null) {
//            entity.setImages(JSON.toJSONString(limitImagesLength(dto.getImages().stream().filter(StrUtil::isNotBlank).collect(toList()))));
//        }
        if (dto.getLogistics() != null) {
            dto.getLogistics().forEach(e -> {
                if (e.getEnabled() == null || !e.getEnabled()) {
                    e.setEnabled(true);
                }
            });
            entity.setLogistics(JSON.toJSONString(dto.getLogistics()));
        }

        return entity;
    }

    @Override
    public ShopeeProductDTO toDto(ShopeeProduct entity) {
        if (entity == null) {
            return null;
        }

        ShopeeProductDTO dto = new ShopeeProductDTO();

        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCollectUrl(entity.getCollectUrl());
        dto.setCollect(entity.getCollect());
        dto.setSourceUrl(entity.getSourceUrl());
        dto.setPrice(entity.getPrice());
        dto.setOriginalPrice(entity.getOriginalPrice());
        dto.setCurrency(entity.getCurrency());
        dto.setStock(entity.getStock());
        dto.setSold(entity.getSold());
        dto.setSkuCode(entity.getSkuCode());
        dto.setWeight(entity.getWeight());
        dto.setLength(entity.getLength());
        dto.setWidth(entity.getWidth());
        dto.setHeight(entity.getHeight());
        dto.setSendOutTime(entity.getSendOutTime());
        dto.setGmtCreate(entity.getGmtCreate());
        dto.setGmtModified(entity.getGmtModified());
        dto.setShopeeItemId(entity.getShopeeItemId());
        dto.setParentId(entity.getParentId());
        dto.setCategoryId(entity.getCategoryId());
        dto.setShopeeCategoryId(entity.getShopeeCategoryId());
        dto.setVariationTier(entity.getVariationTier());
        dto.setOldVariationTier(entity.getOldVariationTier());
        dto.setPlatformNodeId(entity.getPlatformNodeId());
        dto.setPlatformId(entity.getPlatformId());
        dto.setShopId(entity.getShopId());
        dto.setStatus(entity.getStatus());
        dto.setShopeeItemStatus(entity.getShopeeItemStatus());
        dto.setLoginId(entity.getLoginId());
        dto.setMetaDataId(entity.getMetaDataId());
        dto.setOldPlatformId(entity.getOldPlatformId());
        dto.setOldPlatformNodeId(entity.getOldPlatformNodeId());
        dto.setType(entity.getType());
        dto.setOnlineUrl(entity.getOnlineUrl());
        dto.setWarning(entity.getWarning());
        dto.setCrossBorder(entity.getCrossBorder());


        // 兼容以前不是json 数据
        if (ShopeeProduct.Type.SHOP.code.equals(entity.getType())) {
            String error;
            try {
                final JSONObject jsonObject = JSONObject.parseObject(entity.getFeature());
                error  = jsonObject.getString("error");
                dto.setStartDateTime(jsonObject.getString("start"));
                dto.setEndDateTime(jsonObject.getString("end"));
            } catch (Exception e) {
//                    e.printStackTrace();
                error =entity.getFeature();
            }
            if ("success".equals(error)) {
                dto.setFailReason(null);
            }else {
                dto.setFailReason(messageSource.getMessage(error));
            }
        }

        if (ShopeeProduct.Type.PLATFORM_NODE.code.equals(entity.getType())) {

            //todo move to service
            dto.setShopIds(shopeeProductService.shopIds(entity.getId()));
        }
        dto.setNewStatus(messageSource.country(entity.getStatus()));
        dto.setNewShopeeItemStatus(messageSource.country(entity.getShopeeItemStatus()));
//        dto.setVPrice(ShopeeUtil.outputPrice(entity.getPrice(), entity.getCurrency()));
//        dto.setMaxPrice(ShopeeUtil.outputPrice(entity.getMaxPrice(), entity.getCurrency()));
//        dto.setMinPrice(ShopeeUtil.outputPrice(entity.getMinPrice(), entity.getCurrency()));
//        dto.setVWeight(ShopeeUtil.outputWeight(entity.getWeight()));

        if (entity.getPrice() != null && (entity.getPrice() != 0 || entity.getPrice() != ZERO)) {
            dto.setVPrice(ShopeeUtil.outputPrice(entity.getPrice(), entity.getCurrency()));
        }
        if (entity.getMaxPrice() != null && (entity.getMaxPrice() != 0 || entity.getMaxPrice() != ZERO)) {
            dto.setMaxPrice(ShopeeUtil.outputPrice(entity.getMaxPrice(), entity.getCurrency()));
        }
        if (entity.getMinPrice() != null && (entity.getMinPrice() != 0 || entity.getMinPrice() != ZERO)) {
            dto.setMinPrice(ShopeeUtil.outputPrice(entity.getMinPrice(), entity.getCurrency()));
        }

        if (entity.getWeight() != null) {
            dto.setVWeight(ShopeeUtil.outputWeight(entity.getWeight()));
        }

        if (!StrUtil.isBlank(entity.getImages())) {
            dto.setImages(JSON.parseArray(entity.getImages(), String.class).stream().filter(StrUtil::isNotBlank).collect(toList()));
        }
        if (!StrUtil.isBlank(entity.getLogistics())) {
            dto.setLogistics(JSON.parseArray(entity.getLogistics(), ShopeeProductDTO.Logistic.class));
        }

        return dto;
    }

    @Override
    public List<ShopeeProduct> toEntity(List<ShopeeProductDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        List<ShopeeProduct> list = new ArrayList<ShopeeProduct>(dtoList.size());
        for (ShopeeProductDTO shopeeProductDTO : dtoList) {
            list.add(toEntity(shopeeProductDTO));
        }

        return list;
    }

    @Override
    public List<ShopeeProductDTO> toDto(List<ShopeeProduct> entityList) {
        if (entityList == null) {
            return null;
        }

        List<ShopeeProductDTO> list = new ArrayList<ShopeeProductDTO>(entityList.size());
        for (ShopeeProduct shopeeProduct : entityList) {
            list.add(toDto(shopeeProduct));
        }

        return list;
    }

    private List<String> limitImagesLength(List<String> images) {
        int length = 0;
        final List<String> limit = new ArrayList<>();
        for (String image : images) {
            length += image.length();
            if (length > 3000) {
                break;
            }
            limit.add(image);
        }
        return limit;
    }


    @Override
    public List<ShopeeProductDTO> toUrlDTO(List<ShopeeProduct> entityList) {
        if (entityList == null) {
            return null;
        }
        List<ShopeeProductDTO> collect = entityList.stream().map(entity -> {
            ShopeeProductDTO dto = new ShopeeProductDTO();
            dto.setShopeeItemId(entity.getShopeeItemId());
            dto.setSourceUrl(entity.getSourceUrl());
            dto.setCollectUrl(entity.getCollectUrl());
            dto.setOnlineUrl(entity.getOnlineUrl());
            return dto;
        }).collect(toList());
        return collect;
    }

    @Override
    public ShopeeProduct toEntityV2(ShopeeProductDTO dto) {
        if(Objects.isNull(dto)){
            return null;
        }
        return  null;
    }

    @Override
    public ShopeeProductDTO toDtoV2(ShopeeProduct entity) {
        if(Objects.isNull(entity)){
            return null;
        }
        ShopeeProductDTO dto = new ShopeeProductDTO();
        BeanUtils.copyProperties(entity,dto);
        // 兼容以前不是json 数据
        if (ShopeeProduct.Type.SHOP.code.equals(entity.getType())) {
            String error;
            final JSONObject jsonObject = FeatrueUtil.tryGetJsonObject(entity.getFeature());
            if (Objects.nonNull(jsonObject)) {
                error = jsonObject.getString("error");
                dto.setStartDateTime(jsonObject.getString("start"));
                dto.setEndDateTime(jsonObject.getString("end"));
            } else {
                error = entity.getFeature();
            }
            if ("success".equals(error)) {
                dto.setFailReason(null);
            }else {
                dto.setFailReason(messageSource.getMessage(error));
            }
        }
        dto.setNewStatus(messageSource.country(entity.getStatus()));
        dto.setNewShopeeItemStatus(messageSource.country(entity.getShopeeItemStatus()));

        final String currency = entity.getCurrency();
        if(Objects.nonNull(currency)){
            dto.setMinPrice(isPrice(entity.getMinPrice(),currency));
            dto.setMaxPrice(isPrice(entity.getMaxPrice(),currency));
            dto.setVPrice(isPrice(entity.getPrice(),currency));
        }

        if (entity.getWeight() != null) {
            dto.setVWeight(ShopeeUtil.outputWeight(entity.getWeight()));
        }

        if (StringUtils.isNotBlank(entity.getImages())) {
            dto.setImages(JSON.parseArray(entity.getImages(), String.class).stream().filter(StrUtil::isNotBlank).collect(toList()));
        }
        if (StringUtils.isNotBlank(entity.getLogistics())) {
            dto.setLogistics(JSON.parseArray(entity.getLogistics(), ShopeeProductDTO.Logistic.class));
        }
        return dto;
    }


    public  static Float  isPrice(Long price,String currency){
        if(Objects.nonNull(price) && price > 0){
            return  ShopeeUtil.outputPrice(price,currency);
        }
        return  null;
    }

    @Override
    public List<ShopeeProduct> toEntityV2(List<ShopeeProductDTO> dtoList) {
        if(Objects.isNull(dtoList)){
            return null;
        }
        return null;
    }

    @Override
    public List<ShopeeProductDTO> toDtoV2(List<ShopeeProduct> entityList) {
        if(CollectionUtils.isEmpty(entityList)){
            return Collections.emptyList();
        }
        final List<ShopeeProductDTO> shopeeProductDTO = new ArrayList<>();
            for (ShopeeProduct shopeeProduct : entityList) {
                shopeeProductDTO.add(toDtoV2(shopeeProduct));
            }
        return shopeeProductDTO;
    }
}
