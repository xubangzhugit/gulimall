package com.izhiliu.erp.service.item.mapper.list;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.ShopeeProductSkuService;
import com.izhiliu.erp.service.item.business.ShopeeProductMoveServiceImpl;
import com.izhiliu.erp.service.item.mapper.ProductListMapper_V2;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM_V2;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM_V21;
import com.izhiliu.erp.web.rest.item.vm.VariationMV_V2;
import com.izhiliu.uaa.feignclient.UaaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/19 15:20
 */
@Component
public class ProductListMapperImpl_V2 implements ProductListMapper_V2 {

    private static final Logger log = LoggerFactory.getLogger(ProductListMapperImpl_V2.class);

    @Resource
    private ShopeeProductService shopeeProductService;

    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;

    @Resource
    private UaaService uaaService;



    @Override
    public ShopeeProduct toEntity(ProductListVM_V2 dto) {
        return null;
    }


    @Autowired
    @Qualifier(value = "handleProductExceptionInfo")
    private HandleProductExceptionInfo messageSource;

    @Override
    public ProductListVM_V2 toDto(ShopeeProduct entity) {
        if (entity == null) {
            return null;
        }
         ProductListVM_V2 vo =new  ProductListVM_V2();
        transform(entity,vo);
        if (entity.getType().equals(ShopeeProduct.Type.PLATFORM.code)) {
//            vo.setChilds(toDto(shopeeProductService.childs$(entity.getId(), ShopeeProduct.Type.PLATFORM_NODE.code)));

            vo.setHasChild(shopeeProductService.childCount(entity.getId()));
            vo.setShops(shopeeProductService.getShops(entity.getId()));
        }
        if (entity.getType().equals(ShopeeProduct.Type.PLATFORM_NODE.code)) {
            vo.setShops(shopeeProductService.trackToTheShop(entity.getId()));
        }
        if (Objects.equals(ShopeeProduct.Type.SHOP.code,entity.getType())) {
               vo.setShopeeItemId(entity.getShopeeItemId());
               try {
                   vo.setShopName(uaaService.getShopInfo(entity.getShopId()).getBody().getShopName());
               } catch (Exception e) {
                   log.error("[通过UAA拿店铺名称错误]: {}", e.getMessage());
               }

               vo.setStatus(messageSource.doCountry(entity.getStatus()));
               vo.setShopeeItemStatus(messageSource.doCountry(entity.getShopeeItemStatus()));

            // 兼容以前不是json 数据
            if (ShopeeProduct.Type.SHOP.code.equals(entity.getType())) {
                String error;
                try {
                    final JSONObject jsonObject = JSONObject.parseObject(entity.getFeature());
                    error  = jsonObject.getString("error");

                } catch (Exception e) {
//                    e.printStackTrace();
                    error =entity.getFeature();
                }
                if ("success".equals(error)) {
                    vo.setFailReason(null);
                }else {
                    vo.setFailReason(messageSource.getMessage(error));
                }
            }
        }

        return vo;
    }

    private <T extends  ProductListVM_V2> T transform(ShopeeProduct entity,T vo) {
        try {
            vo.setId(entity.getId());
            vo.setName(entity.getName());
            vo.setCurrency(entity.getCurrency());
            vo.setCollectUrl(entity.getCollectUrl());
            vo.setStoreMove(Objects.equals(entity.getCollect(), ShopeeProductMoveServiceImpl.STORE_MOVE) ? true : false);
            vo.setCollect(entity.getCollect());
            vo.setSold(entity.getSold());
            vo.setType(entity.getType());
            vo.setWarning(entity.getWarning());
            vo.setPlatformId(entity.getPlatformId());
            vo.setPlatformNodeId(entity.getPlatformNodeId());
            vo.setGmtCreate(entity.getGmtCreate());
            vo.setGmtModified(entity.getGmtModified());
            vo.setVariationTier(entity.getVariationTier());
            vo.setShopId(entity.getShopId());
            vo.setSkuCode(entity.getSkuCode() == null ? "" : entity.getSkuCode());
            vo.setStock(entity.getStock() == null ? 0 : entity.getStock());
            vo.setOnlineUrl(entity.getOnlineUrl() == null ? "" : entity.getOnlineUrl());

            vo.setVPrice(ShopeeUtil.outputPrice(entity.getPrice(), entity.getCurrency()));
            vo.setMaxPrice(ShopeeUtil.outputPrice(entity.getMaxPrice(), entity.getCurrency()));
            vo.setMinPrice(ShopeeUtil.outputPrice(entity.getMinPrice(), entity.getCurrency()));
            vo.setVWeight(ShopeeUtil.outputWeight(entity.getWeight()));
            if (Objects.nonNull(vo.getVariations())) {
                if (!vo.getVariations().isEmpty()) {
                    vo.getVariations().stream().filter(variationMV_v2 -> {
                        if (Objects.nonNull(variationMV_v2.getOriginalPrice()) && Objects.nonNull(variationMV_v2.getPrice())) {
                            if (variationMV_v2.getOriginalPrice().equals(variationMV_v2.getPrice())) {
                                return true;
                            }
                        }
                        return false;
                    }).findFirst().ifPresent(variationMV_v2 -> vo.setDiscount(true));
                }
            }else{
//                log.info("getAllByCurrentUser {} ", JSON.toJSONString(entity));
            }

            if (!StrUtil.isBlank(entity.getImages())) {
                vo.setImages(JSON.parseArray(entity.getImages(), String.class));
            }
            return vo;
        } catch (Exception e) {
            e.printStackTrace();
        }
       return null;
    }

    @Override
    public List<ShopeeProduct> toEntity(List<ProductListVM_V2> dtoList) {
        return null;
    }

    @Override
    public List<ProductListVM_V2> toDto(List<ShopeeProduct> entityList) {
        if (entityList == null) {
            return null;
        }

        final List<ProductListVM_V2> vos = new ArrayList<>(entityList.size());
        for (ShopeeProduct entity : entityList) {
            vos.add(toDto(entity));
        }
        return vos;
    }

    @Override
    public List<ProductListVM_V2> toProductListV2(List<ShopeeProduct> entityList) {
        if (entityList == null) {
            return null;
        }

        final List<ProductListVM_V2> vos = new ArrayList<>(entityList.size());
        for (ShopeeProduct entity : entityList) {
            vos.add(toProductListV2(entity));
        }
        return vos;
    }

    @Override
    public ProductListVM_V2 toProductListV2(ShopeeProduct entity) {
        if (entity == null) {
            return null;
        }
        ProductListVM_V2 vo = new ProductListVM_V2();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setCurrency(entity.getCurrency());
        vo.setCollectUrl(entity.getCollectUrl());
        vo.setCollect(entity.getCollect());
        vo.setSold(entity.getSold());
        vo.setStock(entity.getStock() == null ? 0 : entity.getStock());
        vo.setSkuCode(entity.getSkuCode() == null ? "" : entity.getSkuCode());
        vo.setType(entity.getType());
        vo.setWarning(entity.getWarning());
        vo.setPlatformId(entity.getPlatformId());
        vo.setPlatformNodeId(entity.getPlatformNodeId());
        vo.setGmtCreate(entity.getGmtCreate());
        vo.setGmtModified(entity.getGmtModified());
        vo.setVariationTier(entity.getVariationTier());
        vo.setShopId(entity.getShopId());
        vo.setOnlineUrl(entity.getOnlineUrl() == null ? "" : entity.getOnlineUrl());

        //兼容老数据
        if (!StrUtil.isBlank(entity.getImages())) {
            vo.setImages(JSON.parseArray(entity.getImages(), String.class).stream().limit(1).collect(Collectors.toList()));
        }

        vo.setVPrice(ShopeeUtil.outputPrice(entity.getPrice(), entity.getCurrency()));
        vo.setMaxPrice(ShopeeUtil.outputPrice(entity.getMaxPrice(), entity.getCurrency()));
        vo.setMinPrice(ShopeeUtil.outputPrice(entity.getMinPrice(), entity.getCurrency()));
        vo.setVWeight(ShopeeUtil.outputWeight(entity.getWeight()));
        return vo;
    }
    @Override
    public List<ProductListVM_V21> toProductListV3(List<ShopeeProduct> entityList) {
        if (entityList == null) {
            return null;
        }
        final List<ProductListVM_V21> productListVM_v21 = new ArrayList<>();
        for (ShopeeProduct entity : entityList) {
            ProductListVM_V21 vo = new ProductListVM_V21();
            transform(entity, vo);
            if (Objects.equals(entity.getType(),(ShopeeProduct.Type.SHOP.code))) {
                vo.setShopeeItemId(entity.getShopeeItemId());
                vo.setNewStatus(messageSource.country(entity.getStatus()));
                vo.setNewShopeeItemStatus(messageSource.country(entity.getShopeeItemStatus()));
            }
            // 兼容以前不是json 数据
            if (Objects.equals(ShopeeProduct.Type.SHOP.code,entity.getType())) {
                String error;
                try {
                    final JSONObject jsonObject = JSONObject.parseObject(entity.getFeature());
                    error  = jsonObject.getString("error");
                    vo.setStartDateTime(jsonObject.getString("start"));
                    vo.setEndDateTime(jsonObject.getString("end"));
                } catch (Exception e) {
                    error =entity.getFeature();
                }
                if ("success".equals(error)) {
                    vo.setFailReason(null);
                }else {
                    vo.setFailReason(messageSource.getMessage(error));
                }
            }
            productListVM_v21.add(vo);
        }

        return productListVM_v21;
    }


    public final static <T extends ProductListVM_V2> void fillingDiscount(T product) {
        boolean isShopeeItem = Objects.nonNull(product.getShopeeItemId());
        //  判斷是否需要表明是否折扣
        if (isShopeeItem) {
            final List<VariationMV_V2> variations = product.getVariations();
            if (!CollectionUtils.isEmpty(variations)) {
                for (VariationMV_V2 variation : variations) {
                    if (!product.isDiscount()) {
                        product.setDiscount(isDisCount(variation.getPrice(), variation.getOriginalPrice(), 0.0f));
                        if(product.isDiscount()){
                            return;
                        }
                    }
                }
            } else {
                if (!product.isDiscount()) {
                    product.setDiscount(isDisCount(product.getPrice(), product.getOriginalPrice(), 0L));
                }
            }
        }

    }



    public final static boolean isDisCount(Number price, Number originalPrice,Number defaultValue) {
        return !Objects.isNull(originalPrice) && !defaultValue.equals(originalPrice) && !price.equals(originalPrice);
    }




}
