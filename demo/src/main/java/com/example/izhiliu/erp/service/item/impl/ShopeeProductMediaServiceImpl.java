package com.izhiliu.erp.service.item.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.domain.item.ShopeeProductMedia;
import com.izhiliu.erp.domain.item.ShopeeProductSku;
import com.izhiliu.erp.repository.item.ShopeeProductMediaRepository;
import com.izhiliu.erp.service.item.ShopeeProductMediaService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.dto.*;
import com.izhiliu.erp.service.item.mapper.ShopeeProductMediaMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing ShopeeProductMedia.
 */
@Primary
@Service
public class ShopeeProductMediaServiceImpl extends IBaseServiceImpl<ShopeeProductMedia, ShopeeProductMediaDTO, ShopeeProductMediaRepository, ShopeeProductMediaMapper> implements ShopeeProductMediaService {

    private final static String CACHE_NAME = "ShopeeProductMediaService";

    private final Logger log = LoggerFactory.getLogger(ShopeeProductMediaServiceImpl.class);


    @Resource
    CacheManager cacheManager;


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Cacheable(value = CACHE_NAME, unless = "#result == null")
    public ShopeeProductMediaDTO selectByProductId(Long productId) {
        return mapper.toDto(repository.selectByProductId(productId));
    }


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ShopeeProductMediaDTO selectByProductIdNotCache(Long productId) {
        return mapper.toDto(repository.selectByProductId(productId));
    }

    @Override
    @Transactional(readOnly = true,propagation = Propagation.NOT_SUPPORTED)
    public List<ShopeeProductMediaDTO> selectMainImagsByProductId(List<Long> productIds) {
       if(productIds.isEmpty()){
           return  Collections.emptyList();
       }
        return mapper.toDto2(repository.selectImagsByProductIds(productIds), productIds.get(0));
    }

    @Override
    public void batchSaveOrUpdateAndCleanCache(List<ShopeeProductMediaDTO> shopeeProductMediaDTOList) {
        super.batchSaveOrUpdate(shopeeProductMediaDTOList);
        clearCacheBatch(shopeeProductMediaDTOList);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "#p0.productId")
    public ShopeeProductMediaDTO updateAndCleanCache(ShopeeProductMediaDTO shopeeProductMediaDTO) {
        if(super.update(shopeeProductMediaDTO)){
            return shopeeProductMediaDTO;
        }else {
            return null;
        }
    }

    @Resource
    ShopeeProductSkuServiceImpl shopeeProductSkuService;
    @Resource
    ShopeeSkuAttributeServiceImpl shopeeSkuAttributeService;
    @Resource
    ShopeeProductService shopeeProductService;



    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<ProductImageResult> selectProductImageBySkuCode(List<ProductImageDto> productImageDtos) {
        final List<ProductImageResult> ProductImageResults = productImageDtos.stream().flatMap(productImageDto -> {
                    final List<ShopeeProductSku> shopeeProductSkuStream = fillSkuImage(productImageDto);
                    return productImageResult(productImageDto, shopeeProductSkuStream).stream();
                }
        ).filter(Objects::nonNull)
                .collect(Collectors.toList());
        return ProductImageResults;
    }


    @Override
    public List<ShopeeProductMedia> selectByPriceRange(Integer page) {
        return repository.selectByPriceRange(page);
    }

    @Override
    public boolean deleteByProductId(Long productId) {

       return SqlHelper.delBool(repository.deleteByProductId(productId));
    }

    @Override
    public void updateByProductId(ShopeeProductMediaDTO dto) {
        repository.updateByProductId(mapper.toEntity(dto));
    }

    @Override
    public void updateByProductIds(List<ShopeeProductMediaDTO> shopeeProductMediaDTOList) {
        if (CommonUtils.isBlank(shopeeProductMediaDTOList)) {
            return;
        }
        repository.updateByProductIds(mapper.toEntity(shopeeProductMediaDTOList));
    }


    private List<ShopeeProductSku> fillSkuImage(ProductImageDto productImageDto) {
       Long productId = shopeeProductService.getProductIdByItemId(productImageDto.getItemId());
       if(Objects.nonNull(productId)){
           final IPage<ShopeeSkuAttributeDTO> shopeeSkuAttributeDTOIPage1 = shopeeSkuAttributeService.pageByProduct(productId, new Page(0, 1));
           if(Objects.nonNull(shopeeSkuAttributeDTOIPage1)){
               final List<ShopeeSkuAttributeDTO> records = shopeeSkuAttributeDTOIPage1.getRecords();
               if(!CollectionUtils.isEmpty(records)){
                   final ShopeeSkuAttributeDTO shopeeSkuAttributeDTOIPage = records.iterator().next();
                   final List<String> options = shopeeSkuAttributeDTOIPage.getImagesUrl();
                   final List<ShopeeProductSku> shopeeProductSkus = shopeeProductSkuService.selectImagesBySkuCodes(productImageDto.getSkuCode(), productId);
                   if(!CollectionUtils.isEmpty(options)){
                       final int size = options.size();
                       shopeeProductSkus.forEach(shopeeProductSku -> {
                           final Integer skuOptionOneIndex = shopeeProductSku.getSkuOptionOneIndex();
                           if(Objects.nonNull(skuOptionOneIndex) && size > skuOptionOneIndex){
                              shopeeProductSku.setImage(options.get(skuOptionOneIndex));
                          }
                       });
                   }
                   return  shopeeProductSkus;
               }
           }
       }
        return Collections.EMPTY_LIST;
    }

    private List<ProductImageResult> productImageResult(ProductImageDto productImageDto, List<ShopeeProductSku> shopeeProductSkus) {
       return   productImageDto.getSkuCode().stream().map(skuCode ->   productImageResult(skuCode,productImageDto,shopeeProductSkus)).collect(Collectors.toList());
    }

    private ProductImageResult productImageResult(String productSku, ProductImageDto productImageDto, final List<ShopeeProductSku> shopeeProductSkus) {
        if(shopeeProductSkus.isEmpty()) {return  null ;};
        for (ShopeeProductSku shopeeProductSku : shopeeProductSkus) {
            if (Objects.equals(productSku, shopeeProductSku.getSkuCode())) {
                String image = shopeeProductSku.getImage();
                return new ProductImageResult().setSkuCode(productSku).setItemId(productImageDto.getItemId()).setImage(image).setVariantId(productImageDto.getVariantId()).setVariantSkuId(productImageDto.getVariantSkuId());
            }
        }
        return null;
    }

    String getProductImage(ShopeeProduct shopeeProduct) {
        if (Objects.nonNull(shopeeProduct.getImages())) {
            return JSON.parseArray(shopeeProduct.getImages(), String.class).iterator().next();
        }
        final ShopeeProductMedia shopeeProductMedia = getRepository().selectImagsByProductId(shopeeProduct.getId());
        if (Objects.nonNull(shopeeProduct) && StringUtils.isNotBlank(shopeeProductMedia.getImages())) {
            return JSON.parseArray(shopeeProductMedia.getImages(), String.class).iterator().next();
        }
        return null;
    }

    private void clearCacheBatch(List<ShopeeProductMediaDTO> shopeeProductMediaDTOS) {
        shopeeProductMediaDTOS.forEach(shopeeProductMediaDTO -> {
            if (null != cacheManager.getCache(CACHE_NAME)) {
                cacheManager.getCache(CACHE_NAME).evict(shopeeProductMediaDTO.getProductId());
            }
        });
    }
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void clearCacheBatch(ShopeeProductMedia shopeeProductMediaDTO) {
            if (null != cacheManager.getCache(CACHE_NAME)) {
                cacheManager.getCache(CACHE_NAME).evict(shopeeProductMediaDTO.getProductId());
            }

    }
}
