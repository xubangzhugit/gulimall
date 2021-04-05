package com.izhiliu.erp.service.item.impl;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.core.common.constant.PlatformNodeEnum;
import com.izhiliu.core.config.security.SecurityInfo;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.log.LoggerOp;
import com.izhiliu.erp.service.item.BatchEditProductService;
import com.izhiliu.erp.service.item.ShopeeProductDescService;
import com.izhiliu.erp.service.item.ShopeeProductMediaService;
import com.izhiliu.erp.service.item.business.BusinessShopeeProductSkuService;
import com.izhiliu.erp.service.item.dto.*;
import com.izhiliu.erp.service.item.mapper.impl.BatchEditProductMapperImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/9 13:45
 */
@Service
@Slf4j
public class BatchEditProductServiceImpl extends ShopeeProductServiceImpl implements BatchEditProductService {

    @Resource
    private BatchEditProductMapperImpl batchEditProductMapper;

    @Resource
    private ShopeeProductMediaService shopeeProductMediaService;
    @Resource
    private BusinessShopeeProductSkuService businessShopeeProductSkuService;

    @Resource
    private ShopeeProductDescService shopeeProductDescService;

    @Override
    public List<BatchEditProductDTO> getProducts(List<Long> productIds) {

        List<ShopeeProductDTO> shopeeProductDTOS = productIds.stream().map(this::find).map(Optional::get).collect(Collectors.toList());
        return shopeeProductDTOS.parallelStream().map(productDTO -> {
            BatchEditProductDTO batchEditProductDTO = new BatchEditProductDTO();
            BeanUtils.copyProperties(productDTO, batchEditProductDTO);
            return batchEditProductDTO;
        }).peek(batchEditProductDTO -> {
            batchEditProductDTO.setVariationWrapper(shopeeProductSkuService.variationByProduct(batchEditProductDTO.getId()));
            batchEditProductDTO.setAttributeValues(shopeeProductAttributeValueService.selectByProduct(batchEditProductDTO.getId()));
        }).collect(Collectors.toList());
    }
    @Override
    public List<BatchEditProductDTO> getProductsV2(List<Long> productIds) {

        List<ShopeeProductDTO> shopeeProductDTOS = productIds.stream().map(this::find).map(Optional::get).collect(Collectors.toList());
        return shopeeProductDTOS.parallelStream().map(productDTO -> {
            BatchEditProductDTO batchEditProductDTO = new BatchEditProductDTO();
            BeanUtils.copyProperties(productDTO, batchEditProductDTO);
            return batchEditProductDTO;
        }).peek(batchEditProductDTO -> {
            batchEditProductDTO.setVariationWrapper(businessShopeeProductSkuService.variationByProduct(batchEditProductDTO.getId(), false));
        batchEditProductDTO.setAttributeValues(shopeeProductAttributeValueService.selectByProduct(batchEditProductDTO.getId()));
    }).collect(Collectors.toList());
    }

    @Override
    public void categoryAndAttribute(List<BatchEditProductDTO> products) {
        final LoggerOp loggerOpObejct = getLoggerOpObejct();
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        loggerOpObejct.setLoginId(securityInfo.getCurrentLogin());

        try {
            log.info(loggerOpObejct.setMessage("categoryAndAttribute").toString());
            super.batchUpdate(products.parallelStream()
                    .map(this::fillCategoryId)
                    .collect(Collectors.toList()));

            products.forEach(e -> shopeeProductAttributeValueService.deleteByProduct(e.getId()));

            shopeeProductAttributeValueService.batchSaveOrUpdate(products.parallelStream()
                    .peek(e -> e.getAttributeValues().forEach(a -> a.setProductId(e.getId())))
                    .map(BatchEditProductDTO::getAttributeValues)
                    .flatMap(Collection::stream)
                    .map(this::fillAttributeValue)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error(loggerOpObejct.error().setMessage(" put categoryAndAttribute error  param "+ JSONObject.toJSONString(products)).toString(),e );
            throw  e;
        }
        log.info(loggerOpObejct.ok().setMessage("categoryAndAttribute").toString());
    }

    /**
     *   todo  漏洞没做校验 然后可以动态的 修改 对应的 参数
     * @param products
     */
    @Override
    public void basicInfo(List<BatchEditProductDTO> products) {
        final LoggerOp loggerOpObejct = getLoggerOpObejct();
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        loggerOpObejct.setLoginId(securityInfo.getCurrentLogin());
        try {
            log.info(loggerOpObejct.setMessage("basicInfo").toString());
            batchUpdate(mapper.toDto(products.parallelStream()
                    .peek(dto -> {
                        dto.setCurrency(null);
                        dto.setImages(dto.getImages().stream().map(image -> {
                            if (image.startsWith("//")) {
                                return "https:" + image;
                            }
                            return image;
                        }).collect(Collectors.toList()));
                    })
                    .map(mapper::toEntity)
                    .collect(Collectors.toList())));

            //将media 和 desc 分开存储
            shopeeProductMediaService.batchSaveOrUpdateAndCleanCache(products.parallelStream().map(dto -> {
                ShopeeProductMediaDTO shopeeProductMediaDTO = shopeeProductMediaService.selectByProductId(dto.getId());
                if (null != shopeeProductMediaDTO) {
                    shopeeProductMediaDTO.setGmtModified(Instant.now());
                } else {
                    shopeeProductMediaDTO = new ShopeeProductMediaDTO();
                    shopeeProductMediaDTO.setProductId(dto.getId());
                }
                shopeeProductMediaDTO.setImages(dto.getImages());
                shopeeProductMediaDTO.setSizeChart(dto.getSizeChart());
                return shopeeProductMediaDTO;
            }).collect(Collectors.toList()));
            shopeeProductDescService.batchSaveOrUpdateAndCleanCache(products.parallelStream().map(dto -> {
                ShopeeProductDescDTO shopeeProductDescDTO = shopeeProductDescService.selectByProductId(dto.getId());
                if (null != shopeeProductDescDTO) {
                    shopeeProductDescDTO.setGmtModified(Instant.now());
                } else {
                    shopeeProductDescDTO = new ShopeeProductDescDTO();
                    shopeeProductDescDTO.setProductId(dto.getId());
                }
                shopeeProductDescDTO.setDescription(dto.getDescription());
                return shopeeProductDescDTO;
            }).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error(loggerOpObejct.error().setMessage(" put basicInfo error  param "+JSONObject.toJSONString(products)).toString(),e );
            throw  e;
        }
        log.info(loggerOpObejct.ok().setMessage("basicInfo").toString());
    }


    @Override
    public void priceAndStock(List<BatchEditProductDTO> products) {
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        products.parallelStream()
                .peek(e -> e.getVariationWrapper().setProductId(e.getId()))
                .peek(e -> e.setCurrency(null))
                .map(BatchEditProductDTO::getVariationWrapper)
                .forEach(param -> businessShopeeProductSkuService.coverTheProduct(param,securityInfo.getCurrentLogin() ));
    }

    @Override
    public void priceAndStockForBatch(List<BatchEditProductDTO> products) {
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        products.parallelStream()
                .peek(e -> e.getVariationWrapper().setProductId(e.getId()))
                .peek(e -> e.setCurrency(null))
                .map(BatchEditProductDTO::getVariationWrapper)
                .forEach(param -> businessShopeeProductSkuService.coverTheProductForBatch(param,securityInfo.getCurrentLogin() ));
    }

    /**
     *   todo   未作权限校验 极度危险  那位兄弟记得有空加下
     * @param products
     */
    @Override
    public void logisticInfo(List<BatchEditProductDTO> products) {
        //只更新物流信息
        final List<ShopeeProductDTO> entityList = batchEditProductMapper.toShopeeProductDTO(products);
        List<ShopeeProductDTO> shopeeProductDTOS =  this.quertLogisticInfoByproductId(entityList.stream().map(ShopeeProductDTO::getId).collect(Collectors.toList()));
        final Iterator<ShopeeProductDTO> iterator = shopeeProductDTOS.iterator();
        while (iterator.hasNext()) {
            final ShopeeProductDTO next = iterator.next();
            entityList.forEach(shopeeProductDTO -> {
                //  兼容 虾皮台湾站点 必须设置为 3 天
                if(Objects.equals(shopeeProductDTO.getId(),next.getId())){
                    iterator.remove();
                }
            });
        }
        batchUpdate(entityList);
    }

    private  List<ShopeeProductDTO> quertLogisticInfoByproductId(List<Long> collect) {
              if(CollectionUtils.isEmpty(collect)){
                  return  Collections.emptyList();
              }
        return   mapper.toDtoV2(this.getRepository().selectLogisticInfo(collect)) ;
    }

    private ShopeeProductAttributeValueDTO fillAttributeValue(ShopeeProductAttributeValueDTO e) {
        return new ShopeeProductAttributeValueDTO()
                .setProductId(e.getProductId())
                .setId(e.getId())
                .setAttributeId(e.getAttributeId())
                .setShopeeAttributeId(e.getShopeeAttributeId())
                .setValue(e.getValue());
    }

    private ShopeeProductDTO fillCategoryId(BatchEditProductDTO e) {
        ShopeeProductDTO product = new ShopeeProductDTO();
        product.setId(e.getId());
        product.setCategoryId(e.getCategoryId());
        shopeeCategoryService.find(e.getCategoryId()).ifPresent(v -> product.setShopeeCategoryId(v.getShopeeCategoryId()));
        return product;
    }
}
