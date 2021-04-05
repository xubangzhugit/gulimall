package com.izhiliu.erp.service.item.mapper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.ShopeeProductMedia;
import com.izhiliu.erp.service.item.dto.ShopeeProductMediaDTO;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.PriceRange;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapper for the entity ShopeeProductMedia and its DTO ShopeeProductMediaDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface ShopeeProductMediaMapper extends EntityMapper<ShopeeProductMediaDTO, ShopeeProductMedia> {


    default List<String> mapImages(String images) {
        if (null == images ){
            return new ArrayList<>();
        }
        return JSON.parseArray(images).toJavaList(String.class);
    }
    default String mapImages(List<String> images) {
        if(null == images || images.isEmpty()){
            return null;
        }
        return JSON.toJSONString(images);
    }

    default List<PriceRange> mapPriceRange(String images) {
        if (null == images ){
            return new ArrayList<>();
        }
        return JSON.parseArray(images).toJavaList(PriceRange.class);
    }

    default String mapPriceRange(List<PriceRange> images) {
        if(null == images){
            return null;
        }
        if(images.isEmpty()){
            return  "[]";
        }
        return JSON.toJSONString(images);
    }


    default ShopeeProductMediaDTO toDto2(ShopeeProductMedia media,Long productId){
        if ( Objects.isNull(media)) {
            return null;
        }
        if(StringUtils.isNotBlank(media.getImages())){
            ShopeeProductMediaDTO shopeeProductMediaDTO = new ShopeeProductMediaDTO();
            shopeeProductMediaDTO.setImages(JSON.parseArray(media.getImages(),String.class).stream().limit(1).collect(Collectors.toList()));
            shopeeProductMediaDTO.setProductId(media.getProductId());
            shopeeProductMediaDTO.setIsCondition(media.getIsCondition());
            return shopeeProductMediaDTO;
        }
        return null;
    }
    default List<ShopeeProductMediaDTO> toDto2(List<ShopeeProductMedia> medias,Long productId){
        if (Objects.isNull(medias)||medias.isEmpty()) {
            return Collections.emptyList();
        }
        return medias.stream().map(shopeeProductMedia -> toDto2(shopeeProductMedia,productId)).filter(Objects::nonNull).collect(Collectors.toList());
    }


}