package com.izhiliu.erp.service.item.mapper.list;

import com.alibaba.fastjson.JSON;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/9 14:16
 */
@Component
public class PlatformProductListMapperImpl implements PlatformProductListMapper {

    @Resource
    private ShopeeProductService shopeeProductService;

    @Override
    public ProductListVM toDto(ShopeeProduct entity) {
        if (entity == null) {
            return null;
        }

        final ProductListVM dto = new ProductListVM();

        dto.setPlatformId(entity.getPlatformId());
        dto.setPlatformNodeId(entity.getPlatformNodeId());
        dto.setType(entity.getType());
        dto.setProductId(entity.getId());
        dto.setSourceUrl(entity.getSourceUrl());
        dto.setCollectUrl(entity.getCollectUrl());

        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCurrency(entity.getCurrency());
        dto.setSold(entity.getSold());

        dto.setLoginId(entity.getLoginId());
        dto.setGmtCreate(entity.getGmtCreate());
        dto.setGmtModified(entity.getGmtModified());

        dto.setImages(JSON.parseArray(entity.getImages(), String.class));
        dto.setPrice(ShopeeUtil.outputPrice(entity.getPrice(), entity.getCurrency()));

        dto.setMaxPrice(ShopeeUtil.outputPrice(entity.getMaxPrice(), entity.getCurrency()));
        dto.setMinPrice(ShopeeUtil.outputPrice(entity.getMinPrice(), entity.getCurrency()));

        dto.setShops(shopeeProductService.trackToTheShop(entity.getType(), entity.getId()));

        return dto;
    }

    @Override
    public List<ProductListVM> toDto(List<ShopeeProduct> entityList) {
        if (entityList == null) {
            return null;
        }

        final List<ProductListVM> dots = new ArrayList<>(entityList.size());
        for (ShopeeProduct entity : entityList) {
            dots.add(toDto(entity));
        }
        return dots;
    }

    @Override
    public ShopeeProduct toEntity(ProductListVM dto) {
        return null;
    }

    @Override
    public List<ShopeeProduct> toEntity(List<ProductListVM> dtoList) {
        return null;
    }
}
