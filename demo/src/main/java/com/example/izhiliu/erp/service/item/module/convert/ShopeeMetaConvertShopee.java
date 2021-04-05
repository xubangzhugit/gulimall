package com.izhiliu.erp.service.item.module.convert;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.dto.*;
import com.izhiliu.erp.service.item.module.basic.BaseModelConvert;
import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.util.FeatrueUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * describe: Shopee源数据 映射到本地存储
 * <p>
 *
 * @author cheng
 * @date 2019/1/11 21:23
 */
@Component
public class ShopeeMetaConvertShopee extends BaseModelConvert implements ApplicationRunner {

    String  key =  "kky:lux:shopee-collect-image-url-mapper-object";
    @Resource
    StringRedisTemplate stringRedisTemplate;

    /**
     * 把shopee采集模型数据映射成平台商品
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void map(ProductMetaDataDTO metaData, String loginId, MetaDataObject.CollectController collectController) {
        try {
            final JSONObject item = JSON.parseObject(metaData.getJson()).getJSONObject("item");
            final Long categoryId = saveCategory(item, metaData.getPlatformNodeId());

            final ShopeeProductDTO shopeeProductDTO = saveProduct(item, metaData, categoryId, loginId,collectController);

            saveAttribute(item, categoryId, shopeeProductDTO.getId());
            saveVariation(item, shopeeProductDTO,collectController);
        } catch (Exception e) {
            log.error("[虾皮转虾皮异常]", e);
        }
    }

    /**
     * 保存类目
     */
    private Long saveCategory(JSONObject item, long platformNodeId) {
        final JSONArray categories = item.getJSONArray("categories");

        // 类目
        Long parentId = 0L;
        for (int i = 0; i < categories.size(); i++) {
            final JSONObject category = categories.getJSONObject(i);
            final String categoryName = category.getString("display_name");
            final Long shopeeCategoryId = category.getLong("catid");
            final Long shopeeParentId = category.getLong("parent_id");

            if (null == category.getBoolean("no_sub")) {
                return 0L;
            }

            final boolean hasChild = !category.getBoolean("no_sub");

            /*
             * 类目已存在(根据ShopeeCategoryId) 读出 localSystemId 用于子集关联
             */
            final ShopeeCategoryDTO shopeeCategoryDTO;
            final Optional<ShopeeCategoryDTO> categoryExist = shopeeCategoryService.findByPlatformNodeAndShopeeCategory(platformNodeId, shopeeCategoryId);
            if (categoryExist.isPresent()) {
                parentId = categoryExist.get().getId();
            } else {
                shopeeCategoryDTO = new ShopeeCategoryDTO();
                shopeeCategoryDTO.setParentId(parentId);
                shopeeCategoryDTO.setPlatformNodeId(platformNodeId);
                shopeeCategoryDTO.setName(categoryName);
                shopeeCategoryDTO.setTier(i + 1);
                shopeeCategoryDTO.setShopeeCategoryId(shopeeCategoryId);
                shopeeCategoryDTO.setShopeeParentId(shopeeParentId);
                shopeeCategoryDTO.setHasChild(hasChild ? 1 : 0);

                parentId = shopeeCategoryService.save(shopeeCategoryDTO).getId();
            }
        }
        return parentId;
    }

    /**
     * 保存商品
     */
    private ShopeeProductDTO saveProduct(JSONObject item, ProductMetaDataDTO data, long categoryId, String loginId, MetaDataObject.CollectController collectController) {
        final ShopeeProductDTO product = new ShopeeProductDTO();
        product.setType(ShopeeProduct.Type.PLATFORM.code);
        product.setPlatformId(PlatformEnum.SHOPEE.getCode().longValue());

        product.setOldPlatformNodeId(ShopeeUtil.nodeId(data.getSuffix()));
        product.setOldPlatformId(ShopeeUtil.platformId(product.getOldPlatformNodeId()));

        product.setLoginId(data.getLoginId());
        product.setMetaDataId(data.getId());

        product.setSkuCode(Objects.nonNull(data.getProductId())?data.getProductId().toString():null);
        product.setCollect(data.getPlatform());
        product.setCollectUrl(data.getUrl());

        product.setCategoryId(categoryId);
        product.setName(data.getName());
        product.setDescription(data.getDescription());
        //默认主图,兼容处理,取前9张
        if (null == data.getMainImages() || data.getMainImages().size() < 1) {
            if(null != data.getImages() && data.getImages().size()>9){
                product.setImages(data.getImages().subList(0,9));
            }else {
                product.setImages(data.getImages());
            }
        } else {
            product.setImages(data.getMainImages());
        }

        product.setStock(CommonUtils.getMaxStock(collectController.isCollectStock() ? item.getInteger("stock") : collectController.getStock()));
        product.setSold(data.getSold());
        if(Objects.nonNull(collectController.getPricing())){
            product.setFeature(FeatrueUtil.addFeature(null,"pricingId",collectController.getPricing().toString()));
        }
        product.setPrice(data.getPrice());
        product.setCurrency(data.getCurrency());

        return shopeeProductService.collectSave(product);
    }

    /**
     * 保存属性
     *
     * @param item
     * @param categoryId
     * @param productId
     */
    private void saveAttribute(JSONObject item, long categoryId, long productId) {
        final JSONArray attributes = item.getJSONArray("attributes");

        for (int i = 0; i < attributes.size(); i++) {
            final JSONObject attribute = attributes.getJSONObject(i);
            final Long shopeeAttributeId = attribute.getLong("id");
            final String attributeName = attribute.getString("name");
            final String attributeValue = attribute.getString("value");

            if (shopeeAttributeId == null || StrUtil.isBlank(attributeName)) {
                continue;
            }
            // 绑定属性-产品
            final ShopeeProductAttributeValueDTO shopAttributeValue = new ShopeeProductAttributeValueDTO();
            shopAttributeValue.setProductId(productId);
            shopAttributeValue.setName(attributeName);
            shopAttributeValue.setValue(attributeValue);
            shopAttributeValue.setShopeeAttributeId(shopeeAttributeId);

            shopeeProductAttributeValueService.save(shopAttributeValue);
        }
    }

    /**
     * 保存变体
     */
    private void saveVariation(JSONObject item, ShopeeProductDTO product, MetaDataObject.CollectController collectController) {
        /*
         * 单品
         * 单SKU
         *  |-- 有 tier_variations
         *  |-- 无 tier_variations
         * 双SKU
         */
        final boolean collectStock = collectController.isCollectStock();
        final boolean collectDiscount = collectController.isCollectDiscount();
        final int defaultStock = collectController.getStock();
        int variationTier;
        final JSONArray variations = item.getJSONArray("tier_variations");
        final JSONArray models = item.getJSONArray("models");
        if ((variations == null || variations.size() == 0) && (models == null || models.size() == 0)) {
            log.info("[Variations] : 单品");

            variationTier = 0;
            final ShopeeProductSkuDTO shopeeProductSkuDTO = fillProductSkuOne(product);
            shopeeProductSkuService.save(shopeeProductSkuDTO);
        } else if (variations.size() == 2) {
            log.info("[Variations] : 双SKU");

            variationTier = 2;
            final ShopeeSkuAttributeDTO shopeeSkuAttributeDTO = saveSkuAttribute(product, variations, 0);
            final ShopeeSkuAttributeDTO shopeeSkuAttributeDTO1 = saveSkuAttribute(product, variations, 1);

            for (int i = 0; i < models.size(); i++) {
                final JSONObject model = models.getJSONObject(i);

                final Integer oneOptionIndex = model.getJSONObject("extinfo").getJSONArray("tier_index").getInteger(0);
                final Integer towOptionIndex = model.getJSONObject("extinfo").getJSONArray("tier_index").getInteger(1);

                final ShopeeProductSkuDTO shopeeProductSkuDTO = fillProductSkuTow(product, model,collectDiscount,collectStock,defaultStock);
                shopeeProductSkuDTO.setSkuOptionOneIndex(oneOptionIndex);
                shopeeProductSkuDTO.setSkuOptionTowIndex(towOptionIndex);
                shopeeProductSkuDTO.setSkuCode(ConvertUtils.generate(shopeeSkuAttributeDTO.getOptions().get(oneOptionIndex),shopeeSkuAttributeDTO1.getOptions().get(towOptionIndex)));
                shopeeProductSkuService.save(shopeeProductSkuDTO);
            }
        } else {
            /*
             * 台湾站的单SKU数据直接存放在 models 内, tier_variations 是空的
             * 印尼站则是放一个 tier_variations, 然后在 models 内用下标指向 tier_variations.options
             */
            variationTier = 1;
            if (variations.size() != 0) {
                log.info("[Variations] : 单SKU,Variations");

                final ShopeeSkuAttributeDTO shopeeSkuAttributeDTO = saveSkuAttribute(product, variations, 0);

                for (int i = 0; i < models.size(); i++) {
                    final JSONObject model = models.getJSONObject(i);
                    final Integer oneOptionIndex = model.getJSONObject("extinfo").getJSONArray("tier_index").getInteger(0);

                    final ShopeeProductSkuDTO shopeeProductSkuDTO = fillProductSkuTow(product, model, collectDiscount, collectStock, defaultStock);
                    shopeeProductSkuDTO.setSkuOptionOneIndex(oneOptionIndex);
                    shopeeProductSkuDTO.setSkuCode(ConvertUtils.generate(shopeeSkuAttributeDTO.getOptions().get(oneOptionIndex)));
                    shopeeProductSkuService.save(shopeeProductSkuDTO);
                }
            } else {
                log.info("[Variations] : 单SKU,Models");

                /*
                 * 抽取 options
                 * 生成 ShopeeProductSkuDTO
                 */
                final List<String> options = new ArrayList<>(models.size());
                final ArrayList<ShopeeProductSkuDTO> skus = new ArrayList<>(models.size());
                for (int i = 0; i < models.size(); i++) {
                    final JSONObject model = models.getJSONObject(i);
                    options.add(model.getString("name"));
                    skus.add(fillProductSkuTow(product, model, collectDiscount, collectStock, defaultStock));
                }

                final ShopeeSkuAttributeDTO shopeeSkuAttributeDTO = new ShopeeSkuAttributeDTO();
                shopeeSkuAttributeDTO.setProductId(product.getId());
                shopeeSkuAttributeDTO.setOptions(options);
                shopeeSkuAttributeDTO.setName("Variation");
                shopeeSkuAttributeService.save(shopeeSkuAttributeDTO).getId();

                /*
                 * 保存 SKU
                 */
                for (int i = 0; i < skus.size(); i++) {
                    final ShopeeProductSkuDTO shopeeProductSkuDTO = skus.get(i);
                    shopeeProductSkuDTO.setSkuOptionOneIndex(i);
                    shopeeProductSkuService.save(shopeeProductSkuDTO);
                }
            }
        }

        // 更新层级
        final ShopeeProductDTO shopeeProductDTO = new ShopeeProductDTO();
        shopeeProductDTO.setId(product.getId());
        shopeeProductDTO.setVariationTier(variationTier);
        shopeeProductService.update(shopeeProductDTO);
    }

    /**
     * 保存SKU属性
     */
    private ShopeeSkuAttributeDTO saveSkuAttribute(ShopeeProductDTO product, JSONArray variations, int index) {
        final String imageUrlPrefix = String.valueOf(Objects.requireNonNull(stringRedisTemplate.opsForHash().get(key, product.getOldPlatformNodeId().toString())));
        final JSONObject variation = variations.getJSONObject(index);
        final String name = variation.getString("name");
        final String options = variation.getString("options");

        final ShopeeSkuAttributeDTO shopeeSkuAttributeDTO = new ShopeeSkuAttributeDTO();
        shopeeSkuAttributeDTO.setProductId(product.getId());
        shopeeSkuAttributeDTO.setName(name);
        final JSONArray images = variation.getJSONArray("images");
        if(StringUtils.isNotBlank(imageUrlPrefix)&& !CollectionUtils.isEmpty(images)){
            final List<String> imageUrls = images.stream().limit(35)
                    .map(imageUrl -> imageUrlPrefix+imageUrl).collect(Collectors.toList());
            shopeeSkuAttributeDTO.setImagesUrl(imageUrls);
        }
        shopeeSkuAttributeDTO.setOptions(JSON.parseArray(options, String.class));
        shopeeSkuAttributeService.save(shopeeSkuAttributeDTO);
        return shopeeSkuAttributeDTO;
    }

    /**
     * 填充部分信息
     */
    private ShopeeProductSkuDTO fillProductSkuOne(ShopeeProductDTO product) {
        final ShopeeProductSkuDTO dto = new ShopeeProductSkuDTO();
        dto.setProductId(product.getId());
        dto.setPrice(ShopeeUtil.outputPrice(product.getPrice(),product.getCurrency()));
        dto.setCurrency(product.getCurrency());
        dto.setStock(product.getStock());
        return dto;
    }

    /**
     * 填充部分属性
     */
    private ShopeeProductSkuDTO fillProductSkuTow(ShopeeProductDTO product, JSONObject model, boolean collectDiscount, boolean collectStock, int defaultStock) {
        final Long price = collectDiscount ? model.getLong("price"):model.getLong("price_before_discount");
        final String currency = model.getString("currency");
        final Integer stock =collectStock? model.getInteger("stock"):defaultStock;


        final ShopeeProductSkuDTO shopeeProductSkuDTO = new ShopeeProductSkuDTO();
        shopeeProductSkuDTO.setProductId(product.getId());
        /*
         * 价格转换 => 采集到平台
         */
        shopeeProductSkuDTO.setCollectPrice(ShopeeUtil.collectInputPrice(price, currency));
        shopeeProductSkuDTO.setCurrency(currency);
        shopeeProductSkuDTO.setStock(stock);

//        shopeeProductSkuDTO.setSkuCode(model.getString("modelid"));
        shopeeProductSkuDTO.setSold(model.getInteger("sold"));

        return shopeeProductSkuDTO;
    }




    @Override
    public void run(ApplicationArguments args) throws Exception {
        final HashOperations<String, Object, Object> stringObjectObjectHashOperations = stringRedisTemplate.opsForHash();
        stringObjectObjectHashOperations.putIfAbsent(key,"1","https://cf.shopee.com.my/file/");
        stringObjectObjectHashOperations.putIfAbsent(key,"2","https://cf.shopee.sg/file/");
        stringObjectObjectHashOperations.putIfAbsent(key,"3","https://cf.shopee.co.id/file/");
        stringObjectObjectHashOperations.putIfAbsent(key,"4","https://s-cf-tw.shopeesz.com/file/");
        stringObjectObjectHashOperations.putIfAbsent(key,"5","https://cf.shopee.co.th/file/");
        stringObjectObjectHashOperations.putIfAbsent(key,"6","https://cf.shopee.vn/file/");
        stringObjectObjectHashOperations.putIfAbsent(key,"7","https://cf.shopee.ph/file/");
        stringObjectObjectHashOperations.putIfAbsent(key,"8","https://cf.shopee.com.br/file/");
        stringObjectObjectHashOperations.putIfAbsent(key,"9","https://cf.shopee.com.mx/file/");
    }
}
