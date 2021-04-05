package com.izhiliu.erp.service.item.mapper.list;

import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.domain.item.ProductMetaData;
import com.izhiliu.erp.service.item.PlatformService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * describe: 商品源数据 to 列表项数据
 * <p>
 *
 * @author cheng
 * @date 2019/1/9 13:29
 */
@Component
public class ProductMetaDataListMapperImpl implements ProductMetaDataListMapper {

    @Resource
    private PlatformService platformService;

    @Resource
    private ShopeeProductService shopeeProductService;

    @Override
    public ProductMetaData toEntity(ProductListVM dto) {
        return null;
    }

    @Override
    public ProductListVM toDto(ProductMetaData entity) {
        if (entity == null) {
            return null;
        }

        final ProductListVM dto = new ProductListVM();
        dto.setMetaDateId(entity.getId());
        dto.setCollectUrl(entity.getUrl());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCurrency(entity.getCurrency());
        dto.setSold(entity.getSold());
        dto.setImages(entity.getImages());

        dto.setGmtCreate(entity.getCollectTime());
        dto.setLoginId(entity.getLoginId());

        dto.setPrice(ShopeeUtil.outputPrice(entity.getPrice(), entity.getCurrency()));

        final List<Long> platformIds = shopeeProductService.listByMetaData(entity.getId()).stream()
            .map(ShopeeProductDTO::getPlatformId)
            .collect(Collectors.toList());

        if (platformIds.size() != 0) dto.setPlatforms(platformService.list(platformIds));
        return dto;
    }

    @Override
    public List<ProductMetaData> toEntity(List<ProductListVM> dtoList) {
        return null;
    }

    @Override
    public List<ProductListVM> toDto(List<ProductMetaData> entityList) {
        if (entityList == null) {
            return null;
        }

        final List<ProductListVM> dots = new ArrayList<>(entityList.size());
        for (ProductMetaData entity : entityList) {
            dots.add(toDto(entity));
        }
        return dots;
    }
}
