package com.izhiliu.erp.service.item.mapper.impl;

import com.alibaba.fastjson.JSON;
import com.izhiliu.erp.domain.item.ShopeeSkuAttribute;
import com.izhiliu.erp.service.item.dto.ShopeeSkuAttributeDTO;
import com.izhiliu.erp.service.item.mapper.ShopeeSkuAttributeMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ShopeeSkuAttributeMapperImpl implements ShopeeSkuAttributeMapper {

    @Override
    public ShopeeSkuAttribute toEntity(ShopeeSkuAttributeDTO dto) {
        if (dto == null) {
            return null;
        }

        ShopeeSkuAttribute shopeeSkuAttribute = new ShopeeSkuAttribute();

        shopeeSkuAttribute.setId(dto.getId());
        shopeeSkuAttribute.setFeature(dto.getFeature());
        shopeeSkuAttribute.setGmtCreate(dto.getGmtCreate());
        shopeeSkuAttribute.setGmtModified(dto.getGmtModified());
        shopeeSkuAttribute.setName(dto.getName());
        shopeeSkuAttribute.setNameChinese(dto.getNameChinese());
        shopeeSkuAttribute.setOptions(JSON.toJSONString(dto.getOptions()));
        shopeeSkuAttribute.setOptionsChinese(JSON.toJSONString(dto.getOptionsChinese()));
        if(null !=dto.getImagesUrl() && !dto.getImagesUrl().isEmpty() && null != dto.getImagesUrl().get(0)){
            shopeeSkuAttribute.setImagesUrl(JSON.toJSONString(dto.getImagesUrl()));
        }
        shopeeSkuAttribute.setProductId(dto.getProductId());

        return shopeeSkuAttribute;
    }

    @Override
    public ShopeeSkuAttributeDTO toDto(ShopeeSkuAttribute entity) {
        if (entity == null) {
            return null;
        }

        ShopeeSkuAttributeDTO shopeeSkuAttributeDTO = new ShopeeSkuAttributeDTO();

        shopeeSkuAttributeDTO.setId(entity.getId());
        shopeeSkuAttributeDTO.setName(entity.getName());
        shopeeSkuAttributeDTO.setNameChinese(entity.getNameChinese());
        shopeeSkuAttributeDTO.setOptions(JSON.parseArray(entity.getOptions(), String.class));
        shopeeSkuAttributeDTO.setOptionsChinese(JSON.parseArray(entity.getOptionsChinese(), String.class));
        shopeeSkuAttributeDTO.setImagesUrl(JSON.parseArray(entity.getImagesUrl(), String.class));
        shopeeSkuAttributeDTO.setGmtCreate(entity.getGmtCreate());
        shopeeSkuAttributeDTO.setGmtModified(entity.getGmtModified());
        shopeeSkuAttributeDTO.setFeature(entity.getFeature());
        shopeeSkuAttributeDTO.setProductId(entity.getProductId());

        return shopeeSkuAttributeDTO;
    }

    @Override
    public List<ShopeeSkuAttribute> toEntity(List<ShopeeSkuAttributeDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        List<ShopeeSkuAttribute> list = new ArrayList<ShopeeSkuAttribute>(dtoList.size());
        for (ShopeeSkuAttributeDTO shopeeSkuAttributeDTO : dtoList) {
            list.add(toEntity(shopeeSkuAttributeDTO));
        }

        return list;
    }

    @Override
    public List<ShopeeSkuAttributeDTO> toDto(List<ShopeeSkuAttribute> entityList) {
        if (entityList == null) {
            return null;
        }

        List<ShopeeSkuAttributeDTO> list = new ArrayList<ShopeeSkuAttributeDTO>(entityList.size());
        for (ShopeeSkuAttribute shopeeSkuAttribute : entityList) {
            list.add(toDto(shopeeSkuAttribute));
        }

        return list;
    }
}
