package com.izhiliu.erp.service.item.mapper.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.ShopeeProductSkuService;
import com.izhiliu.erp.service.item.mapper.BatchEditProductMapperOld;
import com.izhiliu.erp.service.item.mapper.ShopeeProductMapper;
import com.izhiliu.erp.web.rest.item.vm.BatchEditProductVM;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/13 20:14
 */
@Component
public class BatchEditProductMapperImplOld implements BatchEditProductMapperOld {

    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;

    @Resource
    private ShopeeProductMapper shopeeProductMapper;

    @Override
    public ShopeeProduct toEntity(BatchEditProductVM vo) {
        return shopeeProductMapper.toEntity(vo);
    }

    @Override
    public BatchEditProductVM toDto(ShopeeProduct entity) {
        if (entity == null) {
            return null;
        }

        final BatchEditProductVM vo = new BatchEditProductVM();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setSkuCode(entity.getSkuCode());
        vo.setVariationTier(entity.getVariationTier());
        vo.setStock(entity.getStock());
        vo.setShopId(entity.getShopId());
        vo.setShopeeItemId(entity.getShopeeItemId());
        vo.setSendOutTime(entity.getSendOutTime());

        vo.setPlatformId(entity.getPlatformId());
        vo.setPlatformNodeId(entity.getPlatformNodeId());

        vo.setCurrency(entity.getCurrency());

        vo.setVPrice(ShopeeUtil.outputPrice(entity.getPrice(), entity.getCurrency()));
        vo.setVWeight(ShopeeUtil.outputWeight(entity.getWeight()));

        if (!StrUtil.isBlank(entity.getImages())) {
            vo.setImages(JSON.parseArray(entity.getImages(), String.class));
        }

        vo.setVariationWrapper(shopeeProductSkuService.variationByProduct(entity.getId()));
        return vo;
    }

    @Override
    public List<ShopeeProduct> toEntity(List<BatchEditProductVM> voList) {
        if (voList == null) {
            return null;
        }

        final List<ShopeeProduct> list = new ArrayList<>(voList.size());
        for (BatchEditProductVM vo : voList) {
            list.add(toEntity(vo));
        }

        return list;
    }

    @Override
    public List<BatchEditProductVM> toDto(List<ShopeeProduct> entityList) {
        if (entityList == null) {
            return null;
        }

        final List<BatchEditProductVM> list = new ArrayList<>(entityList.size());
        for (ShopeeProduct entity : entityList) {
            list.add(toDto(entity));
        }

        return list;
    }
}
