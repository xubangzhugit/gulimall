package com.izhiliu.erp.service.item.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Sequence;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.izhiliu.core.common.ValidList;
import com.izhiliu.core.common.constant.CurrencyEnum;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.core.common.constant.PlatformNodeEnum;
import com.izhiliu.core.common.constant.ShopeeConstant;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.core.config.security.SecurityInfo;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.config.subscribe.Enum.SubLimitProductConstant;
import com.izhiliu.core.config.subscribe.SubLimitAnnotation;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.common.ShopInfoRedisUtils;
import com.izhiliu.erp.common.TaskExecutorUtils;
import com.izhiliu.erp.config.ApplicationProperties;
import com.izhiliu.erp.config.aop.subscribe.SubLimitService;
import com.izhiliu.erp.config.module.currency.CurrencyRateApiImpl;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateResult;
import com.izhiliu.erp.config.strategy.ShopeePushContent;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.domain.item.BoostItem;
import com.izhiliu.erp.domain.item.ItemCategoryMap;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.log.LogConstant;
import com.izhiliu.erp.log.LoggerOp;
import com.izhiliu.erp.repository.item.*;
import com.izhiliu.erp.service.item.*;
import com.izhiliu.erp.service.item.business.BusinessShopeeProductSkuService;
import com.izhiliu.erp.service.item.cache.Publish;
import com.izhiliu.erp.service.item.dto.*;
import com.izhiliu.erp.service.item.mapper.BatchEditProductMapperOld;
import com.izhiliu.erp.service.item.mapper.ProductListMapper_V2;
import com.izhiliu.erp.service.item.mapper.ShopeeProductMapper;
import com.izhiliu.erp.service.item.mapper.list.PlatformProductListMapper;
import com.izhiliu.erp.service.item.mapper.list.ShopProductListMapper;
import com.izhiliu.erp.service.item.module.channel.ShopeeModelChannel;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductSKUAttrInfoPlus;
import com.izhiliu.erp.service.mq.producer.MQProducerService;
import com.izhiliu.erp.util.FeatrueUtil;
import com.izhiliu.erp.util.RedissonDistributedLocker;
import com.izhiliu.erp.web.rest.errors.*;
import com.izhiliu.erp.web.rest.item.ShopeeProductResource;
import com.izhiliu.erp.web.rest.item.param.*;
import com.izhiliu.erp.web.rest.item.vm.*;
import com.izhiliu.erp.web.rest.provider.dto.HomeTodoVO;
import com.izhiliu.erp.web.rest.util.HeaderUtil;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.entity.*;
import com.izhiliu.open.shopee.open.sdk.api.item.param.InitTierVariationParam;
import com.izhiliu.open.shopee.open.sdk.api.item.result.*;
import com.izhiliu.open.shopee.open.sdk.api.logistic.LogisticApi;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import com.izhiliu.open.shopee.open.sdk.entity.logistics.LogisticsResult;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ClientUserDTO;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.helper.StringUtil;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.izhiliu.erp.domain.item.ShopeeProduct.*;
import static com.izhiliu.erp.service.item.mapper.list.ProductListMapperImpl_V2.fillingDiscount;

/**
 * Service Implementation for managing ShopeeProduct.
 */
@Primary
@Service
public class ShopeeProductServiceImpl extends IBaseServiceImpl<ShopeeProduct, ShopeeProductDTO, ShopeeProductRepository, ShopeeProductMapper> implements ShopeeProductService {

    private final Logger log = LoggerFactory.getLogger(ShopeeProductServiceImpl.class);
    @Resource
    private LogisticApi logisticApi;
    @Resource
    protected UaaService uaaService;
    @Resource
    private ProductMetaDataService productMetaDataService;

    @Resource
    private ShopeeProductMediaService shopeeProductMediaService;

    @Resource
    private ShopeeProductDescService shopeeProductDescService;

    @Resource
    protected ApplicationProperties properties;

    @Resource
    protected ItemApi itemApi;

    @Resource
    protected Sequence sequence;

    @Resource
    protected CurrencyRateApiImpl currency;

    @Resource
    protected ShopeeModelChannel shopeeModelBridge;

    @Resource
    protected RedisLockHelper redisLockHelper;

    @Resource
    protected ProductListMapper_V2 platformProductListMapper_v2;

    @Resource
    protected BatchEditProductMapperOld batchEditProductVOMapper;

    @Resource
    protected PlatformProductListMapper platformProductListMapper;

    @Resource
    protected ShopProductListMapper shopProductListMapper;

    @Resource
    protected ShopeeProductDescRepository shopeeProductDescRepository;

    @Resource
    protected ShopeeProductSkuRepository shopeeProductSkuRepository;

    @Resource
    protected ShopeeSkuAttributeRepository shopeeSkuAttributeRepository;

    @Resource
    protected ShopeeProductAttributeValueRepository shopeeProductAttributeValueRepository;

    @Resource
    protected ShopeeProductMediaRepository ShopeeProductMediaRepository;

    @Resource
    protected PlatformNodeService platformNodeService;

    @Resource
    protected ShopeeProductAttributeValueService shopeeProductAttributeValueService;

    @Resource
    protected ShopeeSkuAttributeService shopeeSkuAttributeService;

    @Resource
    BusinessShopeeProductSkuService businessShopeeProductSkuService;

    @Resource
    protected ShopeeProductSkuService shopeeProductSkuService;

    @Resource
    protected ShopeeCategoryService shopeeCategoryService;


    @Resource
    private BatchBoostItemService batchBoostItemService;
    @Resource
    private ShopeeProductExtraInfoService shopeeProductExtraInfoService;


    @Resource
    private CategoryMapService categoryMapService;

    @Autowired
    @Qualifier(value = "handleProductExceptionInfo")
    private HandleProductExceptionInfo handleProductExceptionInfo;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private RedissonDistributedLocker locker;

    @Resource
    private ShopeePushContent shopeePushContent;

    @Resource
    private ShopeeModelChannel shopeeModelChannel;

    @Resource
    @Lazy
    private ShopeeProductUpdateAsync shopeeProductUpdateAsync;
    @Resource
    private TaskExecutorUtils taskExecutorUtils;

    @Resource
    private ShopInfoRedisUtils shopInfoRedisUtils;

    @Resource
    private MQProducerService mqProducerService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private KyyCategoryRelationService kyyCategoryRelationService;

    private Executor executor;
    private ExecutorService criticalExecutor;
    private ExecutorService imageExecutor;

    public ShopeeProductServiceImpl() {
        executor = Executors.newFixedThreadPool(20);
        criticalExecutor = Executors.newFixedThreadPool(20);
        imageExecutor = Executors.newFixedThreadPool(20);
    }

    LoggerOp getLoggerOpObejct() {
        return new LoggerOp().start().setKind(LogConstant.SHOPEE_PRODUCT).setType(LogConstant.PUT).setCode(LogConstant.SPU);
    }


    @Override
    public Optional<ShopeeProduct> selectShopProductByParentIdAndShopId(Long parentId, Long shopId) {
        final List<ShopeeProduct> shopeeProducts = repository.shopProduct(parentId, shopId);

        return handleRepeatProduct(shopeeProducts);
    }

    @Override
    public Optional<ShopeeProduct> nodeProduct(Long parentId, Long platformNodeId) {
        final List<ShopeeProduct> shopeeProducts = repository.nodeProduct(parentId, platformNodeId);

        return handleRepeatProduct(shopeeProducts);
    }

    /**
     * 处理重复商品
     *
     * @param shopeeProducts
     * @return
     */
    private Optional<ShopeeProduct> handleRepeatProduct(List<ShopeeProduct> shopeeProducts) {
        ShopeeProduct nodeProduct;
        if (shopeeProducts == null || shopeeProducts.size() == 0) {
            return Optional.empty();
        } else {
            nodeProduct = shopeeProducts.get(0);

            for (int i = 1; i < shopeeProducts.size(); i++) {
                delete(shopeeProducts.get(i).getId());
            }
        }
        return Optional.ofNullable(nodeProduct);
    }

    @Override
    public List<Long> shopIds(long productId) {
        return repository.shopIds(productId);
    }

    @Override
    public List<ProductListVM_V2> listShopProductByParentId(Long parentId, List<Long> publishShops, List<Long> unpublishShops) {
        return platformProductListMapper_v2.toDto(repository.listShopProductByParentId(parentId, publishShops, unpublishShops));
    }

    @Override
    public int updateByCategoryId(long categoryId, ShopeeProductDTO dto) {
        return repository.update(mapper.toEntity(dto), new QueryWrapper<>(new ShopeeProduct().setCategoryId(categoryId)).eq("deleted", 0));
    }


    @Override
    public boolean saveOrUpdate(ShopeeProductDTO dto) {
        return super.saveOrUpdate(dto);
    }

    @Override
    public boolean superUpdate(ShopeeProductDTO dto) {
        return super.update(dto);
    }

    @Override
    public boolean update(ShopeeProductDTO dto) {
        fillShopeeCategoryId(dto);
        //将media 和 desc 分开存储≈
        saveOrUpdateImageDescToOtherTable(dto);
        return super.update(dto);
    }


    public boolean updateIgnoreSkuCode(ShopeeProductDTO dto) {
        fillShopeeCategoryId(dto);
        //将media 和 desc 分开存储≈
        saveOrUpdateImageDescToOtherTable(dto);
        ShopeeProduct entity = mapper.toEntity(dto);
        return SqlHelper.retBool(repository.update(entity,
                new UpdateWrapper<ShopeeProduct>()
                        .set("sku_code", Objects.isNull(entity.getSkuCode()) ? null : entity.getSkuCode())
                        .set("source_url", Objects.isNull(entity.getSourceUrl()) ? null : entity.getSourceUrl())
                        .eq("id", entity.getId())
        ));
    }

    /**
     * 新增更新，新增逻辑
     * @param dto
     * @return
     */
    public boolean updateOrsaveIgnoreSkuCode(ShopeeProductDTO dto) {
        //final ShopeeProduct nodeProduct = nodeProduct(dto.getpa, platformNodeId).orElse(null);
        //直接用ID 去查item_shopee_product表
        ShopeeProduct queryData = repository.selectById(dto.getId());
        if (queryData == null){
            //补完商品数据
            dto = replenishProductBase(dto);
        }
        //校验商品类目
        fillShopeeCategoryIdV2(dto);
        //将media 和 desc 分开存储，此处逻辑可以复用(不存在则新增)
        saveOrUpdateImageDescToOtherTable(dto);

        ShopeeProduct entity = mapper.toEntity(dto);
        //此处需要判断是新商品还是旧商品
        if (queryData != null){
            //旧商品走原有逻辑
            return SqlHelper.retBool(repository.update(entity,
                    new UpdateWrapper<ShopeeProduct>()
                            .set("sku_code", Objects.isNull(entity.getSkuCode()) ? null : entity.getSkuCode())
                            .set("source_url", Objects.isNull(entity.getSourceUrl()) ? null : entity.getSourceUrl())
                            .eq("id", entity.getId())
            ));
        }else {
            //新商品直接走新增逻辑
            //entity.setType(Type.PLATFORM_NODE.code);
            return SqlHelper.retBool(repository.insert(entity));
        }
    }

    @Override
    public ResponseEntity<Object> checkUpdate(ShopeeProductDTO dto) {
        final LoggerOp loggerOpObejct = getLoggerOpObejct();
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        final boolean update;
        try {
            loggerOpObejct.setLoginId(securityInfo.getCurrentLogin());
            log.info(loggerOpObejct.setMessage("checkUpdate").toString());
            checkParam(dto);
            update = updateIgnoreSkuCode(dto);
            log.info(loggerOpObejct.ok().setMessage("checkUpdate").toString());
        } catch (Throwable e) {
            log.error(loggerOpObejct.error().setMessage(" put checkUpdate error  param " + JSONObject.toJSONString(dto)).toString(), e);
            throw e;
        }
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ShopeeProductResource.ENTITY_NAME, dto.getId().toString())).build();
    }

    /**
     * Change the original logic,Add new or modified logic
     * @param dto
     * @return
     */
    @Override
    public ResponseEntity<Object> checkUpdateOrSave(ShopeeProductDTO dto) {
        final LoggerOp loggerOpObejct = getLoggerOpObejct();
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        final boolean update;
        try {
            loggerOpObejct.setLoginId(securityInfo.getCurrentLogin());
            log.info(loggerOpObejct.setMessage("checkUpdate").toString());
            //参数校验函数，这里保持原有逻辑未动
            checkParamTwo(dto);
            //新的更新和新增逻辑
            update = updateOrsaveIgnoreSkuCode(dto);
            log.info(loggerOpObejct.ok().setMessage("checkUpdate").toString());
        } catch (Throwable e) {
            log.error(loggerOpObejct.error().setMessage(" put checkUpdate error  param " + JSONObject.toJSONString(dto)).toString(), e);
            throw e;
        }
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ShopeeProductResource.ENTITY_NAME, dto.getId().toString())).build();
    }

    @Override
    public void checkParam(ShopeeProductDTO dto) {
        final Optional<ShopeeProductDTO> result = find(dto.getId());

        /*final String currentUser = SecurityUtils.currentLogin();
        if (result.isPresent() && (!result.get().getLoginId().equals(currentUser) && !currentUser.equals(ANONYMOUS_USER))) {
            throw new IllegalOperationException("非法操作");
        }*/

        /*
         * 平台商品走默认最大值
         */
        final ApplicationProperties.ExamineParamShopee paramShopee = getExamineParamShopee(dto, result);

        if (dto.getVWeight() == null || dto.getVWeight() < MIN_WEIGHT) {
            throw new IllegalOperationException("product.weight.min", true);
        }
        if (StrUtil.isBlank(dto.getName()) || dto.getName().length() > paramShopee.getName()) {
            throw new IllegalOperationException("product.name.size", new String[]{paramShopee.getName().toString()});
        }
        if (StrUtil.isBlank(dto.getDescription()) || dto.getDescription().length() < paramShopee.getMinDescription() || dto.getDescription().length() > paramShopee.getDescription()) {
            throw new IllegalOperationException("product.description.size", new String[]{paramShopee.getMinDescription().toString(), paramShopee.getDescription().toString()});
        } else if (ReUtil.count(DESCRIPTION_TAG, dto.getDescription()) > DESCRIPTION_MAX_TAG_COUNT) {
            //throw new IllegalOperationException("product.description.flag", true);
            throw new BadRequestAlertException("description illegal", "shopeeProduct", "product.description.flag");
        }

        //  图片
        checkImage(dto);

        if (dto.getSendOutTime() == null) {
            throw new IllegalOperationException("product.send.out.time", new String[]{Integer.toString(paramShopee.getMaxShipDays())});
        }

//        int sendOutTime = 2;
//        if (PlatformNodeEnum.SHOPEE_TW.id.equals(result.get().getPlatformNodeId())) {
//            sendOutTime = 3;
//            if (dto.getSendOutTime() == 2) {
//                dto.setSendOutTime(3);
//            }
//        }
        if (dto.getSendOutTime() > paramShopee.getMaxShipDays()) {
            throw new IllegalOperationException("product.send.out.time", new String[]{Integer.toString(paramShopee.getMaxShipDays())});
        }

        //  价格  检查
//        checkPrice(dto);

        //  库存
//        if(Objects.nonNull(dto.getStock())&&dto.getStock()  > 999999){
//            throw  new  IllegalOperationException("product.stock.max", true);
//        }
    }


    /**
     * 补充缺失的商品数据
     * @param dto
     */
    public ShopeeProductDTO replenishProductBase(ShopeeProductDTO dto){
        final Optional<ShopeeProductDTO> productExist = find(dto.getParentId());
        dto.setType(Type.PLATFORM_NODE.code);
        if (!productExist.isPresent()) {
            log.error("商品不存在 : {}", dto.getParentId());
            return dto;
        }
        ShopeeProductDTO product = productExist.get();
        //开始拷贝
        Class dtoCls = dto.getClass();
        Field[] dtoFiles = dtoCls.getDeclaredFields();

        Class proCls = product.getClass();
        Field[] proFiles = proCls.getDeclaredFields();

        //开始复制属性
        Arrays.stream(dtoFiles).forEach(dtofile -> {
            dtofile.setAccessible(true);
            Arrays.stream(proFiles).forEach(proFile -> {
                proFile.setAccessible(true);
                try {
                    if (dtofile.getName().equals(proFile.getName()) && dtofile.get(dto) == null && proFile.get(product) != null){
                        dtofile.set(dto,proFile.get(product));
                    }
                } catch (IllegalAccessException e) {
                    log.error("属性拷贝异常：",e.getMessage());
                }
            });
        });
        PlatformNodeEnum nodeEnum = Arrays.asList(PlatformNodeEnum.values()).stream().filter(v -> v.id.equals(dto.getPlatformNodeId())).findFirst().orElse(null);
        if (nodeEnum!=null){
            dto.setCurrency(nodeEnum.currency);
        }
        return dto;
    }

    @Override
    public void checkParamTwo(ShopeeProductDTO dto) {
        if (dto.getVWeight() == null || dto.getVWeight() < MIN_WEIGHT) {
            throw new IllegalOperationException("product.weight.min", true);
        }
        //  图片
        checkImage(dto);
        if (dto.getSendOutTime() == null) {
            throw new IllegalOperationException("product.send.out.time", "");
        }
    }

    void checkImage(ShopeeProductDTO dto) {
        if (dto.getImages() == null || dto.getImages().size() == 0 || dto.getImages().size() > MAX_IMAGE_COUNT) {
            throw new IllegalOperationException("product.image.size", true);
        }
        dto.setImages(dto.getImages().stream().map(image -> {
            if (image.startsWith("//")) {
                return "https:" + image;
            }
            return image;
        }).collect(Collectors.toList()));
        ;
        if (dto.getImages().stream().anyMatch(this::notImageUrl)) {
            throw new IllegalOperationException("product.image.url", true);
        }
    }

    void checkPrice(ShopeeProductDTO dto) {
        //  价格  检查    Variations' price differences are too large. Limitations detail: ID - 10 times; SG/MY - 7 times; TW/PH/VN - 5 times
        final String currency = dto.getCurrency();
        if (Objects.nonNull(currency)) {
            int times = 0;
            if ("IDR".contains(currency)) {
                times = 10;
            } else if ("TWD/PHP/VND".contains(currency)) {
                times = 5;
            } else if ("SGD/MYR/BRL".contains(currency)) {
                times = 7;
            }
            if (dto.getMinPrice() < (dto.getMaxPrice() / times)) {
                throw new IllegalOperationException("product.price.times");
            }
        }
    }

    private ApplicationProperties.ExamineParamShopee getExamineParamShopee(ShopeeProductDTO dto, Optional<ShopeeProductDTO> result) {
        ApplicationProperties.ExamineParamShopee paramShopee;
        if (Type.PLATFORM.code.equals(result.get().getType())) {
            paramShopee = properties.getShopee().get(null);
        } else {
            paramShopee = properties.getShopee().get(dto.getCurrency() == null ? repository.selectById(dto.getId()).getCurrency() : dto.getCurrency());
        }
        return paramShopee;
    }

    private boolean notImageUrl(String image) {
        if (Objects.isNull(image)) {
            return true;
        }
        //  https   这个也会包进去
        if (image.startsWith("http")) {
            return false;
        }
        return true;
    }

    @Override
    public Optional<ShopeeProductDTO> find(Long id) {
        Optional<ShopeeProductDTO> dto = super.find(id);
        dto.ifPresent(shopeeProductDTO -> {
            if (StringUtils.isNotBlank(shopeeProductDTO.getMetaDataId())) {
                //设置采集图片
                productMetaDataService.findOne(shopeeProductDTO.getMetaDataId())
                        .ifPresent(productMetaDataDTO -> {
                            List<String> allCollectionImages = new ArrayList<>();
                            if (null == productMetaDataDTO.getMainImages()) {
                                //兼容处理
                                allCollectionImages.addAll(productMetaDataDTO.getImages());
                            } else {
                                allCollectionImages.addAll(productMetaDataDTO.getMainImages());
                                allCollectionImages.addAll(productMetaDataDTO.getDescImages());
                            }
                            shopeeProductDTO.setAllCollectionImages(allCollectionImages);
                        });
            }

            //设置商品媒体资源
            ShopeeProductMediaDTO shopeeProductMediaDTO = shopeeProductMediaService.selectByProductIdNotCache(shopeeProductDTO.getId());
            if (null != shopeeProductMediaDTO) {
                shopeeProductDTO.setImages(shopeeProductMediaDTO.getImages());
                shopeeProductDTO.setSizeChart(shopeeProductMediaDTO.getSizeChart());
                shopeeProductDTO.setPriceRange(shopeeProductMediaDTO.getPriceRange());
                shopeeProductDTO.setDiscountActivityId(Objects.equals(shopeeProductMediaDTO.getDiscountActivityId(), 0L) ? null : shopeeProductMediaDTO.getDiscountActivityId());
                shopeeProductDTO.setDiscountActivityName(shopeeProductMediaDTO.getDiscountActivityName());
            }


            //设置商品详情
            ShopeeProductDescDTO shopeeProductDescDTO = shopeeProductDescService.selectByProductId(shopeeProductDTO.getId());
            if (null != shopeeProductDescDTO) {
                shopeeProductDTO.setDescription(shopeeProductDescDTO.getDescription());
            }
        });
        return dto;
    }


    @Override
    public Optional<ShopeeProductDTO> findThrow(long id) {
        final Optional<ShopeeProductDTO> dto = find(id);
        dto.orElseThrow(() -> new DataNotFoundException("data.not.found.exception.item.deleted", true));
        return dto;
    }

    @Override
    public Optional<ShopeeProductDTO> findShopeeProductDTO21(long id) {
        final Optional<ShopeeProductDTO> dto = Optional.ofNullable(this.mapper.toDto(this.repository.selectById(id)));
        dto.orElseThrow(() -> new DataNotFoundException("data.not.found.exception.item.deleted", true));
        return dto;
    }

    @Override
    public Optional<ShopeeProductDTO> findByItemIdAndShopId(long itemId, long shopId) {
        return Optional.ofNullable(mapper.toDto(repository.selectOne(new QueryWrapper<>(new ShopeeProduct().setShopeeItemId(itemId).setShopId(shopId)).eq("deleted", 0))));
    }

    @Override
    public ShopeeProductDTO save(ShopeeProductDTO dto) {
        fillShopeeCategoryId(dto);
        dto.setId(sequence.nextId());
        dto.setId(super.save(dto).getId());
        //将media 和 desc 分开存储≈
        return saveOrUpdateImageDescToOtherTable(dto);
    }

    /**
     * 兼容shopee类目不在最后一级，把类目id设置为null
     * @param dto
     * @return
     */
    @Override
    public ShopeeProductDTO collectSave(ShopeeProductDTO dto) {
        collectFillShopeeCategoryId(dto);
        dto.setId(sequence.nextId());
        dto.setId(super.save(dto).getId());
        //将media 和 desc 分开存储≈
        return saveOrUpdateImageDescToOtherTable(dto);
    }

    public ShopeeProductDTO doSaveSource(ShopeeProductDTO dto) {
        fillShopeeCategoryId(dto);
        dto.setId(super.save(dto).getId());
        //将media 和 desc 分开存储
        return saveOrUpdateImageDescToOtherTable(dto);
    }

    private ShopeeProductDTO saveOrUpdateImageDescToOtherTable(ShopeeProductDTO dto) {
        //将media 和 desc 分开存储
        ShopeeProductDescDTO shopeeProductDescDTO = shopeeProductDescService.selectByProductId(dto.getId());
        if (null != shopeeProductDescDTO) {
            shopeeProductDescDTO.setGmtModified(Instant.now());
            shopeeProductDescDTO.setDescription(dto.getDescription());
            shopeeProductDescService.updateAndCleanCache(shopeeProductDescDTO);
        } else {
            if (StringUtils.isNotBlank(dto.getDescription())) {
                shopeeProductDescDTO = new ShopeeProductDescDTO();
                shopeeProductDescDTO.setProductId(dto.getId());
                shopeeProductDescDTO.setDescription(dto.getDescription());
                shopeeProductDescService.save(shopeeProductDescDTO);
            }
        }

        ShopeeProductMediaDTO shopeeProductMediaDTO = shopeeProductMediaService.selectByProductId(dto.getId());
        if (null != shopeeProductMediaDTO) {
            shopeeProductMediaDTO.setGmtModified(Instant.now());
            fillShopeeProductMediaData(dto, shopeeProductMediaDTO);
            shopeeProductMediaService.updateAndCleanCache(shopeeProductMediaDTO);
        } else {
            if (!CollectionUtils.isEmpty(dto.getImages()) || StringUtils.isNotBlank(dto.getSizeChart())) {
                shopeeProductMediaDTO = new ShopeeProductMediaDTO();
                shopeeProductMediaDTO.setProductId(dto.getId());
                fillShopeeProductMediaData(dto, shopeeProductMediaDTO);
                shopeeProductMediaService.save(shopeeProductMediaDTO);
            }
        }
        //    todo  暂时不需要 增加此表
//         ShopeeProductExtraInfoDto ShopeeProductExtraInfoDto = ShopeeProductExtraInfoService.selectByProductId(dto.getId());
//        if (null != ShopeeProductExtraInfoDto) {
//            ShopeeProductExtraInfoDto.setGmtModified(Instant.now());
//
//        } else {
//            if (!CollectionUtils.isEmpty(dto.getImages()) ||StringUtils.isNotBlank(dto.getSizeChart())) {
//                ShopeeProductExtraInfoDto = new ShopeeProductExtraInfoDto();
//                ShopeeProductExtraInfoDto.setProductId(dto.getId());
//                ShopeeProductExtraInfoService.save(ShopeeProductExtraInfoDto);
//            }
//        }
        return dto;
    }

    private void fillShopeeProductExtraInfoData(ShopeeProductDTO dto, ShopeeProductExtraInfoDto ShopeeProductExtraInfoDTO) {
        ShopeeProductExtraInfoDTO.setSales(dto.getSales());
        ShopeeProductExtraInfoDTO.setCmtCount(dto.getCmtCount());
        ShopeeProductExtraInfoDTO.setLikes(dto.getLikes());
        ShopeeProductExtraInfoDTO.setViews(dto.getViews());
        ShopeeProductExtraInfoDTO.setRatingStar(dto.getRatingStar());
    }

    private void fillShopeeProductMediaData(ShopeeProductDTO dto, ShopeeProductMediaDTO shopeeProductMediaDTO) {
        shopeeProductMediaDTO.setImages(dto.getImages());
        shopeeProductMediaDTO.setSizeChart(dto.getSizeChart());
        shopeeProductMediaDTO.setPriceRange(dto.getPriceRange());
        shopeeProductMediaDTO.setDiscountActivityId(dto.getDiscountActivityId());
        shopeeProductMediaDTO.setDiscountActivity(dto.getDiscountActivity());
        shopeeProductMediaDTO.setDiscountActivityName(dto.getDiscountActivityName());
    }


    @Override
    public Long generate() {
        return sequence.nextId();
    }

    /**
     * @param productIds erp 商品 id
     * @param loginId    拥有人
     * @param status     erp 商品状态
     * @param isShopItem 是否是 在线商品
     * @return
     */
    @Override
    public List<ShopeeProduct> findList(List<Long> productIds, String loginId, Integer status, Boolean isShopItem) {
        if (!Objects.requireNonNull(productIds).isEmpty()) {
            return repository.findList(productIds, loginId, status, isShopItem);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public void batchPublish(List<Publish> publishDtos, String start, String end) {
        final List<@NotNull Long> collect = publishDtos.stream().map(Publish::getProductId).collect(Collectors.toList());
        List<ShopeeProduct> list = repository.findList(collect, null, LocalProductStatus.NO_PUBLISH.code, null);
        list.addAll(repository.findList(collect, null, LocalProductStatus.IN_TIMED_RELEASE.code, null));
        list.parallelStream().forEach(shopeeProduct -> {
            String redisKey = null;
            for (int i = 0; i < publishDtos.size(); i++) {
                if (Objects.equals(publishDtos.get(i).getProductId(), shopeeProduct.getId())) {
                    redisKey = publishDtos.get(i).getKey();
                }
            }
            Objects.requireNonNull(redisKey);
            shopeeProduct.setStatus(LocalProductStatus.IN_TIMED_RELEASE);
            String finalRedisKey = redisKey;
            shopeeProduct.setFeature(JSON.toJSONString(new HashMap<String, String>() {{
                put("start", start);
                put("end", end);
                put("redisKey", finalRedisKey);
            }}));
            repository.updateById(shopeeProduct);
        });
    }

    @Override
    public void deleteBatchPublish(List<Long> productIds) {
        if (Objects.nonNull(productIds) && productIds.size() > 0) {
            for (Long productId : productIds) {
                repository.updateStatusByItemId(productId, LocalProductStatus.NO_PUBLISH.code);
            }

        }

    }

    @Override
    @SubLimitAnnotation(limitProduct = SubLimitProductConstant.UPLOAD_OFFER_TIMES)
    public ShopeeProductDTO saveSource(ShopeeProductInsertDTO dto) {
        ShopeeProductDTO shopeeProduct = dto.getShopeeProduct();

        initShopeeProduct(shopeeProduct);

        VariationVM param = dto.getParam();
        initAndCheckParam(shopeeProduct, param);
        final ShopeeProductDTO shopeeProductDTO = this.doSaveSource(shopeeProduct);
        businessShopeeProductSkuService.coverByProductSource(param, shopeeProduct);

        return shopeeProductDTO;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<List<ShopeeProductDTO>> selectShopeeProductBatch(List<Long> itemIdList) {
        return Optional.ofNullable(mapper.toUrlDTO(repository.listBatch(itemIdList)));
    }

    @Override
    public List<ShopeeProduct> selectBatchIds(List<Long> productIds) {
        return repository.selectBatchIds(productIds);
    }

    @Override
    public Long getProductIdByItemId(Long itemId) {
        return repository.getProductIdByItemId(itemId);
    }

    @Override
    public List<ShopeeProduct> findShopeeProductList(List<Long> itmeIds) {
        if (CommonUtils.isBlank(itmeIds)) {
            return new ArrayList<>();
        }
        return repository.findShopeeProductList(itmeIds);
    }

    @Override
    public void pushSuccessHandle(ShopeeProductDTO product, LocalProductStatus pushSuccess) {
        final String currency = product.getCurrency();
        final String name = product.getName();
        final Long shopId = product.getShopId();
        final Long id = ShopeeUtil.nodeId(currency);
        final Long itemId = product.getShopeeItemId();
        Optional<PlatformNodeDTO> platformNodeDTO = platformNodeService.find(id);
        String onlineUrl = String.format(ShopeeConstant.ITEM_DETAILED_URL, platformNodeDTO.get().getUrl(), name.replaceAll(" ", "-").replaceAll("%", ""), shopId, itemId);
        product.setOnlineUrl(onlineUrl);
        product.setFeature(JSONObject.toJSONString(new HashMap<String, String>() {{
            put("error", "success");
        }}));
        product.setStatus(pushSuccess);
        update(product);
    }

    @Override
    public void pushFailHandle(String productId, String errorMsg, LocalProductStatus pushFailure) {
        final ShopeeProductDTO product = new ShopeeProductDTO();
        product.setId(Long.valueOf(productId));
        int maxLength = (errorMsg.length() < 200) ? errorMsg.length() : 200;
        final String messageKey02 = errorMsg.substring(0, maxLength);
        product.setFeature(JSONObject.toJSONString(new HashMap<String, String>() {{
            put("error", messageKey02);
        }}));
        product.setStatus(pushFailure);
        update(product);
    }

    @Override
    public void updateStatus(Long productId, LocalProductStatus pushFailure) {
        repository.updateStatusByItemId(productId, pushFailure.code);
    }

    @Override
    public Boolean checkNoInitTierVariation(ShopeeProductDTO product) {
        Integer variationTier = product.getVariationTier();
        if (CommonUtils.isBlank(variationTier) || variationTier.compareTo(0) == 0) {
            return true;
        }
        //双层sku发布丢失
        try {
            GetItemDetailResult data = itemApi.getItemDetail(product.getShopId(), product.getShopeeItemId()).getData();
            if (data.getItem().is2TierItem()) {
                return true;
            }
            retryInitTierVariation(product);
        } catch (Exception e) {
            log.error("init商品层级出错", e);
        }
        return true;
    }

    @Override
    public TaskExecuteVO syncByShop(ItemSyncQO qo) {
        final String login = qo.getLogin();
        final List<Long> shopIds = qo.getShopIds();
        final String taskId = qo.getTaskId();
        final boolean needDeletedItem = qo.isNeedDeletedItem();
        TaskExecuteVO result = TaskExecuteVO.builder()
                .taskId(taskId)
                .build();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(shopIds)) {
            return result;
        }
        List<String> collect = shopIds.stream().map(String::valueOf).collect(Collectors.toList());
        List<String> allShopId = shopInfoRedisUtils.getShopId(collect, false);
        if (CommonUtils.isBlank(allShopId)) {
            return result;
        }
        result.setSyncShop(allShopId.size());
        taskExecutorUtils.initSyncTask(taskId, 0L, 0);
        allShopId.forEach(e -> {
            ItemSyncDTO dto = ItemSyncDTO.builder()
                    .login(login)
                    .taskId(taskId)
                    .shopId(Long.valueOf(e))
                    .needDeletedItem(needDeletedItem)
                    .build();
            criticalExecutor.submit(() -> shopeeModelChannel.pull(dto));
        });
        return result;
    }

    @Override
    @Deprecated
    public TaskExecuteVO syncBatch(ItemSyncQO qo) {
        final String login = qo.getLogin();
        final String taskId = qo.getTaskId();
        List<ItemSyncQO.SyncItem> syncItems = qo.getSyncItems();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(syncItems) || CommonUtils.isBlank(taskId)) {
            throw new LuxServerErrorException("error param");
        }
        TaskExecuteVO result = TaskExecuteVO.builder()
                .taskId(taskId)
                .total((long) syncItems.size())
                .build();
        taskExecutorUtils.initSyncTask(taskId, result.getTotal(), null);
        List<GetItemListResult.ItemsBean> itemsBeans = syncItems.stream().map(e -> {
            final Long shopId = e.getShopId();
            final Long itemId = e.getItemId();
            GetItemListResult.ItemsBean itemsBean = new GetItemListResult.ItemsBean();
            itemsBean.setItemId(itemId);
            itemsBean.setShopid(shopId);
            return itemsBean;
        }).collect(Collectors.toList());
        ShopeePullMessageDTO messageDTO = ShopeePullMessageDTO.builder()
                .login(login)
                .taskId(taskId)
                .items(itemsBeans)
                .build();
        mqProducerService.sendMQ(ShopeePullMessageDTO.TAG, login + "_" + taskId, messageDTO);
        return result;
    }

    @Override
    public ProductSyncVO getSyncProcess(String login, String taskId) {
        if (CommonUtils.isBlank(CommonUtils.decodeTaskId(taskId, login))) {
            throw new LuxServerErrorException("Illegal Operation!");
        }
        //总任务进度
        Map entries = stringRedisTemplate.boundHashOps(taskId).entries();
        ProductSyncVO vo = ProductSyncVO.builder()
                .code(Integer.parseInt(entries.getOrDefault("code", "0").toString()))
                .syncShop(Integer.parseInt(entries.getOrDefault("syncShop", "0").toString()))
                .total(Integer.parseInt(entries.getOrDefault("total", 0).toString()))
                .handleTotal(Integer.parseInt( entries.getOrDefault("handleTotal", 0).toString()))
                .success(Integer.parseInt(entries.getOrDefault("success", 0).toString()))
                .fail(Integer.parseInt( entries.getOrDefault("fail", 0).toString()))
                .build();
        boolean timeout = stringRedisTemplate.getExpire(taskId) < 3300;
        if (timeout) {
            //过去5分钟，柔和处理
            vo.setHandleTotal(vo.getTotal());
            vo.setSuccess(vo.getTotal() - vo.getFail());
        }
        Map<Object, Object> shopEntries = stringRedisTemplate.boundHashOps(taskId.concat("_shop")).entries();
        if (CommonUtils.isNotBlank(shopEntries)) {
            Map<String, Map<String, Integer>> mapMap = new HashMap<>();
            shopEntries.entrySet().forEach(e -> {
                String[] s = ((String) e.getKey()).split("_");
                //key
                String childKey = s[0];
                //shopId
                String shopId = s[1];
                //value值
                Integer value = CommonUtils.isNotBlank(e.getValue()) ? Integer.parseInt(e.getValue().toString()) : 0;
                Map<String, Integer> stringIntegerMap = mapMap.get(shopId);
                if (CommonUtils.isBlank(stringIntegerMap)) {
                    stringIntegerMap = new HashMap<>();
                }
                stringIntegerMap.put(childKey, value);
                mapMap.put(shopId, stringIntegerMap);
            });
            List<ProductSyncVO.ShopSync> collect = mapMap.entrySet().stream().map(e -> {
                Map<String, Integer> value = e.getValue();
                ProductSyncVO.ShopSync shopSync = ProductSyncVO.ShopSync.builder()
                        .shopId(e.getKey())
                        .total(value.get("total"))
                        .handleTotal(value.get("handleTotal"))
                        .success(value.get("success"))
                        .fail(CommonUtils.isNotBlank(value.get("fail")) ? value.get("fail") : 0)
                        .build();
                ShopeeShopDTO baseShopInfo = shopInfoRedisUtils.getBaseShopInfo(Long.valueOf(shopSync.getShopId()));
                if (CommonUtils.isNotBlank(baseShopInfo) && CommonUtils.isNotBlank(baseShopInfo.getShopName())) {
                    shopSync.setShopName(baseShopInfo.getShopName());
                }
                if (timeout) {
                    //过去5分钟，柔和处理
                    shopSync.setHandleTotal(shopSync.getTotal());
                    shopSync.setSuccess(shopSync.getTotal() - shopSync.getFail());
                }
                return shopSync;
            }).collect(Collectors.toList());
            vo.setShopSyncList(collect);
        }
        return vo;
    }

    private void retryInitTierVariation(ShopeeProductDTO product) {
        List<ShopeeSkuAttributeDTO> skuAttributes = shopeeSkuAttributeService.pageByProduct(
                product.getId(),
                new Page(0, Integer.MAX_VALUE))
                .getRecords();

        List<ShopeeProductSkuDTO> productSkus = shopeeProductSkuService.pageByProduct(
                product.getId(),
                new Page(0, Integer.MAX_VALUE))
                .getRecords();

        List<TierVariation> tierVariations = getInitTierVariations(skuAttributes);
        final List<Variation> increaseVariations = getInitVariations(productSkus);

        ShopeeResult<VariationResult> variationResultShopeeResult = itemApi.initTierVariation(InitTierVariationParam.builder()
                .shopId(product.getShopId())
                .itemId(product.getShopeeItemId())
                .tierVariations(tierVariations)
                .variations(increaseVariations)
                .build());
        if (variationResultShopeeResult.isResult()) {
            saveTwoVariationId(product.getShopId(), product.getId(), productSkus, variationResultShopeeResult);
        }
    }

    private void saveTwoVariationId(long shopId, long productId, List<ShopeeProductSkuDTO> productSkus, ShopeeResult<VariationResult> result) {
        if (!result.isResult()) {
            log.error("[saveTwoVariationIdError]: {}  {} {} ", result.getError().getMsg(), shopId, productId);
            return;
        }

        if (result.getData() == null || result.getData().getVariationIdList() == null) {
            log.error("[saveTwoVariationIdError]:  result.getData():{}  result.getData().getVariationIdList():{} {} ", result.getData(), result.getData().getVariationIdList());

        } else {
            for (int i = 0; i < result.getData().getVariationIdList().size(); i++) {
                final VariationResult.VariationIdList variationIndex = result.getData().getVariationIdList().get(i);

                // 只需要填充没有variationId的sku
                for (ShopeeProductSkuDTO productSku : productSkus) {

                    if (variationIndex.getTierIndex().size() == 2) {
                        if (Arrays.asList(productSku.getSkuOptionOneIndex(), productSku.getSkuOptionTowIndex()).equals(variationIndex.getTierIndex())) {
                            productSku.setShopeeVariationId(variationIndex.getVariationId());
                            productSku.setOriginalPrice(0f);
                            shopeeProductSkuService.update(productSku);
                            break;
                        }
                    } else {
                        if (Arrays.asList(productSku.getSkuOptionOneIndex()).equals(variationIndex.getTierIndex())) {
                            productSku.setShopeeVariationId(variationIndex.getVariationId());
                            productSku.setOriginalPrice(0f);
                            shopeeProductSkuService.update(productSku);
                            break;
                        }
                    }
                }
            }
        }
    }

    private List<Variation> getInitVariations(List<ShopeeProductSkuDTO> productSkus) {
        if (CommonUtils.isBlank(productSkus)) {
            return new ArrayList<>();
        }
        List<Variation> result = productSkus.stream().map(productSku -> {
            Variation variation = new Variation();
            variation.setStock(productSku.getStock());
            variation.setPrice(productSku.getPrice());
            variation.setVariationSku(productSku.getSkuCode());
            fillIndex(productSku, variation);
            return variation;
        }).collect(Collectors.toList());
        return result;
    }

    private void fillIndex(ShopeeProductSkuDTO productSku, Variation variation) {
        if (null != productSku.getSkuOptionTowIndex()) {
            variation.setTierIndex(Arrays.asList(productSku.getSkuOptionOneIndex(), productSku.getSkuOptionTowIndex()));
        } else {
            variation.setTierIndex(Arrays.asList(productSku.getSkuOptionOneIndex()));
        }
    }

    private List<TierVariation> getInitTierVariations(List<ShopeeSkuAttributeDTO> skuAttributes) {
        boolean isFirst = true;
        final List<TierVariation> tierVariations = new ArrayList<>(skuAttributes.size());
        for (ShopeeSkuAttributeDTO skuAttribute : skuAttributes) {
            final TierVariation tierVariation = new TierVariation();
            tierVariation.setName(skuAttribute.getName());
            tierVariation.setOptions(skuAttribute.getOptions());
            if (isFirst) {
                isFirst = false;
                tierVariation.setImagesUrl(skuAttribute.getImagesUrl());
            }
            tierVariations.add(tierVariation);
        }
        return tierVariations;
    }


    private void initAndCheckParam(ShopeeProductDTO shopeeProduct, VariationVM param) {
        param.getVariations().forEach(variation -> variation.setId(null));

        param.setProductId(shopeeProduct.getId());
        businessShopeeProductSkuService.checkParam(
                param,
                Optional.ofNullable(shopeeProduct));
    }

    private void initShopeeProduct(ShopeeProductDTO shopeeProduct) {
        final String currentUserLogin = SecurityUtils.getCurrentLogin();

        shopeeProduct.setType(Type.PLATFORM.code);
        shopeeProduct.setPlatformId(PlatformEnum.SHOPEE.getCode().longValue());
        shopeeProduct.setLogistics(null);

        shopeeProduct.setLoginId(currentUserLogin);
        final String metaDataId = shopeeProduct.getMetaDataId();
        if (StringUtils.isBlank(metaDataId)) {
            shopeeProduct.setMetaDataId(shopeeProduct.getId().toString());
        } else {
            shopeeProduct.setMetaDataId(metaDataId);  //  元数据
        }

        shopeeProduct.setCollect(PlatformEnum.KEYOUYUN.getName());
        if (Objects.isNull(shopeeProduct.getCollectUrl())) {
            shopeeProduct.setCollectUrl("https://erp.keyouyun.com/offers/posts/10/source/" + shopeeProduct.getId());
        }

        shopeeProduct.setMinPrice(-1f);
        shopeeProduct.setMaxPrice(-1f);

        shopeeProduct.setWeight(new Double(shopeeProduct.getVWeight() * 1000).longValue());
//        product.setCrossBorder(productInfo.getCrossBorderOffer());
        //  Todo  暂时默认为 货币为 中国人名币
        if (shopeeProduct.getCurrency() == null) {
            shopeeProduct.setCurrency(CurrencyEnum.CNY.code);
        }
        shopeeProduct.setVariationTier(0);
    }

    private void fillShopeeCategoryIdV2(ShopeeProductDTO dto) {
        if (dto.getCategoryId() != null && dto.getCategoryId() != 0L && dto.getShopeeCategoryId() == null) {
            Optional<ShopeeCategoryDTO> exist = shopeeCategoryService.find(dto.getCategoryId());
            if (!exist.isPresent()) {
                return;
            }
            final ShopeeCategoryDTO category = exist.get();

            // 例: 马来站的商品绑定了非马来站类目时触发, 根据id查询平台站点ID
            if (dto.getId() != null) {
                if (!category.getPlatformNodeId().equals(dto.getPlatformNodeId())) {
                    throw new IllegalOperationException("illegal.operation.exception.category.exception", true);
                }
            }

            // 不能绑定非末端类目
            if (1 == category.getHasChild()) {
                throw new IllegalOperationException("illegal.operation.exception.category.exception", true);
            }
            dto.setShopeeCategoryId(category.getShopeeCategoryId());
        }
    }

    private void fillShopeeCategoryId(ShopeeProductDTO dto) {
        if (dto.getCategoryId() != null && dto.getCategoryId() != 0L && dto.getShopeeCategoryId() == null) {
            Optional<ShopeeCategoryDTO> exist = shopeeCategoryService.find(dto.getCategoryId());
            if (!exist.isPresent()) {
                return;
            }
            final ShopeeCategoryDTO category = exist.get();

            // 例: 马来站的商品绑定了非马来站类目时触发, 根据id查询平台站点ID
            if (dto.getId() != null) {
                ShopeeProduct product = new ShopeeProduct();
                product.setId(dto.getId());
                if (!category.getPlatformNodeId().equals(repository.selectOne(new QueryWrapper<>(product).eq("deleted", 0).select("platform_node_id")).getPlatformNodeId())) {
                    throw new IllegalOperationException("illegal.operation.exception.category.exception", true);
                }
            }

            // 不能绑定非末端类目
            if (1 == category.getHasChild()) {
                throw new IllegalOperationException("illegal.operation.exception.category.exception", true);
            }
            dto.setShopeeCategoryId(category.getShopeeCategoryId());
        }
    }

    private void collectFillShopeeCategoryId(ShopeeProductDTO dto) {
        if (dto.getCategoryId() != null && dto.getCategoryId() != 0L && dto.getShopeeCategoryId() == null) {
            Optional<ShopeeCategoryDTO> exist = shopeeCategoryService.find(dto.getCategoryId());
            if (!exist.isPresent()) {
                return;
            }
            final ShopeeCategoryDTO category = exist.get();

            // 例: 马来站的商品绑定了非马来站类目时触发, 根据id查询平台站点ID
            if (dto.getId() != null) {
                ShopeeProduct product = new ShopeeProduct();
                product.setId(dto.getId());
                if (!category.getPlatformNodeId().equals(repository.selectOne(new QueryWrapper<>(product).eq("deleted", 0).select("platform_node_id")).getPlatformNodeId())) {
                    throw new IllegalOperationException("illegal.operation.exception.category.exception", true);
                }
            }

            // 不能绑定非末端类目
            if (1 == category.getHasChild()) {
                dto.setCategoryId(null);
                return;
//                throw new IllegalOperationException("illegal.operation.exception.category.exception", true);
            }
            dto.setShopeeCategoryId(category.getShopeeCategoryId());
        }
    }

    @Override
    public List<ShopeeProductDTO> listByMetaData(String metaDataId) {
        return mapper.toDto(repository.listByMetaDataId(metaDataId));
    }


    @Override
    public boolean delete(Long id) {
        if (Objects.isNull(id)) {
            return false;
        }
        boolean isDelete = true;
        final Optional<ShopeeProductDTO> product = find(id);
        if (product.isPresent() && product.get().getShopeeItemId() != null && product.get().getShopeeItemId() != 0) {
            final ShopeeResult<DeleteItemResult> deleteItemResultShopeeResult = itemApi.deleteItem(product.get().getShopId(), product.get().getShopeeItemId());
            if (!deleteItemResultShopeeResult.isResult()) {
                isDelete = false;
                superUpdate(new ShopeeProductDTO()
                        .setId(product.get().getId())
                        .setGmtModified(Instant.now())
                        .setStatus(LocalProductStatus.PUSH_FAILURE)
                        .setFeature(FeatrueUtil.addFeature(product.get().getFeature(), "error", deleteItemResultShopeeResult.getError().getMsg())));
            }
        }

        if (isDelete) {
            shopeeProductExtraInfoService.deleteByProduct(id);
            shopeeProductDescService.deleteByProductId(id);
            shopeeProductMediaService.deleteByProductId(id);
            shopeeProductAttributeValueService.deleteByProduct(id);
            shopeeProductSkuService.deleteByProduct(id);
            shopeeSkuAttributeService.deleteByProduct(id);
            return super.delete(id);
        }
        return false;
    }

    @Override
    public boolean deleteLocal(Long id) {
        if (Objects.isNull(id)) {
            return false;
        }

        shopeeProductExtraInfoService.deleteByProduct(id);
        shopeeProductDescService.deleteByProductId(id);
        shopeeProductMediaService.deleteByProductId(id);
        shopeeProductAttributeValueService.deleteByProduct(id);
        shopeeProductSkuService.deleteByProduct(id);
        shopeeSkuAttributeService.deleteByProduct(id);
        return super.delete(id);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean deleteOther(Long id) {
        if (Objects.isNull(id)) {
            return false;
        }
        shopeeProductExtraInfoService.deleteByProduct(id);
        shopeeProductDescService.deleteByProductId(id);
        shopeeProductMediaService.deleteByProductId(id);
        shopeeProductAttributeValueService.deleteByProduct(id);
        shopeeProductSkuService.deleteByProduct(id);
        shopeeSkuAttributeService.deleteByProduct(id);
        return true;
    }

    @Override
    public boolean delete(Collection<Long> idList) {
        List<ShopeeProduct> shopeeProducts = repository.findIfShopeeItem(idList.parallelStream().collect(Collectors.toList()));
        //先删掉product表，然后异步删除其他相关表
        repository.deleteBatchIds(idList);
        executor.execute(() -> {
            idList.forEach(this::deleteOther);
        });
        //如果是在线商品，需要同步删除在线商品
        deleteBatchForOnlineProduct(shopeeProducts);

        //删除站点商品和sku相关属性
        executor.execute(() -> idList.forEach(id -> repository.childs(id, Type.PLATFORM_NODE.code)
                .forEach(shopeeProductDTO -> {
                    executor.execute(() -> {
                        delete(shopeeProductDTO.getId());
                    });
                })));
        return true;
    }

    private void deleteBatchForOnlineProduct(List<ShopeeProduct> shopeeProducts) {
        //todo 异步处理，失败了没有回补
        executor.execute(() -> {
            shopeeProducts.forEach(shopeeProduct -> {
                try {
                    ShopeeResult<DeleteItemResult> deleteItemResultShopeeResult = itemApi.deleteItem(shopeeProduct.getShopId(), shopeeProduct.getShopeeItemId());
                    if (!deleteItemResultShopeeResult.isResult()) {
                        log.error("delete shopee product shopId:{},shopeeItemId:{} error :{}", shopeeProduct.getShopId(), shopeeProduct.getShopeeItemId(), deleteItemResultShopeeResult.getData().getMsg());
                    }
                } catch (Exception e) {
                    log.error("delete shopee product shopId:{},shopeeItemId:{} error :", shopeeProduct.getShopId(), shopeeProduct.getShopeeItemId(), e);
                }
            });
        });
    }

    @Override
    public int deleteByItemId(long itemId) {
        return repository.deleteByItemId(itemId);
    }


    @Override
    public List<ShopeeProductDTO> childs(long parentId, int childType) {
        return mapper.toDto(repository.childs(parentId, childType));
    }


    @Override
    public List<ShopeeProduct> childs$(long parentId, int childType) {
        return repository.childs(parentId, childType);
    }


    @Override
    public Long copyToShop(long productId, long shopId, String loginId) {
        final Optional<ShopeeProductDTO> productExist = find(productId);
        if (!productExist.isPresent()) {
            log.error("[商品不存在]: {}", productId);
            return null;
        }
        final ShopeeProductDTO product = productExist.get();

        if (Objects.equals(Type.SHOP.code, product.getType()) && Objects.nonNull(product.getShopId())) {
            log.info("[ 已经是个店铺商品 不需要在 copy ]");
            return productId;
        }
        final ShopeeProduct shopProduct = selectShopProductByParentIdAndShopId(productId, shopId).orElse(null);
        if (shopProduct == null) {
            log.info("[生成店铺商品]");
            final boolean lock = redisLockHelper.lock(StringUtil.join(Arrays.asList("copyToShop", String.valueOf(productId), String.valueOf(shopId), String.valueOf(loginId)), ":")
                    , 30, TimeUnit.SECONDS
            );
            if (!lock) {
                return null;
            }

            product.setLogistics(validLogistic(shopId, product.getLogistics()));
            product.setShopId(shopId);
            product.setType(Type.SHOP.code);
            product.setLoginId(loginId);
            return copyProductInfo(product);
        } else if (shopProduct.getShopeeItemId() == null || shopProduct.getShopeeItemId() == 0L) {
            log.info("[更新未发布商品]");
            product.setLoginId(loginId);
            copyUpdateBasicAndAttribute(product, shopProduct.getId());
        }

        return shopProduct.getId();
    }

    /**
     *
     * @param productId 商品id
     * @param platformNodeId 节点ID
     * @return 返回站点商品ID
     * modify by rain ，date：2020年6月19日
     */
    @Override
    public Map copyToPlatformNodeTwo(long productId, long platformNodeId) {

        Map resultMap = new HashMap();
        //通过站点ID查询 改平台是否存在
        final Optional<PlatformNodeDTO> platformNodeExist = platformNodeService.find(platformNodeId);
        if (!platformNodeExist.isPresent()) {
            log.error("平台站点不存在 : {}", platformNodeId);
            return null;
        }
        //通过商品ID查询商品是否存在
        final Optional<ShopeeProductDTO> productExist = find(productId);
        if (!productExist.isPresent()) {
            log.error("商品不存在 : {}", productId);
            return null;
        }
        //判断商品是否为平台商品 type = 1
        final ShopeeProductDTO product = productExist.get();
        if (!product.getType().equals(ShopeeProduct.Type.PLATFORM.code)) {
            log.error("改商品不是平台商品 : {}", productId);
            return null;
        }

        try {
            final String metaDataId = product.getMetaDataId();
            if (StringUtils.isNotBlank(metaDataId)) {
                productMetaDataService.findOne(metaDataId).ifPresent(productMetaDataDTO -> {
                    //  因为是 元数据 转 平台数据  所以 获取到的其实就是 元数据 类目
                    final Long mateDataCategoryId = productMetaDataDTO.getCategoryId();
                    if (Objects.nonNull(mateDataCategoryId)) {
                        //  如果  平台 id 相同  并且 节点id相同    就不进行任何操作
                        if (Objects.equals(product.getPlatformId(), productMetaDataDTO.getPlatformId())) {
                            if (Objects.equals(product.getPlatformNodeId(), productMetaDataDTO.getPlatformNodeId())) {
                                return;
                            }
                        }
                        if(product.getPlatformNodeId() == null){
                            product.setPlatformNodeId(platformNodeId);
                        }
                        final Long mateDataplatform = productMetaDataDTO.getPlatformId();
                        final Long mateDataplatformNodeId = productMetaDataDTO.getPlatformNodeId();
                        final Long productPlatform = product.getPlatformId();
                        final Long productPlatformNodeId = product.getPlatformNodeId();
                        ItemCategoryMap itemCategoryMap = categoryMapService.selectByObj(mateDataplatform, mateDataplatformNodeId, mateDataCategoryId, productPlatform, productPlatformNodeId);
                        if (Objects.nonNull(itemCategoryMap)) {
                            final Optional<ShopeeCategoryDTO> byPlatformNodeAndShopeeCategory = shopeeCategoryService.findByPlatformNodeAndShopeeCategory(itemCategoryMap.getDstPlatfromNodeId(), itemCategoryMap.getDstCategroyId());
                            ;
                            byPlatformNodeAndShopeeCategory.ifPresent(shopeeCategoryDTO -> {
                                //获取目标类目ID,虾皮的类目ID
                                resultMap.put("dstCategroyId",itemCategoryMap.getDstCategroyId());
                                //我们表中的类目ID
                                resultMap.put("categoryId",shopeeCategoryDTO.getId());
                            });
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }
        /*
         * 不能重复Copy, 如果已存在则直接返回ID
         *
         * 当前被Copy的商品, 在当前站点只能有一个子类
         */
        final ShopeeProduct nodeProduct = nodeProduct(productId, platformNodeId).orElse(null);
        if (nodeProduct != null) {
            log.info("[站点商品已存在 - 返回已存在的商品ID]");
            resultMap.put("id", String.valueOf(nodeProduct.getId()));
            resultMap.put("isNew", "1");
            return resultMap;
        }else{
            //不存在，则生成一个站点商品ID返回前端，不再copy商品
            resultMap.put("id", String.valueOf(sequence.nextId()));
            resultMap.put("isNew", "0");
            return resultMap;
        }
    }

    @Override
    public Long copyToPlatformNode(long productId, long platformNodeId) {
        final Optional<PlatformNodeDTO> platformNodeExist = platformNodeService.find(platformNodeId);
        if (!platformNodeExist.isPresent()) {
            log.error("平台站点不存在 : {}", platformNodeId);
            return null;
        }

        final Optional<ShopeeProductDTO> productExist = find(productId);
        if (!productExist.isPresent()) {
            log.error("商品不存在 : {}", productId);
            return null;
        }
        final ShopeeProductDTO product = productExist.get();

        if (!product.getType().equals(ShopeeProduct.Type.PLATFORM.code)) {
            log.error("改商品不是平台商品 : {}", productId);
            return null;
        }

        /*
         * 不能重复Copy, 如果已存在则直接返回ID
         *
         * 当前被Copy的商品, 在当前站点只能有一个子类
         */
        final ShopeeProduct nodeProduct = nodeProduct(productId, platformNodeId).orElse(null);
        if (nodeProduct != null) {
            log.info("[站点商品已存在 - 返回已存在的商品ID]");
            return nodeProduct.getId();
        }

        log.info("[from {} to {}]", product.getPlatformNodeId(), platformNodeId);

        product.setPlatformNodeId(platformNodeId);
        product.setType(Type.PLATFORM_NODE.code);

        try {
            final String metaDataId = product.getMetaDataId();
            if (StringUtils.isNotBlank(metaDataId)) {
                productMetaDataService.findOne(metaDataId).ifPresent(productMetaDataDTO -> {
                    //  因为是 元数据 转 平台数据  所以 获取到的其实就是 元数据 类目
                    final Long mateDataCategoryId = productMetaDataDTO.getCategoryId();
                    if (Objects.nonNull(mateDataCategoryId)) {
                        //  如果  平台 id 相同  并且 节点id相同    就不进行任何操作
                        if (Objects.equals(product.getPlatformId(), productMetaDataDTO.getPlatformId())) {
                            if (Objects.equals(product.getPlatformNodeId(), productMetaDataDTO.getPlatformNodeId())) {
                                return;
                            }
                        }
                        final Long mateDataplatform = productMetaDataDTO.getPlatformId();
                        final Long mateDataplatformNodeId = productMetaDataDTO.getPlatformNodeId();
                        final Long productPlatform = product.getPlatformId();
                        final Long productPlatformNodeId = product.getPlatformNodeId();
                        ItemCategoryMap itemCategoryMap = categoryMapService.selectByObj(mateDataplatform, mateDataplatformNodeId, mateDataCategoryId, productPlatform, productPlatformNodeId);
                        if (Objects.nonNull(itemCategoryMap)) {
                            final Optional<ShopeeCategoryDTO> byPlatformNodeAndShopeeCategory = shopeeCategoryService.findByPlatformNodeAndShopeeCategory(itemCategoryMap.getDstPlatfromNodeId(), itemCategoryMap.getDstCategroyId());
                            ;
                            byPlatformNodeAndShopeeCategory.ifPresent(shopeeCategoryDTO -> {
                                product.setCategoryId(shopeeCategoryDTO.getId());
                                product.setShopeeCategoryId(itemCategoryMap.getDstCategroyId());
                            });
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }

        return cover(product, platformNodeExist.get());
    }


    @Override
    public List<Long> publishToShop(SaveOrPublishToShopParam param) {
        final String currentUserLogin = SecurityUtils.currentLogin();
        final List<Long> shopIds = uaaService.getShopeeShopInfoV2(SecurityUtils.getCurrentLogin(), SecurityUtils.isSubAccount()).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());
        param.setShopIds(param.getShopIds().stream()
                .filter(shopIds::contains).collect(Collectors.toList()));

        /*
         * 源数据 平台商品 站点商品 店铺商品
         * (层级关系\在自身所在的层级是唯一的\向下分裂只能分裂到不同的平台、站点、店铺, 不存在一份源数据采集到同一平台两次...
         *
         * 1. 一个平台商品分裂到店铺的子商品 只能有一个
         * 2. 找到当前商品的所有子商品 => 如果子商品有关联当前 shopId 则判存在
         * 3. 不存在则分裂 存在则更新
         */
        final List<Long> productIds = new ArrayList<>(param.getShopIds().size());
        for (Long shopId : param.getShopIds()) {
            try {
                if (param.isAsync()) {
                    criticalExecutor.execute(() -> publishToShop(param, shopId, currentUserLogin));
                } else {
                    final Future<Long> future = criticalExecutor.submit(() -> publishToShop(param, shopId, currentUserLogin));
                    if (future.get() != null && future.get() != 0L) {
                        productIds.add(future.get());
                    }
                }
            } catch (Exception e) {
                log.error("[保存或发布到店铺] - 异步异常", e);
            }
        }
        return productIds;
    }


    /**
     * 中间商品优化，
     * @param param
     * @return
     */
    @Override
    public List<Long> publishToShopTwo(SaveOrPublishToShopParam param) {
        final String currentUserLogin = SecurityUtils.currentLogin();
        final List<Long> shopIds = uaaService.getShopeeShopInfoV2(SecurityUtils.getCurrentLogin(), SecurityUtils.isSubAccount()).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());
        param.setShopIds(param.getShopIds().stream()
                .filter(shopIds::contains).collect(Collectors.toList()));
        /*
         * 源数据 平台商品 站点商品 店铺商品
         * (层级关系\在自身所在的层级是唯一的\向下分裂只能分裂到不同的平台、站点、店铺, 不存在一份源数据采集到同一平台两次...
         *
         * 1. 一个平台商品分裂到店铺的子商品 只能有一个
         * 2. 找到当前商品的所有子商品 => 如果子商品有关联当前 shopId 则判存在
         * 3. 不存在则分裂 存在则更新
         */
        final List<Long> productIds = new ArrayList<>(param.getShopIds().size());
        final CountDownLatch cdl = new CountDownLatch(param.getShopIds().size());//参数为线程个数
        for (Long shopId : param.getShopIds()) {
            try {
                if (param.isAsync()) {
                    criticalExecutor.execute(() -> publishToShopTwo(param, shopId, currentUserLogin,cdl));
                } else {
                    final Future<Long> future = criticalExecutor.submit(() -> publishToShop(param, shopId, currentUserLogin));
                    if (future.get() != null && future.get() != 0L) {
                        productIds.add(future.get());
                    }
                }
            } catch (Exception e) {
                log.error("[保存或发布到店铺] - 异步异常", e);
            }
        }

        try {
            cdl.await();
            if (param.getDeleted() == 0){
                //则表示前端勾选删除站点商品，则需要清除站点商品 type=2
                //开始删除站点商品
                removePlatItem(param);
            }
        }catch (InterruptedException e){
            log.error("[保存或发布到店铺] - 异步异常", e);
        }

        return productIds;
    }

    /**
     * 清除站点商品逻辑
     */
    public void removePlatItem(SaveOrPublishToShopParam param){
        log.info("删除站点商品,productId={}", param.getProductId());
        /**
         * 需要清除的数据表
         * 1.商品主表：item_shopee_product
         * 2.商品详情：item_shopee_product_desc
         * 3.sku表：item_shopee_product_sku
         * 4.sku属性表：item_shopee_sku_attribute
         * 5.商品类目属性表：item_shopee_product_attribute_value
         * 6.商品媒体资源表：item_shopee_product_media
         */

        if(param != null && param.getProductId() != null){
            //删除商品主表
            repository.deleteById(param.getProductId());

            //删除商品详情
            shopeeProductDescRepository.deleteByDeleted(param.getProductId());

            //删除商品sku信息,根据productId删除
            Map<String,Object> columnMap = new HashMap<>();
            columnMap.put("product_id",param.getProductId());
            //删除SKU信息
            shopeeProductSkuRepository.deleteByMap(columnMap);

            //删除sku属性信息,根据productId删除
            shopeeSkuAttributeRepository.deleteByMap(columnMap);

            //删除商品类目属性信息,根据productId删除
            shopeeProductAttributeValueRepository.deleteByMap(columnMap);

            //删除商品媒体资源,根据productId删除
            ShopeeProductMediaRepository.deleteByMap(columnMap);
        }

    }


    @Override
    public List<Long> saveToShop(SaveOrPublishToShopParam param) {
        final List<Long> shopIds = uaaService.getShopeeShopInfoV2(SecurityUtils.getCurrentLogin(), SecurityUtils.isSubAccount()).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());
        param.setShopIds(param.getShopIds().stream()
                .filter(shopIds::contains).collect(Collectors.toList()));

        /*
         * 源数据 平台商品 站点商品 店铺商品
         * (层级关系\在自身所在的层级是唯一的\向下分裂只能分裂到不同的平台、站点、店铺, 不存在一份源数据采集到同一平台两次...
         *
         * 1. 一个平台商品分裂到店铺的子商品 只能有一个
         * 2. 找到当前商品的所有子商品 => 如果子商品有关联当前 shopId 则判存在
         * 3. 不存在则分裂 存在则更新
         */

        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        final List<Long> productIds = new ArrayList<>(param.getShopIds().size());
        for (Long shopId : param.getShopIds()) {
            try {
                if (param.isAsync()) {
                    criticalExecutor.execute(() -> saveToShop(param, shopId, securityInfo));
                } else {
                    final Future<Long> future = criticalExecutor.submit(() -> saveToShop(param, shopId, securityInfo));
                    if (future.get() != null && future.get() != 0L) {
                        productIds.add(future.get());
                    }
                }
            } catch (Exception e) {
                log.error("[保存或发布到店铺] - 异步异常", e);
            }
        }
        return productIds;
    }


    public Long saveToShop(SaveOrPublishToShopParam param, long shopId, SecurityInfo securityInfo) {
        try {
            final Long productId = param.getProductId();
            final ShopeeProductDTO targetProduct = find(productId).orElseThrow(() -> new DataNotFoundException("data.not.found.exception.product.not.found", new String[]{"productId : " + productId}));
            if (!targetProduct.getType().equals(Type.PLATFORM_NODE.code)) {
                log.error("[保存或发布到店铺] - 企图拷贝非站点商品");
                return 0L;
            }
            //   todo  外面调用的地方进行店铺信息数据校验

//            final List<Long> shopIds = uaaService.getShopeeShopInfoV2(param.getLoginId(), false).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());
//            if (!param.getLoginId().equals(targetProduct.getLoginId()) || !shopIds.contains(shopId)) {
//                log.error("[保存或发布到店铺] - [非法操作 | 店铺 or 商品不属于该用户] : productId: {}, shopId: {}", targetProduct.getId(), shopId);
//                return 0L;
//            }

            ShopeeProduct shopProduct = selectShopProductByParentIdAndShopId(productId, shopId).orElse(null);
            if (shopProduct != null) {
                log.info("[店铺商品已存在]");
                /*
                 * 已发布不能动
                 */
                if (shopProduct.getShopeeItemId() != null && shopProduct.getShopeeItemId() != 0L) {
                    return shopProduct.getId();
                }
            }

            /*
             * 不存在 拷贝
             * 存在 覆盖更新
             */
            if (shopProduct == null) {
                targetProduct.setShopId(shopId);
                targetProduct.setType(Type.SHOP.code);
                targetProduct.setLoginId(securityInfo.getParentLogin());
                String discountId = getDiscountIdByShop(param, shopId);
                if (CommonUtils.isNotBlank(discountId)) {
                    //存在折扣
                    targetProduct.setDiscountActivityId(Long.valueOf(discountId));
                    targetProduct.setDiscountActivity(true);
                }
                final Long copyOfProductId = copyProductInfo(targetProduct);
                final ShopeeProductDTO copyOfProduct = find(copyOfProductId).get();

                /*
                 * 去除不属于它的物流渠道
                 */
                try {
                    copyOfProduct.setLogistics(validLogistic(shopId, copyOfProduct.getLogistics()));
                    superUpdate(copyOfProduct);
                } catch (Exception e) {
                    log.error("[去除物流渠道异常] - [清除全部物流渠道]");
                    copyOfProduct.setLogistics(Collections.EMPTY_LIST);
                    superUpdate(copyOfProduct);
                }

                if (param.getType()) {
                    if (copyOfProduct.getShopeeItemId() == null || copyOfProduct.getShopeeItemId() == 0) {
                        log.info("[发布店铺商品] - type:{}, itemId:{}", param.getType(), copyOfProduct.getShopeeItemId());
                        shopeeModelBridge.publish(copyOfProductId, shopId, securityInfo.getParentLogin());
                    }
                }
                shopProduct = new ShopeeProduct();
                shopProduct.setId(copyOfProductId);
            } else if (param.getType() && (shopProduct.getShopeeItemId() == null || shopProduct.getShopeeItemId() == 0)) {
                copyUpdate(targetProduct, shopProduct.getId());
                log.info("[发布店铺商品] - type:{}, itemId:{}", param.getType(), shopProduct.getShopeeItemId());
                shopeeModelBridge.publish(shopProduct.getId(), shopId, securityInfo.getParentLogin());
            }
            return shopProduct.getId();
        } catch (Exception e) {
            log.error("[发布或更新商品异常]", e);
        }
        return 0L;
    }

    @Resource
    SubLimitService subLimitService;

    /**
     * @return true  数据正常  false 数据异常
     */
    public Optional<ShopeeProductDTO> checkeParam(SaveOrPublishToShopParam param, long shopId, String currentUserLogin) {
        final ShopeeProductDTO targetProduct = find(param.getProductId()).orElseThrow(() -> new DataNotFoundException("data.not.found.exception.product.not.found", new String[]{"productId : " + param.getLoginId()}));
        if (Objects.equals(currentUserLogin, targetProduct.getLoginId()))
            if (!targetProduct.getType().equals(Type.PLATFORM_NODE.code)) {
                log.error("[保存或发布到店铺] - 企图拷贝非站点商品");
                return Optional.empty();
            }
//        final List<Long> shopIds = uaaService.getShopeeShopInfoV2(param.getLoginId(), false).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());
//        if (!param.getLoginId().equals(targetProduct.getLoginId()) || !shopIds.contains(shopId)) {
//            log.error("[保存或发布到店铺] - [非法操作 | 店铺 or 商品不属于该用户] : productId: {}, shopId: {}", targetProduct.getId(), shopId);
//            return Optional.empty();
//        }
        return Optional.ofNullable(targetProduct);
    }

    public Long publishToShopTwo(SaveOrPublishToShopParam param, long shopId, String currentUserLogin,CountDownLatch cdl) {
        try {
            final Long platformNodeProductId = param.getProductId();
            final String discountId = getDiscountIdByShop(param, shopId);

            final Optional<ShopeeProductDTO> shopeeProductDTO = checkeParam(param, shopId, currentUserLogin);
            if (!shopeeProductDTO.isPresent()) {
                cdl.countDown();
                return 0L;
            }
            final ShopeeProductDTO siteProduct = shopeeProductDTO.get();
            if (CommonUtils.isNotBlank(discountId)) {
                //存在折扣
                siteProduct.setDiscountActivityId(Long.valueOf(discountId));
                siteProduct.setDiscountActivity(true);
            }
            boolean isPublish = Boolean.FALSE;

            ShopeeProduct shopProduct = selectShopProductByParentIdAndShopId(platformNodeProductId, shopId).orElse(null);
            /*
             * 不存在 拷贝
             * 存在 覆盖更新
             */
            if (Objects.isNull(shopProduct)) {
                siteProduct.setShopId(shopId);
                siteProduct.setType(Type.SHOP.code);
                siteProduct.setLoginId(currentUserLogin);
                final Long copyOfProductId = copyProductInfo(siteProduct);
                final ShopeeProductDTO copyOfProduct = find(copyOfProductId).get();
                /*
                 * 去除不属于它的物流渠道
                 */
                try {
                    copyOfProduct.setLogistics(validLogistic(shopId, copyOfProduct.getLogistics()));
                    superUpdate(copyOfProduct);
                } catch (Exception e) {
                    log.error("[去除物流渠道异常] - [清除全部物流渠道]");
                    copyOfProduct.setLogistics(new ArrayList<>());
                    superUpdate(copyOfProduct);
                    cdl.countDown();
                    return shopProduct.getId();
                }

                if (param.getType()) {
                    if (copyOfProduct.getShopeeItemId() == null || copyOfProduct.getShopeeItemId() == 0) {
                        log.info("[发布店铺商品] - type:{}, itemId:{}", param.getType(), copyOfProduct.getShopeeItemId());
                        isPublish = Boolean.TRUE;
                    }
                }
                shopProduct = new ShopeeProduct();
                shopProduct.setId(copyOfProductId);
            } else {
                log.info("[店铺商品已存在]");
                /*
                 * 已发布不能动
                 */
                if (!(shopProduct.getShopeeItemId() != null && shopProduct.getShopeeItemId() != 0L)) {
                    copyUpdate(siteProduct, shopProduct.getId());
                    log.info("[发布店铺商品] - type:{}, itemId:{}", param.getType(), shopProduct.getShopeeItemId());
                    isPublish = Boolean.TRUE;
                }
            }
            if (isPublish) {
                shopeeModelBridge.publishSupport(shopProduct.getId(), shopId, currentUserLogin);
            }
            cdl.countDown();
            return shopProduct.getId();
        } catch (Exception e) {
            log.error("[发布或更新商品异常]", e);
        }
        cdl.countDown();
        return 0L;
    }

    /**
     * 通过店铺获取对应折扣id
     * @param param
     * @param shopId
     * @return
     */
    private String getDiscountIdByShop(SaveOrPublishToShopParam param, long shopId) {
        List<SaveOrPublishToShopParam.ShopDiscount> shopDiscounts = param.getShopDiscounts();
        if (CommonUtils.isBlank(shopDiscounts)) {
            return null;
        }
        SaveOrPublishToShopParam.ShopDiscount shopDiscount = shopDiscounts.stream().filter(e ->CommonUtils.isNotBlank(e.getShopId()) &&  e.getShopId().compareTo(shopId) == 0)
                .findFirst()
                .orElseGet(() -> new SaveOrPublishToShopParam.ShopDiscount());
        return shopDiscount.getDiscountId();
    }

    public Long publishToShop(SaveOrPublishToShopParam param, long shopId, String currentUserLogin) {
        try {
            final Long platformNodeProductId = param.getProductId();

            final Optional<ShopeeProductDTO> shopeeProductDTO = checkeParam(param, shopId, currentUserLogin);
            if (!shopeeProductDTO.isPresent()) {
                return 0L;
            }
            final ShopeeProductDTO siteProduct = shopeeProductDTO.get();
            boolean isPublish = Boolean.FALSE;

            ShopeeProduct shopProduct = selectShopProductByParentIdAndShopId(platformNodeProductId, shopId).orElse(null);
            /*
             * 不存在 拷贝
             * 存在 覆盖更新
             */
            if (Objects.isNull(shopProduct)) {
                siteProduct.setShopId(shopId);
                siteProduct.setType(Type.SHOP.code);
                siteProduct.setLoginId(currentUserLogin);
                final Long copyOfProductId = copyProductInfo(siteProduct);
                final ShopeeProductDTO copyOfProduct = find(copyOfProductId).get();
                /*
                 * 去除不属于它的物流渠道
                 */
                try {
                    copyOfProduct.setLogistics(validLogistic(shopId, copyOfProduct.getLogistics()));
                    superUpdate(copyOfProduct);
                } catch (Exception e) {
                    log.error("[去除物流渠道异常] - [清除全部物流渠道]");
                    copyOfProduct.setLogistics(new ArrayList<>());
                    superUpdate(copyOfProduct);
                    return shopProduct.getId();
                }

                if (param.getType()) {
                    if (copyOfProduct.getShopeeItemId() == null || copyOfProduct.getShopeeItemId() == 0) {
                        log.info("[发布店铺商品] - type:{}, itemId:{}", param.getType(), copyOfProduct.getShopeeItemId());
                        isPublish = Boolean.TRUE;
                    }
                }
                shopProduct = new ShopeeProduct();
                shopProduct.setId(copyOfProductId);
            } else {
                log.info("[店铺商品已存在]");
                /*
                 * 已发布不能动
                 */
                if (!(shopProduct.getShopeeItemId() != null && shopProduct.getShopeeItemId() != 0L)) {
                    copyUpdate(siteProduct, shopProduct.getId());
                    log.info("[发布店铺商品] - type:{}, itemId:{}", param.getType(), shopProduct.getShopeeItemId());
                    isPublish = Boolean.TRUE;
                }
            }
            if (isPublish) {
                shopeeModelBridge.publishSupport(shopProduct.getId(), shopId, currentUserLogin);
            }
            return shopProduct.getId();
        } catch (Exception e) {
            log.error("[发布或更新商品异常]", e);
        }
        return 0L;
    }


    @Override
    public void publishToShop(List<ShopProductParam> params) {
        params = authFilter(params);

        for (ShopProductParam param : params) {
            final String currentLogin = SecurityUtils.currentLogin();
            final Optional<ShopeeProductDTO> productExist = find(param.getProductId());
            if (!productExist.isPresent()) {
                log.error("product not found.", "product id : {}", param.getProductId());
                continue;
            }
            final ShopeeProductDTO product = productExist.get();

            /*
             * 只有店铺商品才能发布
             */
            if (product.getType().equals(ShopeeProduct.Type.SHOP.code)) {
                try {
                    executor.execute(() -> {
                        //如果已经存在shopeeItemId 则更新
                        if (product.getShopeeItemId() != null && product.getShopeeItemId() != 0) {
                            log.info("[该商品已发布，执行更新  ]: productId {}", product.getId());
                            shopeeModelBridge.push(param.getProductId(), param.getShopId(), param.getLoginId());
                        } else {
                            log.info("[发布到店铺] : {}", param);
                            shopeeModelBridge.publishSupport(param.getProductId(), param.getShopId(), param.getLoginId());
                        }
                    });
                } catch (Exception e) {
                    log.error("[发布或者更新到店铺失败]:", e);
                }
            }
        }
    }

    private List<ShopProductParam> authFilter(List<ShopProductParam> params) {

        final List<Long> shopIds = uaaService.getShopeeShopInfoV2(SecurityUtils.getCurrentLogin(), SecurityUtils.isSubAccount()).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());

        params = params.stream()
                .filter(e -> shopIds.contains(e.getShopId())).collect(Collectors.toList());

        return params;
    }


    @Override
    public void pullToLocal(List<ShopProductParam> params) {
        /*
         * TODO itemId And shopId
         */
        for (ShopProductParam param : authFilter(params)) {
            log.info("[拉取覆盖到本地] : {}", param);
            executor.execute(() -> shopeeModelBridge.pull(param.getShopeeItemId(), param.getShopId(), param.getLoginId(), false, ""));
        }
    }

    public static final String SYNC_PRODUCT_TO_LOCAL = "sync-product-to-local:";


    @Override
    @Deprecated
    public void pullToLocalAll(List<Long> shopIds) {
        final List<Long> hisShopIds = uaaService.getShopeeShopInfoV2(SecurityUtils.getCurrentLogin(), SecurityUtils.isSubAccount()).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());
        final String parentLoginId = SecurityUtils.currentLogin();
        shopIds = shopIds.stream().filter(hisShopIds::contains).collect(Collectors.toList());

        log.info("[拉取覆盖全部到本地] : {}", shopIds);

        Integer count = null;
        for (Long shopId : shopIds) {
            final String key = SYNC_PRODUCT_TO_LOCAL + shopId;
            if (log.isDebugEnabled()) {
                log.debug("sync  product  key   {}", key);
            }
            try {
                final boolean success = redisLockHelper.tryCounter(key);
                if (!success) {
                    if (count == null) {
                        count = 0;
                    }
                    final String value = redisLockHelper.get(key);
                    count += Integer.parseInt(value);
                    continue;
                }
                executor.execute(() -> shopeeModelBridge.pull(shopId, parentLoginId));
            } catch (Exception e) {
                log.error("[店铺商品同步异常]", e);
                redisLockHelper.unlock(key);
            }
        }

        if (count != null) {
            throw new RepeatSubmitException("repeat.submit.exception.loading", new String[]{(count == 0 ? "初始化中" : count + "")});
        }
    }

    public void pullToLocalAll(List<Long> shopIds, String taskId, String parentLoginId) {
        final List<Long> hisShopIds = uaaService.getShopeeShopInfoV2(SecurityUtils.getCurrentLogin(), SecurityUtils.isSubAccount()).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());

        shopIds = shopIds.stream().filter(hisShopIds::contains).collect(Collectors.toList());

        log.info("[拉取覆盖全部到本地] : {}", shopIds);
        shopIds.forEach(shopId -> {
            executor.execute(() -> shopeeModelBridge.pull(shopId, parentLoginId, taskId));
        });
    }

    public static final String SYNC_PRODUCT_TO_PULL_LOCAL = "sync-product-to-pull-local:";

    @Override
    @Deprecated
    public ShopSycnResult pullToLocalAllTwo(List<Long> shopIds, List<Long> itmeIds, String taskId) {

        if (CollectionUtils.isEmpty(itmeIds) && CollectionUtils.isEmpty(shopIds)) {
            //直接返回
            return new ShopSycnResult(-1);   //没有数据
        }

        final String parentLoginId = SecurityUtils.currentLogin();
        final List<Long> shopIdss = shopIds;
        final List<Long> itmeIdss = itmeIds;
        ShopSycnResult shopSycnResult = new ShopSycnResult();   //结果重新布局
        ShopeeSyncDTO shopeeSyncDTO = null;
        try {
            RLock lock = locker.lock(SYNC_PRODUCT_TO_PULL_LOCAL + taskId);
            Query query = null;
            //查询此用户状态
            query = new Query(Criteria.where("id").is(taskId)); //查询是否存在
            shopeeSyncDTO = mongoTemplate.findOne(query, ShopeeSyncDTO.class);
            locker.unlock(lock);
            if (shopeeSyncDTO == null) {
                shopeeSyncDTO = new ShopeeSyncDTO(taskId);
                //店铺商品同步
                if (!CollectionUtils.isEmpty(shopIds) && CollectionUtils.isEmpty(itmeIds)) {
                    shopeeSyncDTO.setShopCount(shopIds.size());
                    mongoTemplate.save(shopeeSyncDTO);
                    pullToLocalAll(shopIds, taskId, parentLoginId);
                } else if (!CollectionUtils.isEmpty(shopIds) && !CollectionUtils.isEmpty(itmeIds)) {
                    //重试失败商品
                    shopeeSyncDTO.setShopCount(shopIds.size());
                    mongoTemplate.save(shopeeSyncDTO);
                    shopeeModelBridge.pullByItemIds(shopIdss, parentLoginId, itmeIdss, taskId);
                } else {
                    //单个商品和批量商品同步
                    shopeeModelBridge.pullByItemIds(shopIdss, parentLoginId, itmeIdss, taskId);
                }
            } else if (shopeeSyncDTO.getIfSync() == 1) {

                Map<Long, ShopeeSyncDTO> map = shopeeSyncDTO.getMap();

                int shopCount = shopeeSyncDTO.getShopCount();//同步所有店铺
                if (StringUtils.isNotEmpty(shopeeSyncDTO.getDateTime())) {
                    //判断是否是十分钟还没动静
                    String dateStr2 = shopeeSyncDTO.getDateTime();
                    Date date2 = DateUtil.parse(dateStr2);

                    String now = DateUtil.now();
                    Date date1 = DateUtil.parse(now);

                    long betweenDay = DateUtil.between(date2, date1, DateUnit.MINUTE);
                    if (betweenDay > 2) {
                        if (map != null && map.size() > 0) {
                            for (Map.Entry<Long, ShopeeSyncDTO> mapVlue : map.entrySet()) {
                                ShopeeSyncDTO value = mapVlue.getValue();
                                ShopSycnResult.ShopSycnList sycnList = new ShopSycnResult.ShopSycnList(mapVlue.getKey(), value.getShopName(), value.getCount(), value.getSucceedCount(), value.getCount() - value.getSucceedCount(),
                                        100, value.getLoserDetails(), value.getStatus(), value.getMsg());
                                shopSycnResult.getShopSycnLists().add(sycnList);
                            }
                        }
                        shopSycnResult.setIfSync(2);
                        shopSycnResult.setShopCount(shopeeSyncDTO.getShopCount());
                        mongoTemplate.remove(query, ShopeeSyncDTO.class);
                        return shopSycnResult;
                    }
                }

                int size = map.size();  //当前所有商铺
                int reachNumber = 0;    //完成店铺数量
                if (size == shopCount) {
                    for (Map.Entry<Long, ShopeeSyncDTO> mapVlue : map.entrySet()) {
                        ShopeeSyncDTO value = mapVlue.getValue();
                        if (value.getCount() == (value.getSucceedCount() + value.getLoserCount())) {
                            reachNumber++;
                        }
                    }
                    if (size == reachNumber) {   //所有同步都进行过处理之后
                        shopeeSyncDTO.setIfSync(2);
                        mongoTemplate.remove(query, ShopeeSyncDTO.class);
                    }
                }
            } else {
                Map<Long, ShopeeSyncDTO> shopeeSyncDTOMap = shopeeSyncDTO.getMap();
                if (null != shopeeSyncDTOMap) {
                    shopeeSyncDTOMap.forEach((shopId, shopeeSyncDTO1) -> {
                        if (shopeeSyncDTO1.getCount() == (shopeeSyncDTO1.getSucceedCount() + shopeeSyncDTO1.getLoserCount())) {
                            if (shopeeSyncDTO1.getCount() == shopeeSyncDTO1.getSucceedCount()) {
                                shopeeSyncDTO1.setStatus(2);
                            } else {
                                shopeeSyncDTO1.setStatus(2);
                            }
                        }
                    });
                }
            }

            Map<Long, ShopeeSyncDTO> map = shopeeSyncDTO.getMap();
            if (map != null && map.size() > 0) {
                for (Map.Entry<Long, ShopeeSyncDTO> mapVlue : map.entrySet()) {
                    ShopeeSyncDTO value = mapVlue.getValue();
                    ShopSycnResult.ShopSycnList sycnList = new ShopSycnResult.ShopSycnList(mapVlue.getKey(), value.getShopName(), value.getCount(), value.getSucceedCount(), value.getLoserCount(),
                            value.getSucceedRatio(), value.getLoserDetails(), value.getStatus(), value.getMsg());
                    shopSycnResult.getShopSycnLists().add(sycnList);
                }
            }

            if (0 != shopeeSyncDTO.getShopCount() && shopeeSyncDTO.getShopCount() == (shopeeSyncDTO.getMap().values().parallelStream().filter((shopeeSyncDTO1) -> shopeeSyncDTO1.getStatus() == 2).count())) {
                shopSycnResult.setIfSync(2);
            } else {
                if (StringUtils.isNotEmpty(shopeeSyncDTO.getDateTime())) {
                    //判断是否是2分钟还没动静
                    String dateStr2 = shopeeSyncDTO.getDateTime();
                    Date date2 = DateUtil.parse(dateStr2);
                    String now = DateUtil.now();
                    Date date1 = DateUtil.parse(now);
                    long betweenDay = DateUtil.between(date2, date1, DateUnit.MINUTE);
                    if (betweenDay > 2) {
                        shopSycnResult.setIfSync(2);
                        shopSycnResult.setShopCount(shopeeSyncDTO.getShopCount());
                        mongoTemplate.remove(query, ShopeeSyncDTO.class);
                        return shopSycnResult;
                    }
                } else {
                    shopSycnResult.setIfSync(shopeeSyncDTO.getIfSync());
                }
            }
            shopSycnResult.setShopCount(shopeeSyncDTO.getShopCount());
        } catch (Exception e) {
            log.error("shopIds:{} ,itemIds:{} sync error ", shopIds, itmeIds, e);
        }
        return shopSycnResult;
    }

    @Resource
    private MessageSource messageSource;

    @Override
    public void remove(String key1) {
        boolean b = locker.tryLock(key1, TimeUnit.SECONDS, 5L, 10L);

        final String parentLoginId = SecurityUtils.currentLogin();
        final String key = SYNC_PRODUCT_TO_PULL_LOCAL + parentLoginId + key1;
        Query query = new Query(Criteria.where("id").is(key)); //查询是否存在
        mongoTemplate.remove(query, ShopeeSyncDTO.class);
    }


    @Override
    public void pushToShop(List<ShopProductParam> params) {
        for (ShopProductParam param : params) {
            ShopeeProductDTO productDTO = find(param.getProductId()).get();
            checkParam(productDTO);
            shopeeProductSkuService.checkParam(
                    shopeeProductSkuService.variationByProduct(param.getProductId()),
                    this.find(param.getProductId())
            );
            checkNoInitTierVariation(productDTO);
        }

        final String loginId = SecurityUtils.currentLogin();
        try {
            for (ShopProductParam param : params) {
                log.info("[推送到店铺] : {}", param);
                executor.execute(() -> shopeeModelBridge.push(param.getProductId(), param.getShopId(), loginId));
            }
        } catch (Exception e) {
            log.error("[推送到店铺失败]", e);
        }
    }

    @Override
    public Boolean pushToShopCommon(PushToShopeeTaskQO qo) {
        final String login = qo.getLogin();
        List<String> pushType = qo.getPushType();
        final String taskId = qo.getTaskId();
        final List<ShopProductParam> params = qo.getParams();
        if (CommonUtils.isBlank(pushType) || CommonUtils.isBlank(params)) {
            return false;
        }
        params.forEach(param -> {
            final Long productId = param.getProductId();
            VariationVM variationVM = shopeeProductSkuService.variationByProduct(param.getProductId());
            Optional<ShopeeProductDTO> shopeeProductDTOOptional = find(productId);
            ShopeeProductDTO shopeeProductDTO = shopeeProductDTOOptional.orElseThrow(() -> new LuxServerErrorException("商品" + productId + "不存在!"));
            //check商品
            checkParam(shopeeProductDTO);
            //check商品Sku
            shopeeProductSkuService.checkParam(variationVM, shopeeProductDTOOptional);
            //校验是否正在操作
//            if (shopeeModelChannel.checkStatus(shopeeProductDTO)) {
//                throw new LuxServerErrorException("商品" + productId + "正在更新中，切勿重复更新");
//            }
            param.setLoginId(login);
            param.setProduct(shopeeProductDTO);
            param.setTaskId(taskId);
            param.setLocale(qo.getLocale());
            //20201009 alan 折扣商品价格库存更新发了mq,mq 也会校验 status,导致消息消费被拒
//            if (!pushType.contains(PushToShopeeTaskQO.ITEM_ALL)) {
//                updateStatus(productId, LocalProductStatus.IN_PUSH);
//            }
        });
        executor.execute(() -> {
            TaskExecuteVO.TaskDetail param = TaskExecuteVO.TaskDetail.builder()
                    .taskId(taskId)
                    .endCode(TaskExecuteVO.CODE_SUCCESS)
                    .build();
            try {
                params.forEach(e -> shopeeProductUpdateAsync.handleUpdateAsyc(e, pushType));
            } catch (LuxServerErrorException e) {
                param.setErrorMessage(e.getTitle());
                param.setEndCode(TaskExecuteVO.CODE_FAILD);
            } catch (Exception e) {
                log.error("商品更新出错,login={},taskId={}", login, taskId, e);
                param.setErrorMessage("系统繁忙");
                param.setEndCode(TaskExecuteVO.CODE_FAILD);
            }
            if (CommonUtils.isNotBlank(param.getEndCode())) {
                taskExecutorUtils.handleTaskSet(param);
            }
        });
        return true;
    }

    @Override
    public Boolean pushLocalImageToShopee(PushToShopeeTaskQO qo) {
        final String login = qo.getLogin();
        final List<ShopProductParam> params = qo.getParams();
        final String taskId = qo.getTaskId();
        if (CommonUtils.isBlank(params)) {
            return false;
        }
        params.forEach(e -> {
            ShopeeProductDTO productDTO = find(e.getProductId()).orElseThrow(() -> new LuxServerErrorException("商品" + e.getProductId() + "不存在!"));
            e.setProduct(productDTO);
            e.setTaskId(taskId);
        });
        imageExecutor.execute(() -> {
            TaskExecuteVO.TaskDetail param = TaskExecuteVO.TaskDetail.builder()
                    .taskId(taskId)
                    .endCode(TaskExecuteVO.CODE_SUCCESS)
                    .build();
            try {
                shopeeProductUpdateAsync.handleShopeeIamgeAsyc(params);
            } catch (Exception e) {
                log.error("商品更新出错,login={},taskId={}", login, taskId, e);
                param.setErrorMessage("系统繁忙");
                param.setEndCode(TaskExecuteVO.CODE_FAILD);
            }
            if (CommonUtils.isNotBlank(param.getEndCode())) {
                taskExecutorUtils.handleTaskSet(param);
            }
        });
        return true;
    }

    @Override
    public void shopUnlist(ShopUnListParam param) {
        /*
         * 挑选店铺 批量更新
         */
        final Map<@NotNull Long, List<ShopProductParam>> shopIdGroups = param.getParams().stream()
                .collect(Collectors.groupingBy(ShopProductParam::getShopId));

        for (Map.Entry<Long, List<ShopProductParam>> shopIdGroup : shopIdGroups.entrySet()) {
            executor.execute(() -> shopUnlist(param, shopIdGroup));
        }
    }


    private void shopUnlist(ShopUnListParam param, Map.Entry<Long, List<ShopProductParam>> shopIdGroup) {
        final List<Long> itemIds = shopIdGroup.getValue().stream()
                .map(ShopProductParam::getShopeeItemId)
                .collect(Collectors.toList());

        final List<Long> productIds = shopIdGroup.getValue().stream()
                .map(ShopProductParam::getProductId)
                .collect(Collectors.toList());

        refreshStatus(param.getUnlist(), productIds, null, true);

        final List<UnListItem> unList = itemIds.stream()
                .map(e -> {
                    final UnListItem unListItem = new UnListItem();
                    unListItem.setUnlist(!param.getUnlist());
                    unListItem.setItemId(e);
                    return unListItem;
                }).collect(Collectors.toList());

        log.info("[上下架商品] : {}", unList);

        executor.execute(() -> {
            try {
                final ShopeeResult<UnListItemResult> result = itemApi.unListItem(shopIdGroup.getKey(), unList);
                if (result.isResult()) {
                    // 成功
                    Collection<ShopeeProductDTO> list = list(productIds);
                    for (UnListItemResult.SuccessBean success : result.getData().getSuccess()) {
                        for (ShopeeProductDTO product : list) {
                            if (success.getItemId() == product.getShopeeItemId()) {
                                ShopeeProductDTO update = new ShopeeProductDTO();
                                update.setId(product.getId());
                                update.setShopeeItemStatus(!param.getUnlist() ? ShopeeItemStatus.UNLIST : ShopeeItemStatus.NORMAL);
                                update.setStatus(!param.getUnlist() ? LocalProductStatus.UNLIST_SUCCESS : LocalProductStatus.PUBLIC_SUCCESS);
                                update(update);
                            }
                        }
                    }

                    // 失败
                    for (UnListItemResult.FailedBean failed : result.getData().getFailed()) {
                        for (ShopeeProductDTO product : list) {
                            if (failed.getItemId() == product.getShopeeItemId()) {
                                ShopeeProductDTO update = new ShopeeProductDTO();
                                update.setId(product.getId());
                                update.setStatus(!param.getUnlist() ? LocalProductStatus.UNLIST_FAILURE : LocalProductStatus.PUBLIC_FAILURE);
                                update.setFeature(failed.getErrorDescription());
                                update(update);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[上下架商品异常]", e);
                refreshStatus(param.getUnlist(), productIds, e.getMessage(), false);
            }
        });
    }

    private void refreshStatus(boolean unlist, List<Long> productIds, String message, boolean processing) {
        batchUpdate(productIds.stream().map(p -> {
            ShopeeProductDTO product = new ShopeeProductDTO();
            product.setId(p);
            if (processing) {
                product.setStatus(!unlist ? LocalProductStatus.IN_UNLIST : LocalProductStatus.IN_PUBLIC);
            } else {
                product.setFeature(message);
                product.setStatus(!unlist ? LocalProductStatus.UNLIST_FAILURE : LocalProductStatus.PUBLIC_FAILURE);
            }
            return product;
        }).collect(Collectors.toList()));
    }

    /**
     * 追踪店铺
     */

    @Override
    public List<ShopVM> trackToTheShop(int type, long id) {
        /*
         * 当前 id 平台级别
         * 1. 找站点级
         * 2. 找店铺级
         * 3. get店铺ID
         */
        List<ShopVM> shops = null;
        if (type == ShopeeProduct.Type.PLATFORM.code) {
            shops = childs$(id, ShopeeProduct.Type.PLATFORM_NODE.code).stream()
                    .map(ShopeeProduct::getId)
                    .map(e -> childs$(e, ShopeeProduct.Type.SHOP.code))
                    .flatMap(List::stream)
                    .map(this::fillShopVO)
                    .collect(Collectors.toList());
        } else if (type == ShopeeProduct.Type.PLATFORM_NODE.code) {
            shops = childs$(id, ShopeeProduct.Type.SHOP.code).stream()
                    .map(this::fillShopVO).collect(Collectors.toList());
        }
        return shops;
    }


    @Override
    public List<ShopVM> trackToTheShop(long id) {
        return childs$(id, ShopeeProduct.Type.SHOP.code).stream()
                .map(this::fillShopVO).collect(Collectors.toList());
    }

    private ShopVM fillShopVO(ShopeeProduct e) {
        final ShopeeShopDTO body = uaaService.getShopInfo(e.getShopId()).getBody();

        final ShopVM shopVO = new ShopVM();
        if (body != null && body.getShopName() != null) {
            shopVO.setShopName(body.getShopName());
        } else {
            log.error("根据 shopId  查找 shopName 出错");
            shopVO.setShopName("default");
        }
        shopVO.setShopId(e.getShopId());
        shopVO.setStatus(ShopeeItemStatus.UNKNOWN.equals(e.getShopeeItemStatus()) ? 0 : 1);
        return shopVO;
    }


    @Override
    public Optional<ShopeeProductDTO> selectByItemId(long itemId) {
        return Optional.ofNullable(mapper.toDto(repository.selectByShopeeItemId(itemId)));
    }

    @Override
    public List<ScItemDTO> selectShopeeProductAllInfo(Map<Long, List<Long>> map) {
        if (null != map && map.size() > 0) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start("查询订单商品耗时");
            try {
                List<ScItemDTO> collect = map.entrySet().stream().map(e -> {
                    ScItemDTO scItemDTO = new ScItemDTO();
                    selectByItemId(e.getKey()).ifPresent(shopeeProductDTO -> {
                        Long productId = shopeeProductDTO.getId();
                        List<ShopeeProductSkuDTO> shopeeProductSkuDTOList = shopeeProductSkuService.selectByProductIdAndShopeeVariationIds(productId, e.getValue());
                        List<ShopeeSkuAttributeDTO> shopeeSkuAttributeDTOS = shopeeSkuAttributeService.selectByProductIds(Arrays.asList(productId));
                        Optional<ProductMetaDataDTO> productMetaDataDTO = Optional.empty();
                        if (null != shopeeProductDTO.getMetaDataId()) {
                            productMetaDataDTO = productMetaDataService.findOne(shopeeProductDTO.getMetaDataId());
                        }
                        scItemDTO.setVariationTier(shopeeProductDTO.getVariationTier());
                        VariationVM variationVM = new VariationVM();
                        variationVM.setVariationTier(shopeeProductDTO.getVariationTier());

                        if (productMetaDataDTO.isPresent()) {
                            ProductMetaDataDTO mongoProdcutInfo = productMetaDataDTO.get();
                            scItemDTO.setTitle(mongoProdcutInfo.getName());
                            final AlibabaProductProductInfoPlus productInfo = JSON.parseObject(mongoProdcutInfo.getJson(), AlibabaProductProductInfoPlus.class);
                            List<AlibabaProductSKUAttrInfoPlus> attrInfoPlusList = new ArrayList<>();
                            Arrays.stream(productInfo.getSkuInfos()).forEach(f -> {
                                AlibabaProductSKUAttrInfoPlus[] attributes = f.getAttributes();
                                if (null != attributes) {
                                    Arrays.stream(attributes).forEach(x -> {
                                        if (null != x.getAttributeDisplayName()) {
                                            attrInfoPlusList.add(x);
                                        }
                                    });
                                }
                            });
                            if (null != attrInfoPlusList) {
                                if (shopeeProductDTO.getVariationTier() == 2) {
                                    List<VariationVM.Variation> variations = attrInfoPlusList.stream().filter(distinctByKey(z -> z.getAttributeValue())).collect(Collectors.groupingBy(AlibabaProductSKUAttrInfoPlus::getAttributeDisplayName)).entrySet().stream().map(x -> {
                                        VariationVM.Variation variation = new VariationVM.Variation();
                                        variation.setName(x.getKey());
                                        variation.setOptions(x.getValue().stream().map(AlibabaProductSKUAttrInfoPlus::getAttributeValue).collect(Collectors.toList()));
                                        variation.setProviderName(productInfo.getSupplierLoginId());
                                        return variation;
                                    }).collect(Collectors.toList());
                                    variationVM.setVariations(variations);
                                } else {
                                    variationVM.setVariations(shopeeProductSkuDTOList.stream().map(x -> {
                                        VariationVM.Variation variation = new VariationVM.Variation();
                                        variation.setName(productInfo.getSubject());
                                        variation.setPrice(mongoProdcutInfo.getPrice().floatValue());
                                        variation.setCurrency(x.getCurrency());
                                        variation.setStock(x.getStock());
                                        variation.setSkuCode(x.getSkuCode());
                                        variation.setProductSkuCode(Arrays.asList(x.getSkuCode()));
                                        variation.setWeight(shopeeProductDTO.getWeight() == null ? 0f : Float.valueOf(shopeeProductDTO.getWeight()));
                                        variation.setSkuTitle(productInfo.getSubject());
                                        variation.setProviderName(productInfo.getSupplierLoginId());
                                        try {
                                            variation.setSkuImage(shopeeSkuAttributeDTOS.get(0) == null ? null : shopeeSkuAttributeDTOS.get(0).getImagesUrl().get(0));
                                            variation.setImageUrls(shopeeSkuAttributeDTOS.get(0) == null ? null : shopeeSkuAttributeDTOS.get(0).getImagesUrl());
                                        } catch (Exception ex) {

                                        }
                                        variation.setSourceUrl(CommonUtils.isNotBlank(shopeeProductDTO.getSourceUrl()) ? shopeeProductDTO.getSourceUrl() : mongoProdcutInfo.getUrl());
                                        String feature = x.getFeature();
                                        if (null != feature) {
                                            Map<String, String> json = (Map<String, String>) JSON.parse(feature);
                                            variation.setSpecId(json.get("specId"));
                                        }
                                        return variation;
                                    }).collect(Collectors.toList()));
                                }
                            }
                        } else {
                            scItemDTO.setTitle(shopeeProductDTO.getName());
                            if (shopeeProductDTO.getVariationTier() == 2) {
                                //多sku
                                variationVM.setVariations(shopeeSkuAttributeDTOS.stream().map(x -> {
                                    VariationVM.Variation variation = new VariationVM.Variation();
                                    variation.setName(x.getName());
                                    variation.setOptions(x.getOptions());
                                    return variation;
                                }).collect(Collectors.toList()));
                            } else {
                                variationVM.setVariations(shopeeProductSkuDTOList.stream().map(x -> {
                                    VariationVM.Variation variation = new VariationVM.Variation();
                                    variation.setName(shopeeSkuAttributeDTOS.get(0).getOptions().get(0));
                                    variation.setPrice(x.getPrice());
                                    variation.setCurrency(x.getCurrency());
                                    variation.setStock(x.getStock());
                                    variation.setSkuCode(x.getSkuCode());
                                    variation.setWeight(shopeeProductDTO.getWeight() == null ? 0f : Float.valueOf(shopeeProductDTO.getWeight()));
                                    variation.setSkuTitle(shopeeProductDTO.getName());
                                    variation.setSourceUrl(shopeeProductDTO.getSourceUrl());
                                    try {
                                        variation.setSkuImage(shopeeSkuAttributeDTOS.get(0) == null ? null : shopeeSkuAttributeDTOS.get(0).getImagesUrl().get(0));
                                        variation.setImageUrls(shopeeSkuAttributeDTOS.get(0) == null ? null : shopeeSkuAttributeDTOS.get(0).getImagesUrl());
                                    } catch (Exception ex) {
                                    }
                                    variation.setProductSkuCode(Arrays.asList(x.getSkuCode()));
                                    String feature = x.getFeature();
                                    if (null != feature) {
                                        Map<String, String> json = (Map<String, String>) JSON.parse(feature);
                                        variation.setSpecId(json.get("specId"));
                                    }
                                    return variation;
                                }).collect(Collectors.toList()));
                            }
                        }
                        List<VariationVM.VariationIndex> variationIndices = shopeeProductSkuDTOList.stream().map(y -> {
                            VariationVM.VariationIndex variationIndex = new VariationVM.VariationIndex();
                            variationIndex.setCurrency(y.getCurrency());
                            variationIndex.setIndex(Arrays.asList(y.getSkuOptionOneIndex(), y.getSkuOptionTowIndex()));
                            variationIndex.setPrice(y.getPrice());
                            variationIndex.setStock(y.getStock());
                            variationIndex.setSkuCode(y.getSkuCode());
                            variationIndex.setWeight(shopeeProductDTO.getWeight() == null ? 0f : Float.valueOf(shopeeProductDTO.getWeight()));
                            variationIndex.setSkuTitle(shopeeProductDTO.getName());
                            variationIndex.setSourceUrl(CommonUtils.isNotBlank(shopeeProductDTO.getSourceUrl()) ? shopeeProductDTO.getSourceUrl() : shopeeProductDTO.getCollectUrl());
                            variationIndex.setSkuImage(shopeeSkuAttributeDTOS.get(0).getImagesUrl() == null ? null : shopeeSkuAttributeDTOS.get(0).getImagesUrl().get(y.getSkuOptionOneIndex()));
                            String feature = y.getFeature();
                            if (null != feature) {
                                Map<String, String> json = (Map<String, String>) JSON.parse(feature);
                                variationIndex.setSpecId(json.get("specId"));
                            }
                            String skuValue = "";
                            if (null != y.getSkuOptionTowIndex()) {
                                skuValue = skuValue + "-" + shopeeSkuAttributeDTOS.get(1).getOptions().get(y.getSkuOptionTowIndex());
                            }
                            variationIndex.setSkuValue(skuValue);
                            variationIndex.setProductSkuCode(Arrays.asList(y.getSkuCode()));
                            return variationIndex;
                        }).collect(Collectors.toList());
                        variationVM.setVariationIndexs(variationIndices);
                        scItemDTO.setVariations(variationVM);
                        scItemDTO.setImages(shopeeSkuAttributeDTOS.get(0).getImagesUrl() == null ? new ArrayList<>() : shopeeSkuAttributeDTOS.get(0).getImagesUrl());
                    });
                    return scItemDTO;
                }).collect(Collectors.toList());
                stopWatch.stop();
                log.info(stopWatch.prettyPrint());
                return collect;
            } catch (Exception e) {

            }
        }
        return new ArrayList<>();
    }

    /**
     * 覆盖拷贝到站点
     *
     * @param product 平台商品
     * @param toNode  站点 (跨站处理)
     */
    private Long cover(ShopeeProductDTO product, PlatformNodeDTO toNode) {
        // init status
        init(product);

        final boolean crossStation = !product.getCurrency().equals(toNode.getCurrency());

        final Long productId = product.getId();
        Long copyOfProductId;

        if (crossStation) {
            /*
             * 1. 转换价格
             * 2. 映射属性
             * 4. 台湾站SKU打横
             * 3. 转换SKU价格
             */
            final ShopeeProductDTO copyOfProduct = save(product);
            copyOfProductId = copyOfProduct.getId();
            refreshPrice(copyOfProduct, toNode.getCurrency());

            log.info("[异常诊断] : productId: {}, copyOfProductId: {}, copyOfProduct: {}", productId, copyOfProduct, toNode);

            shopeeProductAttributeValueService.productResetShopeeCategory(productId, copyOfProductId, copyOfProduct.getCategoryId() == null ? 0L : copyOfProduct.getCategoryId(), toNode.getId());

            // 双SKU转台湾站点
//            if (PlatformNodeEnum.SHOPEE_TW.id.equals(toNode.getId()) && ShopeeProduct.VariationTier.TWO.val.equals(product.getVariationTier())) {
//                log.info("[其他站点转台湾站 - 双SKU打横]");
//                shopeeProductSkuService.cover(productId, copyOfProductId, toNode.getCurrency(), true);
//            } else {
            shopeeSkuAttributeService.copyShopeeSkuAttribute(productId, copyOfProductId);
            shopeeProductSkuService.copyShopeeProductSku(productId, copyOfProductId, toNode.getCurrency(), false);
//            }
        } else {
            copyOfProductId = copyProductInfo(product);
        }
        return copyOfProductId;
    }

    /**
     * 根据商品拷贝一份 并返回拷贝后的商品ID
     */
    private Long copyProductInfo(ShopeeProductDTO product) {
        // init status
        init(product);

        final Long productId = product.getId();
        final Long copyOfProductId;
        try {
            copyOfProductId = save(product).getId();
        } catch (Exception e) {
            log.error("保存商品异常: {}", e);
            throw new InternalServiceException(handleProductExceptionInfo.doMessage("internal.service.exception.error.saving.product") + e.getMessage());
        }
        ShopeeProductCopyDTO copyDTO = ShopeeProductCopyDTO.builder()
                .productId(productId)
                .copyOfProductId(copyOfProductId)
                .toTw(false)
                .build();
        shopeeProductAttributeValueService.copyShopeeProductAttributeValue(productId, copyOfProductId);
        shopeeProductSkuService.copyShopeeProductSku(copyDTO);
        shopeeSkuAttributeService.copyShopeeSkuAttribute(productId, copyOfProductId);
        return copyOfProductId;
    }

    /**
     * 将一个商品覆盖到另一个商品 (只需要基本信息和属性)
     *
     * @param product
     * @param coverToProductId
     */
    private Long copyUpdateBasicAndAttribute(ShopeeProductDTO product, long coverToProductId) {
        final Long productId = product.getId();

        product.setParentId(productId);
        product.setStatus(null);
        product.setShopeeItemStatus(null);
        product.setShopeeItemId(null);
        product.setShopId(null);
        product.setType(null);
        product.setOnlineUrl(null);
        product.setGmtCreate(null);
        product.setGmtModified(null);
        product.setFeature(null);
        product.setWarning(null);
        product.setVariationTier(null);
        product.setLoginId(null);

        product.setId(coverToProductId);
        update(product);

        shopeeProductAttributeValueService.copyShopeeProductAttributeValue(productId, coverToProductId);
        return productId;
    }

    /**
     * 将一个商品覆盖到另一个商品
     *
     * @param product
     * @param coverToProductId
     */
    private void copyUpdate(ShopeeProductDTO product, long coverToProductId) {
        final Long productId = copyUpdateBasicAndAttribute(product, coverToProductId);
        shopeeSkuAttributeService.copyShopeeSkuAttribute(productId, coverToProductId);
        ShopeeProductCopyDTO copyDTO = ShopeeProductCopyDTO.builder()
                .productId(productId)
                .copyOfProductId(coverToProductId)
                .toTw(false)
                .build();
        shopeeProductSkuService.copyShopeeProductSku(copyDTO);
    }

    /**
     * 马来(MYR)
     * to 新加坡(SGD) 0.332941d
     * to 印尼(IDR) 3460.7844
     */
    protected void refreshPrice(ShopeeProductDTO product, String toCurrency) {
        final ShopeeProductDTO shopeeProductDTO = new ShopeeProductDTO();
        log.info("[跨站拷贝-处理金额]");

        log.info("[原价格] {}:{}", product.getVPrice(), product.getCurrency());
        ///  如果是 越南  或者马来西亚 就 直接舍弃小数
        final boolean roundDown = PlatformNodeEnum.SHOPEE_ID.currency.equals(toCurrency) || PlatformNodeEnum.SHOPEE_VN.currency.equals(toCurrency);
        Float price = convertPrice(product.getCurrency(), toCurrency, product.getVPrice());
        if (roundDown) {
            price = price.intValue() - 0f;
        }
        if (Objects.nonNull(product.getOriginalPrice())) {
            Float originalPrice = convertPrice(product.getCurrency(), toCurrency, ShopeeUtil.outputPrice(product.getOriginalPrice(), product.getCurrency()));
            if (roundDown) {
                originalPrice = originalPrice.intValue() - 0f;
            }
            shopeeProductDTO.setOriginalPrice(ShopeeUtil.apiInputPrice(originalPrice, toCurrency));
        }


        log.info("[新价格] {}:{}", product.getVPrice(), product.getCurrency());
        shopeeProductDTO.setId(product.getId());
        shopeeProductDTO.setCurrency(toCurrency);
        shopeeProductDTO.setVPrice(price);
        update(shopeeProductDTO);
    }

    @Override
    public Float convertPrice(String from, String to, Float price) {
        final CurrencyRateResult result = currency.currencyConvert(from, to);
        if (result.isSuccess()) {
            return result.getRate().multiply(new BigDecimal(price)).floatValue();
        }
        throw new InternalServiceException(handleProductExceptionInfo.doMessage("internal.service.exception.exchange.rate.conversion.failed"));
    }

    private void init(ShopeeProductDTO product) {
        product.setParentId(product.getId());
        product.setStatus(LocalProductStatus.NO_PUBLISH);
        product.setShopeeItemStatus(ShopeeItemStatus.UNKNOWN);
        product.setGmtCreate(null);
        product.setGmtModified(null);

        /*
         * 线上同步下来的
         */
        if (product.getShopeeItemId() != null && product.getParentId() == null) {
            product.setShopeeItemId(0L);
        }
    }


    @Override
    public List<BatchEditProductVM> batchCopyToPlatformNode(BatchCopyToPlatformNodeParam param) {

        /*
         * 批量保存到站点, 并返回装配好的数据
         */
        List<Long> nodeProductIds = new ArrayList<>(param.getProductIds().size());
        for (Long productId : param.getProductIds()) {
            try {
                final Future<Long> future = criticalExecutor.submit(() -> copyToPlatformNode(productId, param.getPlatformNodeId()));
                if (future.get() != null) {
                    nodeProductIds.add(future.get());
                }
            } catch (Exception e) {
                log.warn("[拷贝商品到站点异常]", e);
            }
        }

        if (param.getShop()) {
            return nodeProductIds.stream().map(e -> {
                final BatchEditProductVM vm = new BatchEditProductVM();
                vm.setId(e);
                return vm;
            }).collect(Collectors.toList());
        } else {
            return batchEditProductVOMapper.toDto(repository.selectBatchIds(nodeProductIds));
        }
    }


    @Override
    public List<BatchEditProductVM> batchGetProduct(List<Long> productIds) {
        return batchEditProductVOMapper.toDto(repository.selectBatchIds(productIds));
    }


    @Override
    public boolean batchSave(ValidList<BatchEditProductVM> products) {
        for (BatchEditProductVM vo : products) {
            checkParam(vo);
            final VariationVM param = vo.getVariationWrapper();
            shopeeProductSkuService.checkParam(
                    param,
                    this.find(param.getProductId()));
        }
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        for (BatchEditProductVM vo : products) {
            executor.execute(() -> update(vo, securityInfo.getCurrentLogin()));
        }
        return true;
    }


    public void update(BatchEditProductVM vo, String login) {
        try {
            log.info("[批量编辑保存] : id: {}", vo.getId());

            final VariationVM variations = vo.getVariationWrapper().setProductId(vo.getId());
            final ShopeeProduct entity = batchEditProductVOMapper.toEntity(vo);

            /*
             * 更新
             */
            repository.updateById(entity);
            variations.setLogin(entity.getLoginId());
            businessShopeeProductSkuService.coverTheProduct(variations, login);
        } catch (Exception e) {
            log.error("[批量保存异常]", e);
        }
    }


    @Override
    public boolean batchSaveToShop(BatchEditToShopParam param) {
        final String currentLogin = SecurityUtils.getCurrentLogin();
        List<Long> shopIds = uaaService.getShopeeShopInfoV2(currentLogin, SecurityUtils.isSubAccount()).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());
        shopIds = param.getShopIds().stream()
                .filter(shopIds::contains).collect(Collectors.toList());
        /*
         * 把多个站点商品 拷贝到店铺
         */
        for (Long productId : param.getProductIds()) {
            for (Long shopId : shopIds) {
                executor.execute(() -> {
                    try {
                        /*
                         * 1. 保存基本信息到站点商品
                         * 2. 保存新的属性值到站点商品
                         * 3. 将站点商品拷贝到店铺商品(已存在 && 未发布则更新)
                         */
                        batchUpdateBasic(productId, param.getCategoryId(), param.getLogistics());
                        shopeeProductAttributeValueService.coverByProduct(getParam(param.getAttributeValues(), productId));
                        copyToShop(productId, shopId, currentLogin);
                    } catch (Exception e) {
                        log.error("[批量保存到店铺异常]", e);
                    }
                });
            }
        }
        return true;
    }

    private ProductAttributeValueParam getParam(List<ShopeeProductAttributeValueDTO> attributeValues, Long productId) {
        final ProductAttributeValueParam valueParam = new ProductAttributeValueParam();
        valueParam.setAttributeValues(attributeValues);
        valueParam.setProductId(productId);
        return valueParam;
    }


    private void batchUpdateBasic(Long productId, Long categoryId, List<ShopeeProductDTO.Logistic> logistics) {
        final ShopeeProductDTO dto = new ShopeeProductDTO();
        dto.setId(productId);
        dto.setCategoryId(categoryId);
        dto.setLogistics(logistics);
        update(dto);
    }

    @Resource
    ShopeeBasicDataServiceImpl shopeeBasicDataService;


    private List<ShopeeProductDTO.Logistic> validLogistic(Long shopId, List<ShopeeProductDTO.Logistic> logistics) {
        if (CollectionUtils.isEmpty(logistics)) {
            return Collections.emptyList();
        }

        final List<ShopeeProductDTO.Logistic> validLogistics = new ArrayList<>();
        final List<LogisticsResult.LogisticsBean> logisticsInfoByShopId = shopeeBasicDataService.getLogisticsInfoByShopId(shopId);
        for (LogisticsResult.LogisticsBean remoteLogistic : logisticsInfoByShopId) {
            for (ShopeeProductDTO.Logistic logistic : logistics) {
                if (logistic.getLogisticId().equals(remoteLogistic.getLogisticId().longValue())) {
                    validLogistics.add(logistic);
                }
            }
        }

        log.info("[筛选 - 物流渠道 - 当前] : {}", validLogistics.toString());
        return validLogistics.stream().peek(e -> e.setEnabled(true)).collect(Collectors.toList());
    }

    @Override
    public boolean batchSavePriceAndStock(BatchSavePriceAndStockParam param) {
        executor.execute(() -> updateProductSkus(param));

        if (param.getItems() != null) {
            executor.execute(() -> handleSingle(param.getItems().stream().collect(Collectors.groupingBy(BatchSavePriceAndStockParam.Item::getShopId))));
        }
        if (param.getVariations() != null) {
            executor.execute(() -> handleSku(param.getVariations().stream().collect(Collectors.groupingBy(BatchSavePriceAndStockParam.Variation::getShopId))));
        }
        return true;
    }

    private void updateProductSkus(BatchSavePriceAndStockParam param) {
        if (param.getItems() != null) {
            final List<ShopeeProductSkuDTO> productSkus = param.getItems().stream().map(e -> {
                final Optional<ShopeeProductDTO> productExist = findByItemIdAndShopId(e.getItemId(), e.getShopId());
                if (productExist.isPresent()) {
                    final ShopeeProductSkuDTO productSku = shopeeProductSkuService.pageByProduct(productExist.get().getId(), new Page(0, Integer.MAX_VALUE)).getRecords().get(0);
                    fillStockAndPrice(productSku, e.getStock(), e.getPrice());
                    return productSku;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            shopeeProductSkuService.batchUpdate(productSkus);
        }

        if (param.getVariations() != null) {
            final List<ShopeeProductSkuDTO> productSkus = param.getVariations().stream().map(e -> {
                final Optional<ShopeeProductSkuDTO> productSkuExist = shopeeProductSkuService.find(e.getId());
                if (productSkuExist.isPresent()) {
                    final ShopeeProductSkuDTO productSku = productSkuExist.get();
                    fillStockAndPrice(productSku, e.getStock(), e.getPrice());
                    return productSku;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            shopeeProductSkuService.batchUpdate(productSkus);
        }
    }

    private void fillStockAndPrice(ShopeeProductSkuDTO productSku, Integer stock, Float price) {
        if (productSku.getStock() != null) {
            productSku.setStock(stock);
        }
        if (productSku.getPrice() != null) {
            productSku.setPrice(price);
        }
    }

    private void handleSku(Map<Long, List<BatchSavePriceAndStockParam.Variation>> variationMap) {
        try {
            for (Map.Entry<Long, List<BatchSavePriceAndStockParam.Variation>> variation : variationMap.entrySet()) {
                final Long shopId = variation.getKey();

                final List<UpdateVariationPriceBatchVariation> updateVariationPriceBatchVariations = new ArrayList<>(variation.getValue().size());
                final List<UpdateVariationStockBatchVariation> updateVariationStockBatchVariations = new ArrayList<>(variation.getValue().size());

                variation.getValue().forEach(e -> {
                    if (e.getPrice() != null && e.getPrice() > 0) {
                        final UpdateVariationPriceBatchVariation updatePriceParam = new UpdateVariationPriceBatchVariation();
                        updatePriceParam.setItemId(e.getItemId());
                        updatePriceParam.setVariationId(e.getVariationId());
                        updatePriceParam.setPrice(e.getPrice());
                        updateVariationPriceBatchVariations.add(updatePriceParam);
                    }
                    if (e.getStock() != null && e.getStock() > 0) {
                        final UpdateVariationStockBatchVariation updateStockParam = new UpdateVariationStockBatchVariation();
                        updateStockParam.setItemId(e.getItemId());
                        updateStockParam.setVariationId(e.getVariationId());
                        updateStockParam.setStock(e.getStock());
                        updateVariationStockBatchVariations.add(updateStockParam);
                    }
                });

                if (updateVariationPriceBatchVariations.size() != 0) {
                    executor.execute(() -> {
                        try {
                            final ShopeeResult<UpdateVariationBatchResult> updatePriceResult = itemApi.updateVariationPriceBatch(shopId, updateVariationPriceBatchVariations);
                            if (updatePriceResult.isResult()) {
                                for (UpdateVariationBatchResult.BatchResult batchResult : updatePriceResult.getData().getBatchResult()) {

                                    for (UpdateVariationBatchResult.BatchResult.Failure failure : batchResult.getFailures()) {
                                        updateVariationPriceBatchVariations.stream().filter(e -> e.getVariationId().equals(failure.getVariationId())).findFirst().ifPresent(e ->
                                                shopeeProductSkuService.findByVariationId(e.getVariationId()).ifPresent(v -> {
                                                    String errorDescription = failure.getErrorDescription();
                                                    String feature = v.getFeature();
                                                    //错误信息 添加到spec字段
                                                    v.setFeature(FeatrueUtil.addFeature(feature, "error", errorDescription));
                                                    shopeeProductSkuService.update(v);
                                                }));
                                    }
                                    for (UpdateVariationBatchResult.BatchResult.Modification modification : batchResult.getModifications()) {
                                        final String currency = getCurrencyByShopIdAndItemId(shopId, modification.getItemId());
                                        updateVariationPriceBatchVariations.stream().filter(e -> e.getVariationId().equals(modification.getVariationId())).findFirst().ifPresent(e ->
                                                shopeeProductSkuService.findByVariationId(e.getVariationId()).ifPresent(v -> {
                                                    v.setPrice(e.getPrice());
                                                    v.setCurrency(currency);
                                                    shopeeProductSkuService.update(v);
                                                }));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("[批量编辑价格异常]", e);
                        }
                    });
                }

                if (updateVariationStockBatchVariations.size() != 0) {
                    executor.execute(() -> {
                        try {
                            final ShopeeResult<UpdateVariationBatchResult> updateStockResult = itemApi.updateVariationStockBatch(shopId, updateVariationStockBatchVariations);
                            if (updateStockResult.isResult()) {
                                for (UpdateVariationBatchResult.BatchResult batchResult : updateStockResult.getData().getBatchResult()) {
                                    for (UpdateVariationBatchResult.BatchResult.Failure failure : batchResult.getFailures()) {
                                        updateVariationPriceBatchVariations.stream().filter(e -> e.getVariationId().equals(failure.getVariationId())).findFirst().ifPresent(e ->
                                                shopeeProductSkuService.findByVariationId(e.getVariationId()).ifPresent(v -> {
                                                    v.setFeature(FeatrueUtil.addFeature(v.getFeature(), "error", failure.getErrorDescription()));
                                                    shopeeProductSkuService.update(v);
                                                }));
                                    }
                                    for (UpdateVariationBatchResult.BatchResult.Modification modification : batchResult.getModifications()) {
                                        updateVariationStockBatchVariations.stream().filter(e -> e.getVariationId().equals(modification.getVariationId())).findFirst().ifPresent(e ->
                                                shopeeProductSkuService.findByVariationId(e.getVariationId()).ifPresent(v -> {
                                                    v.setStock(e.getStock());
                                                    shopeeProductSkuService.update(v);
                                                }));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("[批量保存库存异常]", e);
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("[处理双SKU异常]", e);
        }
    }

    private void handleSingle(Map<Long, List<BatchSavePriceAndStockParam.Item>> itemMap) {
        try {
            for (Map.Entry<Long, List<BatchSavePriceAndStockParam.Item>> item : itemMap.entrySet()) {
                final Long shopId = item.getKey();

                final List<UpdatePriceBatchItem> updatePriceBatchItems = new ArrayList<>(item.getValue().size());
                final List<UpdateStockBatchItem> updateStockBatchItems = new ArrayList<>(item.getValue().size());

                item.getValue().forEach(e -> {
                    if (e.getPrice() != null && e.getPrice() > 0) {
                        final UpdatePriceBatchItem newPriceParam = new UpdatePriceBatchItem();
                        newPriceParam.setItemId(e.getItemId());
                        newPriceParam.setPrice(e.getPrice());
                        updatePriceBatchItems.add(newPriceParam);
                    }
                    if (e.getStock() != null && e.getStock() > 0) {
                        final UpdateStockBatchItem newStockParam = new UpdateStockBatchItem();
                        newStockParam.setItemId(e.getItemId());
                        newStockParam.setStock(e.getStock());
                        updateStockBatchItems.add(newStockParam);
                    }
                });

                if (updatePriceBatchItems.size() != 0) {
                    executor.execute(() -> handleUpdatePriceResult(shopId, updatePriceBatchItems, itemApi.updatePriceBatch(shopId, updatePriceBatchItems)));
                }
                if (updateStockBatchItems.size() != 0) {
                    executor.execute(() -> handleUpdateStockResult(shopId, updateStockBatchItems, itemApi.updateStockBatch(shopId, updateStockBatchItems)));
                }
            }
        } catch (Exception e) {
            log.error("[处理单SKU异常]", e);
        }
    }

    private void handleUpdateStockResult(Long shopId, List<UpdateStockBatchItem> updateStockBatchItems, ShopeeResult<UpdateBatchResult> updateStockResult) {
        try {
            if (updateStockResult.isResult()) {
                final List<UpdateBatchResult.BatchResultBean.FailuresBean> failures = updateStockResult.getData().getBatchResult().getFailures();
                for (UpdateBatchResult.BatchResultBean.FailuresBean failure : failures) {
                    findByItemIdAndShopId(failure.getItemId(), shopId).ifPresent(e -> {
                        e.setFeature(failure.getErrorDescription());
                        update(e);
                    });
                }

                final List<UpdateBatchResult.BatchResultBean.ModificationsBean> modifications = updateStockResult.getData().getBatchResult().getModifications();
                for (UpdateBatchResult.BatchResultBean.ModificationsBean modification : modifications) {
                    updateStockBatchItems.stream()
                            .filter(e -> e.getItemId().equals(modification.getItemId()))
                            .findFirst().ifPresent(e -> findByItemIdAndShopId(e.getItemId(), shopId).ifPresent(v -> {
                        v.setStock(e.getStock());
                        update(v);

                        final ShopeeProductSkuDTO productSku = shopeeProductSkuService.pageByProduct(v.getId(), new Page(0, Integer.MAX_VALUE)).getRecords().get(0);
                        productSku.setStock(e.getStock());
                        shopeeProductSkuService.update(productSku);
                    }));
                }
            }
        } catch (Exception e) {
            log.error("[处理批量更新库存异常]", e);
        }
    }

    private void handleUpdatePriceResult(Long shopId, List<UpdatePriceBatchItem> updatePriceBatchItems, ShopeeResult<UpdateBatchResult> updatePriceResult) {
        try {
            if (updatePriceResult.isResult()) {
                final List<UpdateBatchResult.BatchResultBean.FailuresBean> failures = updatePriceResult.getData().getBatchResult().getFailures();
                for (UpdateBatchResult.BatchResultBean.FailuresBean failure : failures) {
                    findByItemIdAndShopId(failure.getItemId(), shopId).ifPresent(e -> {
                        e.setFeature(failure.getErrorDescription());
                        update(e);
                    });
                }

                final List<UpdateBatchResult.BatchResultBean.ModificationsBean> modifications = updatePriceResult.getData().getBatchResult().getModifications();
                for (UpdateBatchResult.BatchResultBean.ModificationsBean modification : modifications) {
                    final String currency = getCurrencyByShopIdAndItemId(shopId, modification.getItemId());

                    updatePriceBatchItems.stream()
                            .filter(e -> e.getItemId().equals(modification.getItemId()))
                            .findFirst().ifPresent(e -> findByItemIdAndShopId(e.getItemId(), shopId).ifPresent(v -> {
                        v.setVPrice(e.getPrice());
                        v.setCurrency(currency);
                        update(v);

                        final ShopeeProductSkuDTO productSku = shopeeProductSkuService.pageByProduct(v.getId(), new Page(0, Integer.MAX_VALUE)).getRecords().get(0);
                        productSku.setPrice(e.getPrice());
                        productSku.setCurrency(currency);
                        shopeeProductSkuService.update(productSku);
                    }));
                }
            }
        } catch (Exception e) {
            log.error("[处理批量更新价格异常]", e);
        }
    }


    @Override
    public List<String> listBySource(String loginId) {
        return repository.findAllBySource(loginId);
    }


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public IPage<ProductListVM_V2> searchProductByCurrentUserV2(String loginId, ProductSearchStrategyDTO
            productSearchStrategy, Page page) {
        fill(loginId, productSearchStrategy);
        // 看是否根据 sku搜索
        if (productSearchStrategy.isSku()) {
            final IPage<ShopeeProductSkuDTO> skuListByloginIdAndSkuCode = shopeeProductSkuService.findSkuListByloginIdAndSkuCode(page, productSearchStrategy.getKeyword());
            if (CollectionUtils.isEmpty(skuListByloginIdAndSkuCode.getRecords())) {
                return new Page();
            }
            productSearchStrategy.setProductIds(skuListByloginIdAndSkuCode.getRecords().stream().map(ShopeeProductSkuDTO::getProductId).collect(Collectors.toSet()));
        }
        if (CommonUtils.isNotBlank(productSearchStrategy.getUnpublishShops())) {
            productSearchStrategy.setCollectProductIds(repository.queryUnpublish(loginId, productSearchStrategy.getUnpublishShops()));
        }
        final IPage<ProductListVM_V2> productListVM_v2IPage = repository.searchV2(page, loginId, SecurityUtils.currentLogin(), productSearchStrategy).convert(platformProductListMapper_v2::toProductListV2);
        List<Long> productIds = productListVM_v2IPage.getRecords().stream().map(ProductListVM_V2::getId).collect(Collectors.toList());
        //填充sku
        Map<Long, List<VariationMV_V2>> shopeeProductSKUDTOMap = shopeeProductSkuService.selectVariationMV_V2ByProductIds(productIds)
                .stream()
                .collect(Collectors.groupingBy(VariationMV_V2::getProductId));

        productListVM_v2IPage.getRecords().forEach(productListVM_v2 -> {
            productListVM_v2.setVariations(shopeeProductSKUDTOMap.get(productListVM_v2.getId()));
        });

        //填充站点商品数量
        List<ProductListVM_V2> records = productListVM_v2IPage.getRecords();
        List<Long> parentIds = records.stream().map(ProductListVM_V2::getId).collect(Collectors.toList());
        if (CommonUtils.isNotBlank(parentIds)) {
            List<ChildCount> childCounts = repository.childCountBatch(parentIds);
            if (CommonUtils.isNotBlank(childCounts)) {
                records.forEach(e -> {
                    childCounts.stream()
                            .filter(f -> Objects.equals(e.getId(), f.getId()))
                            .findFirst()
                            .ifPresent(f -> e.setHasChild(f.getCounts()));
                });
            }
        }
        try {
            //填充店铺信息
            Map<Long, List<ShopVM>> shopeeShopListMap = getShopsByProductIdsV2(SecurityUtils.getCurrentLogin(), SecurityUtils.isSubAccount(), productIds);
            productListVM_v2IPage.getRecords().forEach(
                    productListVM_v2 -> productListVM_v2.setShops(shopeeShopListMap.get(productListVM_v2.getId()))
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        //填充图片，兼容处理
        productListVM_v2IPage.getRecords().forEach(shopeeProduct -> {
            ShopeeProductMediaDTO shopeeProductMediaDTO = shopeeProductMediaService.selectByProductId(shopeeProduct.getId());
            if (null != shopeeProductMediaDTO) {
                shopeeProduct.setImages(shopeeProductMediaDTO.getImages());
            }
        });
        return productListVM_v2IPage;
    }

    /**
     * 通过平台产品ID获取该平台产品已发布或已保存的店铺
     *
     * @param login
     * @return
     */
    private Map<Long, List<ShopVM>> getShopsByProductIds(String login, Boolean isSubAccount, List<Long> productIds) {

        List<ShopeeShopDTO> shopeeShopDTOS = uaaService.getShopeeShopInfoV2(login, isSubAccount).getBody();
        Map<Long, List<ShopVM>> shopListMap = new HashMap<>();
        if (null != shopeeShopDTOS && !shopeeShopDTOS.isEmpty()) {
            productIds.forEach(productId -> {
                List<ShopVM> shopVMS = repository.getShops(productId)
                        .stream()
                        .map(shopeeProduct -> shopeeShopDTOS
                                .stream()
                                .filter(shopeeShopDTO -> shopeeShopDTO.getShopId().equals(shopeeProduct.getShopId()))
                                .findFirst()
                                .map(shopeeShopDTO -> {
                                    final ShopVM shopVO = new ShopVM();
                                    shopVO.setShopName(shopeeShopDTO.getShopName());
                                    shopVO.setShopId(shopeeShopDTO.getShopId());
                                    shopVO.setStatus(ShopeeItemStatus.UNKNOWN.equals(shopeeProduct.getShopeeItemStatus()) ? 0 : 1);
                                    return shopVO;
                                }).orElse(new ShopVM())).collect(Collectors.toList());
                shopListMap.put(productId, shopVMS);
            });
        }
        return shopListMap;
    }

    /**
     *
     * @param login 用户登录名称
     * @param isSubAccount
     * @param productIds 查询商品编号
     * @return
     */
    private Map<Long, List<ShopVM>> getShopsByProductIdsV2(String login, Boolean isSubAccount, List<Long> productIds) {
        Map<Long, List<ShopVM>> shopListMap = new HashMap<>();
        //查询用户店铺信息
        List<ShopeeShopDTO> shopeeShopDTOS = uaaService.getShopeeShopInfoV2(login, isSubAccount).getBody();
        //判断是否存在店铺
        if (null != shopeeShopDTOS && !shopeeShopDTOS.isEmpty()) {
            //遍历商品信息
            productIds.forEach(productId ->{
                List<ShopVM> shopVMS = repository.getShopsByMate(productId)
                        .stream()
                        .map(shopeeProduct -> shopeeShopDTOS
                                .stream()
                                .filter(shopeeShopDTO -> shopeeShopDTO.getShopId().equals(shopeeProduct.getShopId()))
                                .findFirst()
                                .map(shopeeShopDTO -> {
                                    final ShopVM shopVO = new ShopVM();
                                    shopVO.setShopName(shopeeShopDTO.getShopName());
                                    shopVO.setShopId(shopeeShopDTO.getShopId());
                                    shopVO.setStatus(ShopeeItemStatus.UNKNOWN.equals(shopeeProduct.getShopeeItemStatus()) ? 0 : 1);
                                    return shopVO;
                                }).orElse(new ShopVM())).collect(Collectors.toList());
                shopListMap.put(productId, shopVMS);
            });
        }
        return shopListMap;
    }


    @Override
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public IPage<ProductListVM_V21> searchShopProductByCurrentUser(ProductSearchStrategyDTO productSearchStrategy) {
        productSearchStrategy.setType(Type.SHOP.code);
        final String loginId = SecurityUtils.currentLogin();
        final Page page = new Page(productSearchStrategy.getPage() + 1, productSearchStrategy.getSize());
        fill(loginId, productSearchStrategy);
        // 看是否根据 sku搜索
        if (productSearchStrategy.isSku()) {
            final IPage<ShopeeProductSkuDTO> skuListByloginIdAndSkuCode = shopeeProductSkuService.findSkuListByloginIdAndSkuCode(page, productSearchStrategy.getKeyword());
            if (CollectionUtils.isEmpty(skuListByloginIdAndSkuCode.getRecords())) {
                return new Page();
            }
            productSearchStrategy.setProductIds(skuListByloginIdAndSkuCode.getRecords().stream().map(ShopeeProductSkuDTO::getProductId).collect(Collectors.toSet()));
        }
        //查看是否根据客优云类目搜索
        if (CommonUtils.isNotBlank(productSearchStrategy.getKyyCategoryIdList())){
            //根据类目获取productIds
            Set<Long> productIdList = kyyCategoryRelationService.selectProductIdByCategorys(productSearchStrategy.getKyyCategoryIdList(), loginId);
            Set<Long> productIds = productSearchStrategy.getProductIds();
            if (CommonUtils.isNotBlank(productIds)){
                productIdList.addAll(productIds);
            }
            productSearchStrategy.setProductIds(productIdList);
        }
        final IPage<ShopeeProduct> search = repository.searchShopProductByLoginId(page, loginId, productSearchStrategy);
        IPage<ProductListVM_V21> productListVMV21IPage = getProductListVM_v21IPage(search);
        settingRequiredInfo(productSearchStrategy, loginId, productListVMV21IPage);
        return productListVMV21IPage;
    }

    private boolean isSearchSkuCode(ProductSearchStrategyDTO productSearchStrategy) {
        if (Objects.equals(ProductSearchStrategyDTO.fields[1], productSearchStrategy.getField()) && StringUtils.isNotBlank(productSearchStrategy.getKeyword())) {
            productSearchStrategy.setField(null);
            return true;
        }
        if (Objects.equals(productSearchStrategy.getField(), ProductSearchStrategyDTO.fields[3])) {
            productSearchStrategy.setField(ProductSearchStrategyDTO.fields[1]);
        }
        return false;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void settingRequiredInfo(ProductSearchStrategyDTO productSearchStrategy, String loginId, IPage<ProductListVM_V21> productListVMV21IPage) {
        List<Long> productIds = productListVMV21IPage.getRecords().stream().map(ProductListVM_V2::getId).collect(Collectors.toList());
//        stopWatch.start("  sku ");
        //填充sku
        this.settingSku(productListVMV21IPage, productIds);
//        stopWatch.stop();
//        stopWatch.start(" 填充图片，兼容处理");
        //填充图片，兼容处理
        this.settingMediaInfo(productListVMV21IPage, productIds);
//        stopWatch.stop();
//        stopWatch.start(" shopName");
        try {
            this.settingShopName(loginId, productListVMV21IPage);
        } catch (Exception e) {
            log.error("[通过UAA拿店铺名称错误]: loginId [{}]  , productIds [{}] , errorMessage [{}] \n {}", loginId, productIds, e.getMessage(), e);
        }
//        stopWatch.stop();
//        stopWatch.start(" boost");
        if ((Objects.nonNull(productSearchStrategy.getOnline()) && productSearchStrategy.getOnline()) ||
                productSearchStrategy.getBoost()) {
            this.settingBoost(productListVMV21IPage, productIds);
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void settingBoost(IPage<ProductListVM_V21> productListVMV21IPage, List<Long> productIds) {
        final List<BoostItem> boostItems = batchBoostItemService.selectPartBoostItemByProductIds(productIds);
        productListVMV21IPage.getRecords().forEach(entity -> {
            boostItems.forEach(boostItem -> {
                if (Objects.equals(boostItem.getProductId(), entity.getId())) {
                    entity.setToppingStatusCode(boostItem.getStatus());
                    entity.setBoost(Boolean.TRUE);
                }
            });
        });
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void settingShopName(String loginId, IPage<ProductListVM_V21> productListVMV21IPage) {
        final List<Long> collect = productListVMV21IPage.getRecords().stream().map(ProductListVM_V21::getShopId).distinct().collect(Collectors.toList());
        if (!collect.isEmpty()) {
            final List<ShopeeShopDTO> body = uaaService.getShopName(collect, loginId).getBody();
            if (!body.isEmpty()) {
                productListVMV21IPage.getRecords().forEach(productListVM_v21 -> {
                    boolean isShop = true;
                    final Long shopId = productListVM_v21.getShopId();
                    for (ShopeeShopDTO shopeeShopDTO : body) {
                        if (Objects.equals(shopId, shopeeShopDTO.getShopId())) {
                            productListVM_v21.setShopName(shopeeShopDTO.getShopName());
                            isShop = false;
                        }
                    }
                    if (isShop) {
                        productListVM_v21.setShopName("Undefined");
                    }
                });
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void settingMediaInfo(IPage<ProductListVM_V21> productListVMV21IPage, List<Long> productIds) {
        final List<ShopeeProductMediaDTO> shopeeProductMediaDTOS = shopeeProductMediaService.selectMainImagsByProductId(productIds);
        if (!shopeeProductMediaDTOS.isEmpty()) {
            productListVMV21IPage.getRecords().parallelStream().forEach(shopeeProduct -> {
                for (ShopeeProductMediaDTO shopeeShopDTO : shopeeProductMediaDTOS) {
                    if (Objects.equals(shopeeProduct.getId(), shopeeShopDTO.getProductId())) {
                        shopeeProduct.setImages(shopeeShopDTO.getImages());
                        shopeeProduct.setCondition(shopeeShopDTO.getIsCondition());
                    }
                }
            });
        }

        final List<ShopeeProductExtraInfoDto> ShopeeProductExtraInfoDtos = shopeeProductExtraInfoService.selectMainFinalInfoByProductId(productIds);
        if (!ShopeeProductExtraInfoDtos.isEmpty()) {
            productListVMV21IPage.getRecords().parallelStream().forEach(shopeeProduct -> {
                for (ShopeeProductExtraInfoDto mediaFinalDto : ShopeeProductExtraInfoDtos) {
                    if (Objects.equals(shopeeProduct.getId(), mediaFinalDto.getProductId())) {
                        shopeeProduct.setSales(mediaFinalDto.getSales());
                        shopeeProduct.setViews(mediaFinalDto.getViews());
                        shopeeProduct.setLikes(mediaFinalDto.getLikes());
                        shopeeProduct.setCmtCount(mediaFinalDto.getCmtCount());
                        shopeeProduct.setRatingStar(mediaFinalDto.getRatingStar());
                    }
                }
            });
        }

    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void settingSku(IPage<ProductListVM_V21> productListVMV21IPage, List<Long> productIds) {
        Map<Long, List<VariationMV_V2>> shopeeProductSKUDTOMap = shopeeProductSkuService.selectVariationMV_V2ByProductIds(productIds)
                .stream()
                .collect(Collectors.groupingBy(VariationMV_V2::getProductId));
        productListVMV21IPage.getRecords().forEach(productListVM_v2 -> {
            productListVM_v2.setVariations(shopeeProductSKUDTOMap.get(productListVM_v2.getId()));
            try {
                fillingDiscount(productListVM_v2);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    /**
     * 将对应的 转换成 特定的类
     *
     * @param search
     * @return
     */
    @Override
    public IPage<ProductListVM_V21> getProductListVM_v21IPage(IPage<ShopeeProduct> search) {
        return new Page<ProductListVM_V21>()
                .setTotal(search.getTotal())
                .setCurrent(search.getCurrent())
                .setPages(search.getPages())
                .setSize(search.getSize())
                .setRecords(platformProductListMapper_v2.toProductListV3(search.getRecords()));
    }


    @Override
    public void fill(String loginId, ProductSearchStrategyDTO productSearchStrategy) {
        if (ShopeeProduct.Type.SHOP.code.equals(productSearchStrategy.getType()) && (productSearchStrategy.getPublishShops() == null || productSearchStrategy.getPublishShops().size() == 0)) {
            final SecurityInfo securityInfo = SecurityUtils.genInfo();
            List<ShopeeShopDTO> body = uaaService.getShopeeShopInfoV2(securityInfo.getCurrentLogin(), securityInfo.isSubAccount()).getBody();
            ;
            if (CommonUtils.isNotBlank(body)) {
                final List<Long> shopIds = body.stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());
                productSearchStrategy.setPublishShops(shopIds);
            } else {
                productSearchStrategy.setPublishShops(Arrays.asList(-1L));
            }
        }
        if (StringUtils.isNotBlank(productSearchStrategy.getField())) {
            Arrays.stream(ProductSearchStrategyDTO.fields).filter(field -> Objects.equals(productSearchStrategy.getField(), field)).findFirst().orElseThrow(() -> new IllegalOperationException("illegal.operation.exception"));
        }
        productSearchStrategy.setSku(isSearchSkuCode(productSearchStrategy));

        if (!CollectionUtils.isEmpty(productSearchStrategy.getSubAccounts())) {
            final SecurityInfo securityInfo = SecurityUtils.genInfo();
            List<ClientUserDTO> body = uaaService.queryAllSubAccount(securityInfo.getCurrentLogin()).getBody();
            if (body != null) {
                final List<String> loginIds = body.stream().map(ClientUserDTO::getLogin).collect(Collectors.toList());
                productSearchStrategy.setSubAccounts(loginIds.stream().filter(t -> productSearchStrategy.getSubAccounts().contains(t)).collect(Collectors.toList()));
            } else {
                productSearchStrategy.setSubAccounts(null);
            }
        }

        if (StringUtils.isNotBlank(productSearchStrategy.getField())) {
            Arrays.stream(ProductSearchStrategyDTO.fields).filter(field -> Objects.equals(productSearchStrategy.getField(), field)).findFirst().orElseThrow(() -> new IllegalOperationException("illegal.operation.exception"));
        }
        if (StringUtils.isBlank(productSearchStrategy.getStartDate()) || StringUtils.isBlank(productSearchStrategy.getEndDate())) {
            productSearchStrategy.setStartDate(null);
            productSearchStrategy.setEndDate(null);
        }
        if (StringUtils.isBlank(productSearchStrategy.getUstartDate()) || StringUtils.isBlank(productSearchStrategy.getUendDate())) {
            productSearchStrategy.setUstartDate(null);
            productSearchStrategy.setUendDate(null);
        }
        if (StringUtils.isNotBlank(productSearchStrategy.getKeyword())) {
            productSearchStrategy.setKeyword(productSearchStrategy.getKeyword().replaceAll("'", "_"));
        }
    }

    @Override
    public List<ProductListVM_V2> getNodeChilds(Long productId) {

        List<ProductListVM_V2> productListVM_v2List = platformProductListMapper_v2.toDto(childs$(productId, Type.PLATFORM_NODE.code));
        productListVM_v2List.forEach(productListVM_v2 -> {
            ShopeeProductMediaDTO shopeeProductMediaDTO = shopeeProductMediaService.selectByProductId(productListVM_v2.getId());
            if (null != shopeeProductMediaDTO) {
                productListVM_v2.setImages(shopeeProductMediaDTO.getImages());
            }
        });

        List<Long> productIds = productListVM_v2List.stream().map(ProductListVM_V2::getId).collect(Collectors.toList());
//        stopWatch.start("  sku ");
        //填充sku
        Map<Long, List<VariationMV_V2>> shopeeProductSKUDTOMap = shopeeProductSkuService.selectVariationMV_V2ByProductIds(productIds)
                .stream()
                .collect(Collectors.groupingBy(VariationMV_V2::getProductId));
        productListVM_v2List.forEach(productListVM_v2 -> {
            productListVM_v2.setVariations(shopeeProductSKUDTOMap.get(productListVM_v2.getId()));
            try {
                fillingDiscount(productListVM_v2);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
        return productListVM_v2List;
    }

    @Override
    public List<ShopVM> getShops(Long productId) {
        return repository.getShops(productId).stream()
                .map(this::fillShopVO)
                .collect(Collectors.toList());
    }

    @Override
    public String getCurrencyByShopIdAndItemId(Long shopId, Long itemId) {
        return repository.getCurrencyByShopIdAndItemId(shopId, itemId);
    }

    @Override
    public String getLoginId(Long id) {
        return repository.getLoginId(id);
    }

    @Override
    public String getCurrencyById(Long id) {
        return repository.getCurrencyById(id);
    }

    @Override
    public int childCount(Long productId) {
        return repository.childCount(productId);
    }

    @Override
    public Long pending(String login) {
        return repository.pending(login);
    }

    @Override
    public Long fail(String login) {
        return repository.fail(login);
    }

    static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public HomeTodoVO todo() {
        String login = SecurityUtils.currentLogin();
        Long pending = pending(login);
        Long fail = fail(login);
        HomeTodoVO.Item item = new HomeTodoVO.Item().setFail(fail).setPending(pending);
        return new HomeTodoVO().setItem(item);
    }

}
