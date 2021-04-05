package com.izhiliu.erp.service.item.mapper.list;

import com.alibaba.fastjson.JSON;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.ShopeeProductSkuService;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * describe: 店铺商品数据列表项
 * <p>
 *
 * @author cheng
 * @date 2019/1/9 14:32
 */
@Component
public class ShopProductListMapperImpl implements ShopProductListMapper {

    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;

    @Override
    public ShopeeProduct toEntity(ProductListVM dto) {
        return null;
    }

    @Override
    public ProductListVM toDto(ShopeeProduct entity) {
        if (entity == null) {
            return null;
        }

        /*
         * 填充基本信息
         */
        final ProductListVM dto = new ProductListVM();
        dto.setPlatformId(entity.getPlatformId());
        dto.setPlatformNodeId(entity.getPlatformNodeId());
        dto.setProductId(entity.getId());
        dto.setSourceUrl(entity.getSourceUrl());
        dto.setCollectUrl(entity.getCollectUrl());
        dto.setOnlineUrl(entity.getOnlineUrl());
        dto.setShopeeItemId(entity.getShopeeItemId());

        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCurrency(entity.getCurrency());
        dto.setStatus(entity.getStatus());
        dto.setShopeeItemStatus(entity.getShopeeItemStatus());

        dto.setImages(JSON.parseArray(entity.getImages(), String.class));

        dto.setPrice(ShopeeUtil.outputPrice(entity.getPrice(), entity.getCurrency()));
        dto.setMaxPrice(ShopeeUtil.outputPrice(entity.getMaxPrice(), entity.getCurrency()));
        dto.setMinPrice(ShopeeUtil.outputPrice(entity.getMinPrice(), entity.getCurrency()));

        dto.setShopId(entity.getShopId());
        dto.setGmtCreate(entity.getGmtCreate());
        dto.setGmtModified(entity.getGmtModified());

        if (entity.getType().equals(ShopeeProduct.Type.SHOP.code)) {
            if ("success".equals(entity.getFeature())) {
                dto.setFailReason(null);
            } else {
                dto.setFailReason(entity.getFeature());
            }
        }

        dto.setVariationWrapper(shopeeProductSkuService.variationByProduct(entity.getId()));
        return dto;
    }

    @Override
    public List<ShopeeProduct> toEntity(List<ProductListVM> dtoList) {
        return null;
    }

    @Override
    public List<ProductListVM> toDto(List<ShopeeProduct> entityList) {
        if (entityList == null) {
            return null;
        }

        final ArrayList<ProductListVM> dtos = new ArrayList<>(entityList.size());
        for (ShopeeProduct entity : entityList) {
            dtos.add(toDto(entity));
        }
        return dtos;
    }
}
