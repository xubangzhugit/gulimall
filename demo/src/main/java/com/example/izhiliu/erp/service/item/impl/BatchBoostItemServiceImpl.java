package com.izhiliu.erp.service.item.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.izhiliu.config.produer.CallbackMQProduerVariable;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.erp.config.DingDingInfoMessageSendr;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.domain.item.BoostItem;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.repository.item.BoostItemRepository;
import com.izhiliu.erp.service.discount.impl.DiscountPriceServiceImpl;
import com.izhiliu.erp.service.item.BatchBoostItemService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.cache.BoostItemCacheService;
import com.izhiliu.erp.service.item.dto.*;
import com.izhiliu.erp.service.item.mapper.BatchTimingBoostItemMapper;
import com.izhiliu.erp.service.mq.producer.MQProducerService;
import com.izhiliu.erp.web.rest.item.param.ShopeeProductMoveParam;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM_V21;
import com.izhiliu.feign.client.DariusService;
import com.izhiliu.model.ProductFeatureDTO;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.param.GetItemBoostParam;
import com.izhiliu.open.shopee.open.sdk.api.item.param.InsertItemBoostParam;
import com.izhiliu.open.shopee.open.sdk.api.item.result.GetItemBootsResult;
import com.izhiliu.open.shopee.open.sdk.api.item.result.InsertItemBootsResult;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ShopDTO;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
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
public class BatchBoostItemServiceImpl extends IBaseServiceImpl<BoostItem, BoostItemDTO, BoostItemRepository, BatchTimingBoostItemMapper> implements BatchBoostItemService, CallbackMQProduerVariable.Tag.BOOST {


    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    RedisLockHelper redisLockHelper;

    @Resource
    ItemApi itemApi;

    @Resource
    BoostItemCacheService boostItemCacheService;
    @Resource
    ShopeeProductService shopeeProductService;

    @Resource
    MQProducerService mqProducerService;

    @Resource
    UaaService uaaService;

    private int MAX_BOOST_ITEM_COUNT = 5;


    LoadingCache<Long, String> shopIdLoginMap = CacheBuilder.newBuilder()
            //缓存池大小，在缓存项接近该大小时， Guava开始回收旧的缓存项
            .maximumSize(10000)
            //设置时间对象写访问则对象从内存中删除(在另外的线程里面不定期维护)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            //移除监听器,缓存项被移除时会触发
            .build(new CacheLoader<Long, String>() {
                @Override
                public String load(Long aLong) throws Exception {
                    return "";
                }
            });

    //    @Resource
//    @Qualifier("taskExecutor")
    Executor executor = DiscountPriceServiceImpl.executorService;

    public static ExecutorService executorService = new ThreadPoolExecutor(
            20,
            100,
            10,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<Runnable>(10000), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());

    @Resource
    DingDingInfoMessageSendr dingDingInfoMessageSendr;
    private String keySubscribeProfix = "STimingTopOffer:subscribe2";
    private String keyResetProfix = "STimingTopOffer:reset2";
    private String keyTimeProfix = "erp:lux:boost:dateTime:luck";

    @Override
    public Boolean timingTopping(List<Long> longs) {
        final List<ShopeeProduct> list = getCheckParam(longs);
        final List<BoostItemDTO> collect = getCheck(list, boostItemDTOS ->
                boostItemDTOS.stream().filter(boostItemDTO -> Objects.isNull(repository.findProductId(boostItemDTO.getProductId()))).collect(Collectors.toList())
        );
        if (!collect.isEmpty()) {
            batchSave(collect);
        }
        return true;
    }

    @Override
    public Boolean deletetimingTopping(List<Long> deleteId) {
        final List<ShopeeProduct> checkParam = getCheckParam(deleteId);
        final Set<Long> collect = checkParam.stream().filter(Objects::nonNull).map(ShopeeProduct::getId).collect(Collectors.toSet());
        if (!collect.isEmpty()) {
            repository.deleteBatchProductIds(collect);
        }
        return true;
    }


    @Override
    public BoostItem findProductId(Long productId) {
        BoostItem boostItem = repository.findProductId(productId);
        return boostItem;
    }

    @Override
    public List<BoostItem> selectPartBoostItemByProductIds(List<Long> productIds) {
        if (!productIds.isEmpty()) {
            return repository.selectPartBoostItemByProductIds(productIds);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public IPage<ProductListVM_V21> searchShopBoostProductByCurrentUser(ProductSearchStrategyDTO productSearchStrategy) {
        final String currentLogin = SecurityUtils.currentLogin();
        shopeeProductService.fill(currentLogin, productSearchStrategy);
        final IPage<ShopeeProduct> boostItemDTOS = selectPartBoostItemByShopId(
                new Page(productSearchStrategy.getPage() + 1, productSearchStrategy.getSize())
                , currentLogin
                , Objects.isNull(productSearchStrategy.getBoostStatus()) ? null : BoostItem.Status.getStatus(productSearchStrategy.getBoostStatus()).getCode()
                , productSearchStrategy
        );
        if (Objects.isNull(boostItemDTOS) || CollectionUtils.isEmpty(boostItemDTOS.getRecords())) {
            return new Page<ProductListVM_V21>().setCurrent(0).setSize(0L).setTotal(0L).setRecords(Collections.emptyList());
        }
        IPage<ProductListVM_V21> productListVMV21IPage = shopeeProductService.getProductListVM_v21IPage(new Page<ShopeeProduct>(boostItemDTOS.getCurrent(), boostItemDTOS.getSize(), boostItemDTOS.getTotal()).setRecords(boostItemDTOS.getRecords()));
        shopeeProductService.settingRequiredInfo(productSearchStrategy, currentLogin, productListVMV21IPage);
        return productListVMV21IPage;
    }

    private IPage<ShopeeProduct> selectPartBoostItemByShopId(Page page, String currentLogin, Integer status, ProductSearchStrategyDTO productSearchStrategyDTO) {
        return repository.selectPartBoostItemByShopId(page, currentLogin, status, productSearchStrategyDTO);
    }

    /**
     * 检查参数并且转换成合适的数据格式
     *
     * @param list
     * @return
     */
    private List<BoostItemDTO> getCheck(List<ShopeeProduct> list, UnaryOperator<List<BoostItemDTO>> unaryOperator) {
        final List<BoostItemDTO> collect = list.stream()
                .filter(Objects::nonNull)
                .filter(shopeeProduct -> Objects.nonNull(shopeeProduct.getShopeeItemId()))
                .map(shopeeProduct -> {
                    return new BoostItemDTO()
                            .setLogin(shopeeProduct.getLoginId())
                            .setPlatformId(shopeeProduct.getPlatformId())
                            .setPlatformNodeId(shopeeProduct.getPlatformNodeId())
                            .setProductId(shopeeProduct.getId())
                            .setShopId(shopeeProduct.getShopId())
                            .setShopeeItemId(shopeeProduct.getShopeeItemId());
                })
                .collect(Collectors.toList());
        final List<BoostItemDTO> apply = unaryOperator.apply(collect);
        return apply;
    }

    private List<ShopeeProduct> getCheckParam(List<Long> deleteId) {
        final String login = SecurityUtils.currentLogin();
        if (isNotSubscribe(login)) {
            return Collections.emptyList();
        }
        List<ShopeeProduct> shopeeProducts = shopeeProductService.findList(deleteId, login, null, null);
        shopeeProducts = shopeeProducts.stream().filter(shopeeProduct ->
                Objects.equals(shopeeProduct.getStatus().code, LocalProductStatus.PUBLISH_SUCCESS.code) ||
                        Objects.equals(shopeeProduct.getStatus().code, LocalProductStatus.PUSH_SUCCESS.code) ||
                        Objects.equals(shopeeProduct.getStatus().code, LocalProductStatus.PUBLIC_SUCCESS.code) ||
                        Objects.equals(shopeeProduct.getStatus().code, LocalProductStatus.PULL_SUCCESS.code)
        ).collect(Collectors.toList());

        return shopeeProducts;
    }


    @Resource
    DariusService dariusService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean isNotSubscribe(String loginId) {
        //0 未订阅或已过期 ，2 未过期
        boolean isNotSubscribe = true;
        try {
            String subscribe = stringRedisTemplate.opsForValue().get(keySubscribeProfix + loginId);
            if (StringUtils.isNotBlank(subscribe)) {
                final int parseInt = Integer.parseInt(subscribe);
                if (parseInt == 2) {
                    isNotSubscribe = false;
                }
                return isNotSubscribe;
            }
            final ResponseEntity<ProductFeatureDTO> sTimingTopOffer = dariusService.findProductOne(loginId, "STimingTopOffer");
            if (!sTimingTopOffer.getBody().getExpired()) {
                isNotSubscribe = false;
            }
        } catch (Throwable e) {
            log.error("loginId:{} error:", loginId, e);
            isNotSubscribe = false;
        } finally {
            String reset = stringRedisTemplate.opsForValue().get(keyResetProfix + loginId);
            // 未订购 或者 已过期
            if (isNotSubscribe) {
                if (StringUtils.isNotBlank(reset) && 0 != Integer.parseInt(reset)) {
                    this.repository.updateByLoginId(loginId, null, BoostItem.Status.INVALID_BOOST.getCode());
                }
                stringRedisTemplate.opsForValue().set(keyResetProfix + loginId, String.valueOf(0));
            } else {
                //reset 0:已置为无效 2:已重置
                if (StringUtils.isNotBlank(reset) && 2 != Integer.parseInt(reset)) {
//                    this.repository.updateByLoginId(loginId, BoostItem.Status.INVALID_BOOST.getCode(), BoostItem.Status.NO_BOOST.getCode());
                }
                stringRedisTemplate.opsForValue().set(keyResetProfix + loginId, String.valueOf(2));
            }
        }
        if (isNotSubscribe) {
            handlerSubscribe(loginId, 0);
        } else {
            handlerSubscribe(loginId, 2);
        }
        return isNotSubscribe;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handlerSubscribe(String loginId, int isSubscribe) {
        stringRedisTemplate.opsForValue().set(keySubscribeProfix + loginId, String.valueOf(isSubscribe));
        stringRedisTemplate.expire(keySubscribeProfix + loginId, 1, TimeUnit.MINUTES);
    }

    /**
     * 虾皮单点铺一次最大多少个
     */
    public static final int BOOST_ITEM_MAX_SIZE = 5;


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int boostItem() {
        try {
            //  获取到 当前要置顶的店铺的数量
            final int distinctLoginCount = repository.getDistinctShopIdCount();
            boolean isRun = true;
            do {
                //  获取到 置顶人员的节点 游标      a 服务从  0 取 5 个     b 服务 从 6开始取5个
                final Optional<Integer> loginPage = boostItemCacheService.run(Optional.of(CacheObejct.builder().size(distinctLoginCount).key("login").lock("login:lock").build()));
                final Integer cursor = loginPage.get();
                log.info("cursor [{}]", cursor);
                //  根据游标 取数据库里面抽取 5 个出来进行处理
                final List<BoostItem> distinctLogin = repository.getDistinctLogin(settingMyPage(cursor, 5));
                distinctLogin.forEach(boostItem -> {
                    //  然后根据每个人 进行处理
                    try {
                        final boolean lock = redisLockHelper.lock("lux:boost:shop:luck:" + boostItem.getShopId(), 1, TimeUnit.MINUTES);
                        if (lock) {
                            // 如果这个key  存在证明 线上还没有 过期
                            final Boolean hasKey = stringRedisTemplate.hasKey(keyTimeProfix + ":" + boostItem.getShopId());
                            if (!hasKey) {
                                if (!isNotSubscribe(boostItem.getLogin())) {
                                    final BoostMQItems boostMQItems = new BoostMQItems().setShopId(boostItem.getShopId()).setLoginId(boostItem.getLogin());
                                    boostItem(boostMQItems, BOOST_ITEM_MAX_SIZE);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        redisLockHelper.unlock("lux:boost:shop:luck::" + boostItem.getShopId());
                    }
                });

                if (cursor == -1) {
                    isRun = false;
                }
            } while (isRun);
            //  重置数据
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
        return -1;
    }

    public void boostItemV2() {
        try {
            //获取所有有效的需要置顶的店铺ID
            List<Long> shopIds = repository.getDistinctShopIds();
            shopIds.forEach(shopId -> {
                try {
                    String login = shopIdLoginMap.getIfPresent(shopId);
                    if (null == login) {
                        ShopeeShopDTO shopeeShopDTO = uaaService.getShopInfo(shopId).getBody();
                        if (null != shopeeShopDTO) {
                            if (null != shopeeShopDTO.getLogin()) {
                                login = shopeeShopDTO.getLogin();
                                shopIdLoginMap.put(shopId, login);
                            } else {
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                    boolean isSubscribe = !isNotSubscribe(login);
                    log.info("login:{} , shopId:{} ,isSubscribe:{}", login, shopId, isSubscribe);
                    if (isSubscribe) {
                        executorService.execute(() -> {
                            final ShopeeResult<GetItemBootsResult> itemsBoost = itemApi.getItemsBoost(GetItemBoostParam.builder().shopId(shopId).build());
                            if (null != itemsBoost.getData() && itemsBoost.isResult()) {
                                List<GetItemBootsResult.Items> items = itemsBoost.getData().getItems();
                                //获得已经置顶的数量
                                List<Long> boostedItemIds = items.stream().map(GetItemBootsResult.Items::getItemId).collect(Collectors.toList());
                                //还可以置顶的数量
                                int availableCount = MAX_BOOST_ITEM_COUNT - boostedItemIds.size();
                                if (!boostedItemIds.isEmpty()) {
                                    //将表里未置顶状态，但是shopee已置顶的更新
                                    repository.updateBoostingStatusForBoostedItem(shopId, boostedItemIds);
                                }
                                //将表里已经处于置顶状态的商品，并且shopee状态未置顶的 状态更新
                                repository.updateBoostStatusForUnBoostItem(shopId, boostedItemIds);

                                if (availableCount > 0) {
                                    //从表里查找出 availableCount 个可以置顶的商品
                                    List<BoostItem> boostItems = repository.findAvailableBoostItem(shopId, availableCount);
                                    if (!boostItems.isEmpty()) {
                                        InsertItemBoostParam insertItemBoostParam = new InsertItemBoostParam();
                                        insertItemBoostParam.setItemId(boostItems.stream().map(BoostItem::getShopeeItemId).collect(Collectors.toList()));
                                        insertItemBoostParam.setShopId(shopId);
                                        ShopeeResult<InsertItemBootsResult> insertItemBootsResult = itemApi.insertItemsBoost(insertItemBoostParam);
                                        if (insertItemBootsResult.isResult()) {
                                            log.info("shopId:{},boost success: {}", shopId, insertItemBootsResult.getData().getBatchResult());
                                            if (!insertItemBootsResult.getData().getBatchResult().getSuccesses().isEmpty()) {
                                                repository.batchStatusByIds(insertItemBootsResult.getData().getBatchResult().getSuccesses(), BoostItem.Status.CUCCENT_BOOST.getCode());
                                            }
                                            if (null != insertItemBootsResult.getData().getBatchResult().getFailures() && !insertItemBootsResult.getData().getBatchResult().getFailures().isEmpty()) {
                                                List<Long> failShouldUpdateInvalid = insertItemBootsResult.getData().getBatchResult().getFailures().parallelStream().filter(item -> !item.getDescription().contains("5 boosted")).map(InsertItemBootsResult.Failures::getId).collect(Collectors.toList());
                                                if (!failShouldUpdateInvalid.isEmpty()) {
                                                    repository.batchStatusByIds(failShouldUpdateInvalid, BoostItem.Status.INVALID_BOOST.getCode());
                                                }
                                            }
                                        } else {
                                            log.error("shopId:{},boost fail:{} ", shopId, insertItemBootsResult);
//                                        repository.batchStatusByIds(boostItems.stream().map(BoostItem::getShopeeItemId).collect(Collectors.toList()),BoostItem.Status.INVALID_BOOST.getCode());
                                        }
                                    }
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("boost item shopId:{} error: ", shopId, e);
                }
            });

        } catch (Exception e) {
            log.error("boost item error: ", e);
        }
    }


    /**
     * 处理每个人的定时置顶
     *
     * @param item 定时置顶对象
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int boostItem(BoostMQItems item, int count) {
        final String login = item.getLoginId();
        final Long shopId = item.getShopId();
        log.info("distinctLogin [{}]", JSONObject.toJSONString(item));
        //  需要置顶数量
        try {
            int boostItemSize = handlerResult(item, count, login, shopId);
            //  去虾皮哪里 获取到 现在的置顶状况
            final ShopeeResult<GetItemBootsResult> itemsBoost = itemApi.getItemsBoost(GetItemBoostParam.builder().shopId(item.getShopId()).build());
            //  解析 返回的值
            final GetItemBootsResult data = JSON.parseObject(itemsBoost.getJson(), GetItemBootsResult.class);
            if (Objects.nonNull(data)) {
                final List<GetItemBootsResult.Items> items = data.getItems();
                if (Objects.nonNull(items) && !items.isEmpty()) {
                    // 已经过期的有多少个
                    boostItemSize = regulateCount(login, items, shopId);
                } else {
                    resetItemCurrentExpiredBoost(login, shopId, boostItem1 ->
                            //  在 当前置顶里面没有找到 证明过时
                            boostItem1.getGmtModified().plus(4, ChronoUnit.HOURS).isBefore(Instant.now()));
                }
            }
            //   数据库里面可以进行置顶的 数量
            final int dataBaseBoostCount = repository.getCountByLogin(login, shopId, BoostItem.Status.NO_BOOST.getCode());
            log.info("distinctLogin [{}] ，boostItemSize [{}]，dataBaseBoostCount [{}]", JSONObject.toJSONString(item), boostItemSize, dataBaseBoostCount);
            // 如果游标不等于   -1  就是 还有可以使用的 值  证明需要置顶
            if (dataBaseBoostCount > 0) {
                if (boostItemSize > 0) {
                    log.info(" product login {} ,shopId {}  置顶数量 {} ", login, shopId, boostItemSize);
                    send(shopId, boostItemSize, login);
                }
            } else {
                //  重置数据
                executor.execute(() -> {
                    resetItmeBoost(item);
                });
            }
        } catch (Exception e) {
            log.error(" product login {} ,shopId {}", login, shopId, e.getMessage(), e);
        }
        return -1;
    }

    /**
     * 用来处理回调函数 的 方法
     *
     * @param item
     * @param count
     * @param login
     * @param shopId
     * @return
     */
    int handlerResult(BoostMQItems item, int count, String login, Long shopId) {
        if (Objects.nonNull(count) && count > 0) {
            // 处理多余的 数据
            final List<MyBoostItem> oldBoostItems = item.getOldBoostItems();
            if (!CollectionUtils.isEmpty(oldBoostItems)) {
                final List<BoostItemDTO> boostItemDTOS = mapper.toDto(mapper.toEntityV2(oldBoostItems));
                boostItemDTOS.forEach(boostItemDTO -> {
                    boostItemDTO.setStatus((byte) BoostItem.Status.NO_BOOST.getCode());
                });
                batchUpdate(boostItemDTOS, boostItemDTOS.size());
            }
            //  处理异常数据
            handlerFailuresResult(item);
            //   假设 时间大于现在的 时间才会去设置一个屏障
            if (Objects.nonNull(item.getDateTime()) && Objects.nonNull(item.getLastTime())) {
                final Duration between = Duration.between(LocalDateTime.now(), LocalDateTime.ofInstant(Instant.ofEpochSecond(item.getDateTime()), ZoneOffset.UTC).plusSeconds(item.getLastTime()));
                if (between.getSeconds() > 60) {
                    //  在基础的时间上面加上 4分钟
                    redisLockHelper.lock(keyTimeProfix + ":" + shopId, between.getSeconds() + 240, TimeUnit.SECONDS);
                }
            }
            // 已经过期的有多少个
            if (!CollectionUtils.isEmpty(item.getCurrnetBoost())) {
                return regulateCountV2(login, item.getCurrnetBoost(), shopId);
            }

            return count;
        } else {
            //  基本不会走这里了。。
            resetItemCurrentExpiredBoost(login, shopId, boostItem1 ->
                    //  在 当前置顶里面没有找到 证明过时
                    boostItem1.getGmtModified().plus(4, ChronoUnit.HOURS).isBefore(Instant.now()));
            return 0;
        }
    }

    private void handlerFailuresResult(BoostMQItems item) {
        doHandlerFailuresResult(item, Objects.nonNull(item.getErrorCode()));
    }

    /**
     * @param item
     * @param isShopeeError 是否是接口方面的错 比如说 店铺未授权等等
     */
    private void doHandlerFailuresResult(BoostMQItems item, boolean isShopeeError) {
        if (!CollectionUtils.isEmpty(item.getFailures())) {
            final List<BoostMQItems.Failures> failures = item.getFailures();
            for (BoostMQItems.Failures failure : failures) {
                final BoostItem boostItem = new BoostItem();
                final int status = isShopeeError || "error_param".equals(failure.getErrorCode()) ? BoostItem.Status.INVALID_BOOST.getCode() : BoostItem.Status.NO_BOOST.getCode();
                boostItem.setStatus((byte) status);
                boostItem.setFeature(JSONObject.toJSONString(new HashMap<String, Object>() {{
                    //  partner and shop has no linked
                    put("error", isShopeeError ? item.getError() : failure.getDescription());
                    //  error_auth
                    put("errorCode", isShopeeError ? item.getErrorCode() : failure.getErrorCode());
                }}));
                repository.update(boostItem, new QueryWrapper<BoostItem>().eq("shopee_item_id", failure.getId()).eq("deleted", 0));
            }
        }
    }

    private void resetItemCurrentExpiredBoost(String login, Long shopId, Predicate<? super BoostItem> predicate) {
        final List<BoostItem> records = repository.selectListByLoginAndStatus(new Page<BoostItem>(0, 10), shopId, login, (byte) BoostItem.Status.CUCCENT_BOOST.getCode());
        final List<Long> collect = records
                .stream()
                .filter(predicate)
                .map(BoostItem::getShopeeItemId)
                .collect(Collectors.toList());
        //    将状态更正为 已置顶一次了
        if (!collect.isEmpty()) {
            repository.batchStatusByIds(collect, BoostItem.Status.SHUTDOWN_BOOST.getCode());
        }
    }


    private void resetItmeBoost(BoostMQItems item) {

        shutdownBoost(item, BoostItem.Status.START_BOOST);
        shutdownBoost(item, BoostItem.Status.SHUTDOWN_BOOST);

    }

    private void shutdownBoost(BoostMQItems item, BoostItem.Status status) {
        Integer countByLogin;
        do {
            final String login = item.getLoginId();
            final Long shopId = item.getShopId();
            countByLogin = repository.getCountByLogin(login, shopId, status.getCode());
            final List<Long> collect = repository.selectListByLoginAndStatus(new Page<BoostItem>(0, 10), shopId, login, (byte) status.getCode())
                    .stream()
                    .map(BoostItem::getShopeeItemId)
                    .collect(Collectors.toList());
            if (!collect.isEmpty()) {
                repository.batchStatusByIds(collect, BoostItem.Status.NO_BOOST.getCode());
            }
        } while (countByLogin >= 1);
    }

    private int regulateCount(String login, List<GetItemBootsResult.Items> items, Long shopId) {
        final List<Long> itemIds = items.stream().map(GetItemBootsResult.Items::getItemId).collect(Collectors.toList());
        //  数据库里面找到
        resetItemCurrentExpiredBoost(login, shopId, boostItem1 ->
                //  在 当前置顶里面没有找到 证明过时
                !itemIds.contains(boostItem1.getShopeeItemId()));

        //    将状态更正为 已置顶一次了
        batchUpdatePlus(items.stream().map(items1 -> new BoostItemDTO().setShopeeItemId(items1.getItemId())).collect(Collectors.toList()), items.size(), boostItemDTO -> {
            final BoostItemDTO boostItemDTO1 = boostItemDTO.setStatus((byte) BoostItem.Status.CUCCENT_BOOST.getCode());
            return new UpdateWrapper<BoostItem>().set("`status`", (byte) BoostItem.Status.CUCCENT_BOOST.getCode()).eq("shopee_item_id", boostItemDTO1.getShopeeItemId());
        });
        return BOOST_ITEM_MAX_SIZE - itemIds.size();
    }


    private int regulateCountV2(String login, List<Long> items, Long shopId) {

        //  处理过时
        //  数据库里面找到
        resetItemCurrentExpiredBoost(login, shopId, boostItem1 ->
                //  在 当前置顶里面没有找到 证明过时
                !items.contains(boostItem1.getShopeeItemId()));

        //  处理正在置顶的
        batchUpdatePlus(items.stream().map(items1 -> new BoostItemDTO().setShopeeItemId(items1)).collect(Collectors.toList()), items.size(), boostItemDTO -> {
            final BoostItemDTO boostItemDTO1 = boostItemDTO.setStatus((byte) BoostItem.Status.CUCCENT_BOOST.getCode());
            return new UpdateWrapper<BoostItem>().set("`status`", (byte) BoostItem.Status.CUCCENT_BOOST.getCode()).eq("shopee_item_id", boostItemDTO1.getShopeeItemId());
        });

        return BOOST_ITEM_MAX_SIZE - items.size();
    }


    /**
     * 重新 找 几个 发到 虾皮上面去
     *
     * @param shopId
     * @param selectCount
     * @param login
     */
    private void send(final Long shopId, int selectCount, String login) {
        if (selectCount < 0) {
            return;
        }
        List<BoostItem> records = checkProduct(shopId, selectCount);
        records.forEach(boostItem -> {
            try {
                boostItem.setStatus((byte) BoostItem.Status.START_BOOST.getCode());
                repository.updateById(boostItem);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
        String key = shopId + ":" + login + ":" + records.stream().map(boostItem -> boostItem.getShopeeItemId().toString()).collect(Collectors.joining(":"));
        final BoostMQItems boostMQItems = new BoostMQItems().setLoginId(login).setShopId(shopId).setBoostItems(mapper.toDtoV2(records));
//        mqProducerService.sendMQ(BaseVariable.Boost.TO_SHOPEE_BOOST,key , boostMQItems);
        mqProducerService.sendMQ(getTagVariable(), key, boostMQItems, msg -> {
            msg.putUserProperties("version", "2");
        });
    }

    /**
     * 检查商品是否还在我们erp 系统里面 存活 并且获取对应数量
     *
     * @return Collections.emptyList()  属于正常结束
     * null        表示 数据库里面没有值
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<BoostItem> doCheckProduct(final Long shopId, int selectCount) {
        List<BoostItem> records = new ArrayList<>(selectCount);
        int index = 0;
        // 从数据库获取到当前数值
        do {
            List<ShopeeProduct> shopeeProducts = Collections.EMPTY_LIST;
            final List<BoostItem> boostItems = repository.selectBoostItemByShopId(settingMyPage(index, CollectionUtils.isEmpty(records) ? selectCount : BOOST_ITEM_MAX_SIZE - records.size()), shopId, BoostItem.Status.NO_BOOST.getCode());
            log.info("准备数据发送mq records [{}] ，selectCount [{}]，countByLogin [{}]", records.isEmpty(), JSONObject.toJSONString(records));
            if (!CollectionUtils.isEmpty(boostItems)) {
                final List<Long> collect = boostItems.stream().map(BoostItem::getProductId).collect(Collectors.toList());
                shopeeProducts = shopeeProductService.findList(collect, null, null, Boolean.TRUE);
            }
            if (!CollectionUtils.isEmpty(shopeeProducts)) {
                final List<Long> collect1 = shopeeProducts.stream().map(ShopeeProduct::getId).collect(Collectors.toList());
                final Map<Boolean, List<BoostItem>> collect2 = boostItems.stream().collect(Collectors.groupingBy(boostItem -> {
                    final boolean contains = collect1.contains(boostItem.getProductId());
                    return contains;
                }));
                final List<BoostItem> falseCollect = collect2.get(Boolean.FALSE);
                if (!CollectionUtils.isEmpty(falseCollect)) {
                    //  因为 boost 不知道   商品是否已经被删除
                    getRepository().deleteBatchIds(falseCollect.stream().map(BoostItem::getId).collect(Collectors.toList()));
                }
                final List<BoostItem> trueCollect = collect2.get(Boolean.TRUE);
                if (!CollectionUtils.isEmpty(trueCollect)) {
                    index = index + trueCollect.size();
                    records.addAll(trueCollect);
                    ;
                }
            } else {
                break;
            }
        } while (Objects.isNull(records) || records.size() < selectCount);
        return records;
    }

    /**
     * 检查商品是否还在我们erp 系统里面 存活 并且获取对应数量
     *
     * @return Collections.emptyList()  属于正常结束
     * null        表示 数据库里面没有值
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<BoostItem> checkProduct(final Long shopId, int selectCount) {
        final long start = System.currentTimeMillis();
        try {
            return doCheckProduct(shopId, selectCount);
        } finally {
            final long end = System.currentTimeMillis();
            log.info("checkProduct用时 :【{}】", (end - start) / 1000);
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MyPage {

        @NotNull
        private int page;
        @NotNull
        private int size;


    }

    /**
     * @param limit
     * @param selectCount
     * @return
     */
    public final MyPage settingMyPage(int limit, int selectCount) {
        if (limit <= -1) {
            limit = 0;
        }
        if (log.isDebugEnabled()) {
            log.debug(" ===================================================================================================== current  {} ,page  {}", limit, selectCount);
        }
        return new MyPage(limit, selectCount);
    }
}
