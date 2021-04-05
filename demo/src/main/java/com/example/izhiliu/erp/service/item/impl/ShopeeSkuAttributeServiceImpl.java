package com.izhiliu.erp.service.item.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.erp.domain.item.ShopeeSkuAttribute;
import com.izhiliu.erp.repository.item.ShopeeSkuAttributeRepository;
import com.izhiliu.erp.service.item.ShopeeSkuAttributeService;
import com.izhiliu.erp.service.item.dto.ShopeeSkuAttributeDTO;
import com.izhiliu.erp.service.item.mapper.ShopeeSkuAttributeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing ShopeeSkuAttribute.
 */
@Service
public class ShopeeSkuAttributeServiceImpl extends IBaseServiceImpl<ShopeeSkuAttribute, ShopeeSkuAttributeDTO, ShopeeSkuAttributeRepository, ShopeeSkuAttributeMapper> implements ShopeeSkuAttributeService {

    private final Logger log = LoggerFactory.getLogger(ShopeeSkuAttributeServiceImpl.class);

    @Override
    public IPage<ShopeeSkuAttributeDTO> pageByProduct(long productId, Page pageable) {
        return toDTO(repository.pageByProductId(pageable, productId));
    }


    @Override
    public void copyShopeeSkuAttribute(long productId, long copyProductId) {
        deleteByProduct(copyProductId);

        /*
         * 取出源商品的SKU属性, 更换商品ID后保存
         */
        final List<ShopeeSkuAttributeDTO> skuAttributes = pageByProduct(productId, new Page(0, Integer.MAX_VALUE)).getRecords().stream()
            .peek(e -> e.setProductId(copyProductId)).collect(Collectors.toList());

        log.info("[SKU属性] :{}", skuAttributes);
        batchSave(skuAttributes);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<ShopeeSkuAttributeDTO> selectByProductIds(List<Long> productIds) {
        if(productIds.isEmpty()){
            return new ArrayList<>();
        }
        return mapper.toDto(repository.selectByProductIds(productIds));
    }

    @Override
    public int deleteByProduct(long productId) {
        return repository.delete(new QueryWrapper<>(new ShopeeSkuAttribute().setProductId(productId)));
    }
}
