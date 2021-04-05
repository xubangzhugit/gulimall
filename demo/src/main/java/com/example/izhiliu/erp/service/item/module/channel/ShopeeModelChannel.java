package com.izhiliu.erp.service.item.module.channel;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.lock.annotation.Lock4j;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.core.config.subscribe.Enum.SubLimitProductConstant;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.dingtalk.notice.mobile.PhoneNumberEnumerate;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.common.TaskExecutorUtils;
import com.izhiliu.erp.config.ApplicationProperties;
import com.izhiliu.erp.config.DingDingInfoMessageSendr;
import com.izhiliu.erp.config.aop.subscribe.SubLimitService;
import com.izhiliu.erp.config.mq.vo.ShopeeActionVO;
import com.izhiliu.erp.config.task.AsyncRemoveDeletedProduct;
import com.izhiliu.erp.domain.enums.InternationEnum;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.log.LogConstant;
import com.izhiliu.erp.log.LoggerOp;
import com.izhiliu.erp.service.item.*;
import com.izhiliu.erp.service.item.dto.ItemSyncDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeePullMessageDTO;
import com.izhiliu.erp.service.item.dto.ShopeeSyncDTO;
import com.izhiliu.erp.service.item.impl.ShopeeBasicDataServiceImpl;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import com.izhiliu.erp.service.module.metadata.basic.ModelChannel;
import com.izhiliu.erp.service.mq.producer.MQProducerService;
import com.izhiliu.erp.util.RedissonDistributedLocker;
import com.izhiliu.erp.web.rest.errors.DataNotFoundException;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.param.GetItemListParam;
import com.izhiliu.open.shopee.open.sdk.api.item.result.GetItemListResult;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import com.izhiliu.open.shopee.open.sdk.entity.logistics.LogisticsResult;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.izhiliu.erp.service.item.impl.ShopeeProductServiceImpl.SYNC_PRODUCT_TO_LOCAL;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * describe: 负责 ERP 与 Shopee 平台模型互转
 * <p>
 *
 * @author cheng
 * @date 2019/1/16 14:58
 */
@Component
public class ShopeeModelChannel implements ModelChannel {

    private static final Logger log = LoggerFactory.getLogger(ShopeeModelChannel.class);

    /**
     * 单次获取个数
     */
    private static final int SIZE = 50;

    private static final String INVALID_CATEGORY_ID = "invalid category id";

    @Resource
    private ApplicationProperties properties;

    @Resource
    private RedissonDistributedLocker locker;

    @Resource
    private RedisLockHelper redisLockHelper;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private MQProducerService mqProducerService;

    @Resource
    private PlatformNodeService platformNodeService;

    @Resource
    private ShopeeProductService shopeeProductService;

    @Resource
    ShopeeBasicDataServiceImpl shopeeBasicDataService;

    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;

    @Resource
    private ShopeeSkuAttributeService shopeeSkuAttributeService;

    @Resource
    private ShopeeProductAttributeValueService shopeeProductAttributeValueService;

    @Resource
    private ShopeeCategoryService shopeeCategoryService;

    @Resource
    HandleProductExceptionInfo handleProductExceptionInfo;

    @Resource
    DingDingInfoMessageSendr textMessageSender;

    @Resource
    private UaaService uaaService;

    @Resource
    private ItemApi itemApi;

    @Resource
    private MessageSource messageSource;

    @Resource
    private TaskExecutorUtils taskExecutorUtils;



    private Executor executor;

    public ShopeeModelChannel() {
        executor = Executors.newFixedThreadPool(20);
    }

    @Override
    public boolean push(Long productId, Long shopId, String loginId) {
        try {
            final ShopeeProductDTO product = getProduct(productId);
            if (product.getShopeeItemId() == null) {
                log.warn("[此产品尚未发布, 无法更新] - 产品编号:{}", productId);
                return false;
            }
            /*
             * 正在操作
             */
            if (checkStatus(product)) {
                return false;
            }
            /*
             * 提前校验 确保提交到MQ的任务绝大部分是可正常执行的
             */
            shopeeProductService.checkParam(product);
            shopeeProductAttributeValueService.checkRequired(productId, product.getShopeeCategoryId(), product.getPlatformNodeId());
            shopeeProductService.checkNoInitTierVariation(product);

            final ShopeeActionVO action = ShopeeActionVO.builder()
                    .action(ShopeeActionVO.Action.PUSH.getCode())
                    .shopId(shopId)
                    .productId(productId)
                    .loginId(loginId)
                    .build();
            mqProducerService.sendMQ("SHOPEE_ACTION_PUSH", shopId + ":" + productId, action);
        } catch (Exception e) {
            log.error("[更新商品失败] {}", e);
            fail(productId, handleProductExceptionInfo.doMessage(e.getMessage()), LocalProductStatus.PUSH_FAILURE);
        }
        return true;
    }

    @Override
    public boolean pullTask(Long itemId, Long shopId, String loginId, boolean batch,String taskId,Integer shopeeUpdateTime) {
        try {
            /*
             * 重复同步
             */
            final Optional<ShopeeProductDTO> productExist = shopeeProductService.selectByItemId(itemId);
            if (productExist.isPresent()) {
                final ShopeeProductDTO shopeeProductDTO = productExist.get();
                if (shopeeProductDTO.getStatus().equals(LocalProductStatus.IN_PULL) && shopeeProductDTO.getGmtModified().plus(3, ChronoUnit.MINUTES).isAfter(Instant.now())) {
                    log.info("[不能短时间内重复同一个商品] : {}", shopeeProductDTO);
                    // shopeeOperate(shopId, 1, 3, itemId, "不能短时间内重复同一个商品");
                    return false;
                }
            }

            final ShopeeActionVO action = ShopeeActionVO.builder()
                    .action(ShopeeActionVO.Action.PULL.getCode())
                    .shopId(shopId)
                    .itemId(itemId)
                    .loginId(loginId)
                    .key(taskId)
                    .shopeeUpdateTime(shopeeUpdateTime)
                    .build();

            mqProducerService.sendMQ("SHOPEE_ACTION_PULL", shopId + ":" + itemId, action);

        } catch (Exception e) {
            log.error("[同步商品失败] shopId:{} itemId:{}, loginId:{}, exception:{}", shopId, itemId, loginId, e);
            //shopeeOperate(shopId, 1, 3, itemId, "同步商品未知异常");
            fail0(itemId, shopId, e.getMessage());
        } finally {
            if (batch && properties.getShopee().getMqPull()) {
                redisLockHelper.minusCounter(SYNC_PRODUCT_TO_LOCAL + shopId);
            }
        }
        return true;
    }

    public boolean pull(Long itemId, Long shopId, String loginId, boolean batch, String key) {

        try {
            /*
             * 重复同步
             */
            final Optional<ShopeeProductDTO> productExist = shopeeProductService.selectByItemId(itemId);
            if (productExist.isPresent()) {
                final ShopeeProductDTO shopeeProductDTO = productExist.get();
                if (shopeeProductDTO.getStatus().equals(LocalProductStatus.IN_PULL) && shopeeProductDTO.getGmtModified().plus(3, ChronoUnit.MINUTES).isAfter(Instant.now())) {
                    log.info("[不能短时间内重复同一个商品] : {}", shopeeProductDTO);
                    shopeeOperate(shopId, 1, 3, itemId, messageSource.getMessage(InternationEnum.REPETITION_SYNC_COMMODITY.getCode(), null, LocaleContextHolder.getLocale()), key, loginId);
                    return false;
                }
            }
            final ShopeeActionVO action = ShopeeActionVO.builder()
                    .action(ShopeeActionVO.Action.PULL.getCode())
                    .shopId(shopId)
                    .itemId(itemId)
                    .loginId(loginId)
                    .key(key)
                    .build();

            mqProducerService.sendMQ("SHOPEE_ACTION_PULL", shopId + ":" + itemId + key, action);
        } catch (Exception e) {
            log.error("[同步商品失败] shopId:{} itemId:{}, loginId:{}, exception:{}", shopId, itemId, loginId, e);
            shopeeOperate(shopId, 1, 3, itemId, messageSource.getMessage(InternationEnum.SYNC_EXCEPTION.getCode(), null, LocaleContextHolder.getLocale()), key, loginId);
            fail0(itemId, shopId, e.getMessage());
        } finally {
            if (batch && properties.getShopee().getMqPull()) {
                redisLockHelper.minusCounter(SYNC_PRODUCT_TO_LOCAL + shopId);
            }
        }
        return true;
    }

    @Resource
    SubLimitService subLimitService;

    @Override
    public boolean publish(Long productId, Long shopId, String currentLogin) {
        return handleChannel(doPublish(productId, shopId, currentLogin), currentLogin, SubLimitProductConstant.RELEASE_OFFER_COUNT);
    }

    /**
     * 新增支持的方法  在 调方法的时候才进行扣除
     *
     * @param productId
     * @param shopId
     * @param currentLogin
     * @return
     */
    @Override
    public boolean publishSupport(Long productId, Long shopId, String currentLogin) {
        subLimitService.handleLimitSupport(currentLogin, SubLimitProductConstant.RELEASE_OFFER_COUNT, String.valueOf(productId));
        return handleChannel(doPublish(productId, shopId, currentLogin), currentLogin, SubLimitProductConstant.RELEASE_OFFER_COUNT);
    }

    private boolean handleChannel(boolean result, String currentLogin, String key) {
        if (!result) {
            subLimitService.doAfterThrowing(currentLogin, key);
        }
        return result;
    }

    public boolean doPublish(Long productId, Long shopId, String currentLogin) {
        final LoggerOp loggerOp = getLoggerOpObejct();
        loggerOp.setLoginId(currentLogin);
        loggerOp.setMessage("[开始发布] - 产品编号:{}");
        log.info(loggerOp.toString(), productId);
        try {
            final ShopeeProductDTO product = getProduct(productId);

            if (product.getShopeeItemId() != null) {
                loggerOp.setMessage("[此产品已发布, 无法再次发布] - 产品编号:{}").error();
                log.warn(loggerOp.toString(), productId);
                return false;
            }
            if (checkStatus(product)) {
                loggerOp.setMessage("[发布] - 商品正在执行操作, 本次任务拒绝执行, productId: {}, status: {}").error();
                log.warn(loggerOp.toString(), product.getId(), product.getStatus().status);
                return false;
            }

            validate(productId, product);
            shopeeProductSkuService.checkPublishParam(productId);

            final List<Integer> collect = shopeeBasicDataService.getLogisticsInfoByShopId(product.getShopId()).stream().filter(LogisticsResult.LogisticsBean::getEnabled).map(LogisticsResult.LogisticsBean::getLogisticId).collect(toList());
            if (!CollectionUtils.isEmpty(collect)) {
                final List<ShopeeProductDTO.Logistic> logistics = product.getLogistics().stream().filter(logistic -> collect.contains(logistic.getLogisticId().intValue())).collect(toList());
                if (CollectionUtils.isEmpty(logistics)) {
                    throw new IllegalOperationException("shopee.logistics.not.found");
                }
                shopeeProductService.superUpdate(new ShopeeProductDTO().setId(productId).setLogistics(logistics));
            }
            shopeeProductAttributeValueService.checkRequired(productId, product.getShopeeCategoryId(), product.getPlatformNodeId());

            final ShopeeActionVO action = ShopeeActionVO.builder()
                    .action(ShopeeActionVO.Action.PUBLISH.getCode())
                    .shopId(shopId)
                    .productId(productId)
                    .loginId(currentLogin)
                    .build();
            mqProducerService.sendMQ("SHOPEE_ACTION_PUBLISH", shopId + ":" + productId, action);
        } catch (Exception e) {
            loggerOp.setMessage("[发布商品异常]").error();
            log.error(loggerOp.toString(), e);
            fail(productId, handleProductExceptionInfo.doMessage(e.getMessage()), LocalProductStatus.PUBLISH_FAILURE);
            return false;
        }
        return true;
    }


    LoggerOp getLoggerOpObejct() {
        return new LoggerOp().start().setKind(LogConstant.SHOPEE_PRODUCT).setType("create").setCode(LogConstant.PUBLISH);
    }

    @Override
    @Deprecated
    public boolean pull(Long shopId, String loginId) {
        final String lockKey = getShopSyncLockKey(shopId);

        try {
            List<GetItemListResult.ItemsBean> itemsBeans = new ArrayList<>();

            int page = 1;
            while (true) {
                try {
                    final ShopeeResult<GetItemListResult> items = itemApi.getItemList(GetItemListParam.builder().shopId(shopId).page(page++).size(SIZE).build());
                    if (!items.isResult()) {
                        break;
                    }
                    itemsBeans.addAll(items.getData().getItems());
                    //如果没有后续的数据直接跳出
                    if (!items.getData().isMore()) {
                        break;
                    }


                } catch (Exception e) {
                    log.error(" shopId:{" + shopId + "}, loginId:{" + loginId + "}, page:{" + page + "} exception {}", e);
                    final StringBuilder append = new StringBuilder()
                            .append("  shopId:【").append(shopId)
                            .append("】loginId:【").append(loginId)
                            .append("】page:【").append(page);
                    textMessageSender.send(append, new String[]{PhoneNumberEnumerate.Levi.getMobile()}, true, e);
                }
            }
            log.info(" 同步店铺商品 shopId:{}, loginId:{} ,item size:{} ", shopId, loginId, itemsBeans.size());
            redisLockHelper.counter(lockKey, itemsBeans.size());
            itemsBeans.forEach(itemsBean ->
            {
                final String status = itemsBean.getStatus();
                if (Objects.equals("DELETED", status)) {
                    executor.execute(() -> deleteLocalByShopeeItem(itemsBean.getItemId(), itemsBean.getShopid()));
                } else {
                    executor.execute(() -> pull(itemsBean.getItemId(), shopId, loginId, true,""));
                }
            });
        } catch (Exception e) {
            redisLockHelper.unlock(lockKey);
        }
        return true;
    }

    /**
     * 同步店铺商品
     * @param shopId
     * @param loginId
     * @param taskId
     * @return
     */
    public boolean pull(Long shopId, String loginId, String taskId) {

        final String lockKey = getShopSyncLockKey(shopId);

        try {
            List<GetItemListResult.ItemsBean> itemsBeans = new ArrayList<>();
            int page = 1;
            while (true) {
                try {
                    final ShopeeResult<GetItemListResult> items = itemApi.getItemList(GetItemListParam.builder().shopId(shopId).page(page).size(SIZE).build());
                    page++;
                    if (!items.isResult()) {
                        break;
                    }
                    itemsBeans.addAll(items.getData().getItems());
                    //如果没有后续的数据直接跳出
                    if (!items.getData().isMore()) {
                        break;
                    }
                } catch (Exception e) {
                    log.error(" shopId:{}, loginId:{}, page:{} exception {}",shopId,loginId,page, e);
                }
            }
            log.info(" 同步店铺商品 shopId:{}, loginId:{} ,item size:{} ", shopId, loginId, itemsBeans.size());
            int sum = Long.valueOf(itemsBeans.parallelStream().filter(itemsBean -> !itemsBean.getStatus().equals(ShopeeItemStatus.DELETED.status)).count()).intValue();
            redisLockHelper.counter(lockKey, sum);

            //区分删除和未删除商品，保存未删除状态商品总数到mongodb
            saveShopSumToMongo(shopId,sum,taskId,loginId);

            itemsBeans.forEach(itemsBean ->
            {
                final String status = itemsBean.getStatus();
                if (Objects.equals("DELETED", status)) {
                    executor.execute(() -> deleteLocalByShopeeItem(itemsBean.getItemId(), itemsBean.getShopid()));
                } else {
                    executor.execute(() -> pullTask(itemsBean.getItemId(), shopId, loginId, true,taskId,itemsBean.getUpdateTime()));
                }
            });
        } catch (Exception e) {
            redisLockHelper.unlock(lockKey);
        }
        return true;
    }

    public String getShopSyncLockKey(Long shopId) {
        return SYNC_PRODUCT_TO_LOCAL + shopId;
    }

    /**
     * 同步店铺商品
     * @param shopIds
     * @param loginId
     * @param key
     * @return
     */
    @Deprecated
    public boolean pull(List<Long> shopIds, String loginId, String key) {

        List<List<GetItemListResult.ItemsBean>> lists = new ArrayList<>();
        for (Long shopId : shopIds) {
            try {
                List<GetItemListResult.ItemsBean> itemsBeans = new ArrayList<>();
                int page = 1;
                while (true) {
                    try {
                        final ShopeeResult<GetItemListResult> items = itemApi.getItemList(GetItemListParam.builder().shopId(shopId).page(page++).size(SIZE).build());
                        if (!items.isResult()) {
                            break;
                        }
                        itemsBeans.addAll(items.getData().getItems());
                        //如果没有后续的数据直接跳出
                        if (!items.getData().isMore()) {
                            break;
                        }
                    } catch (Exception e) {
                        log.error(" shopId:{" + shopId + "}, loginId:{" + loginId + "}, page:{" + page + "} exception {}", e);
//                            shopeeOperate(shopId, 1, 4, null, null, key, loginId);
                    }
                }
//                    shopeeOperate(shopId, itemsBeans.size(), 1, null, null, key, loginId);
                log.info(" 同步店铺商品 shopId:{}, loginId:{} ,item size:{} ", shopId, loginId, itemsBeans.size());
                lists.add(itemsBeans);

                //区分删除和未删除商品，保存总数到mongodb


            } catch (Exception e) {
//                    shopeeOperate(shopId, 1, 4, null, null, key, loginId);
            }

        }

        for (List<GetItemListResult.ItemsBean> itemsBeans : lists) {
            itemsBeans.forEach(itemsBean -> //消息消费有可能有冲突
            {
                final String status = itemsBean.getStatus();
                if (Objects.equals("DELETED", status)) {
                    deleteByShopeeItem(itemsBean.getItemId(), itemsBean.getShopid(), key, loginId);
                }
            });
        }

        for (List<GetItemListResult.ItemsBean> itemsBeans : lists) {
            itemsBeans.forEach(itemsBean ->
            {
                final String status = itemsBean.getStatus();
                if (!Objects.equals("DELETED", status)) {
                    executor.execute(() -> pull(itemsBean.getItemId(), itemsBean.getShopid(), loginId, true, key));
                }
            });
        }
        return true;
    }

    public boolean pullByItemIds(List<Long> shopIds, String loginId, List<Long> itmeIds, String taskId) {
        // 单个商品同步，批量商品同步，店铺商品同步，重试同步
        if (!CollectionUtils.isEmpty(itmeIds)) {
            //单个商品同步，批量商品同步
            if (CollectionUtils.isEmpty(shopIds)) {
                List<ShopeeProduct> list = shopeeProductService.findShopeeProductList(itmeIds.stream().filter(Objects::nonNull).collect(toList()));

                long shopCount = list.parallelStream().map(ShopeeProduct::getShopId).distinct().count();
                ShopeeSyncDTO shopeeSyncDTO = new ShopeeSyncDTO(taskId);
                shopeeSyncDTO.setShopCount(Long.valueOf(shopCount).intValue());
                mongoTemplate.save(shopeeSyncDTO);

                list.parallelStream().collect(groupingBy(ShopeeProduct::getShopId)).forEach((k,v)->{
                    saveShopSumToMongo(k,v.size(),taskId,loginId);
                });

                for (ShopeeProduct shopeeProduct : list) {
                    executor.execute(() -> pullTask(shopeeProduct.getShopeeItemId(), shopeeProduct.getShopId(), loginId, false, taskId,null));
                }
            } else {
                saveShopSumToMongo(shopIds.get(0),Long.valueOf(itmeIds.stream().filter(Objects::nonNull).count()).intValue(),taskId,loginId);
                // 重试店铺商品
                for (Long itmeId : itmeIds) {
                    if(null != itmeId){
                        executor.execute(() -> pullTask(itmeId, shopIds.get(0), loginId, false, taskId,null));
                    }
                }
            }
        }
        return true;
    }

    public static final String SYNC_PRODUCT_TO_PULL_LOCAL = "sync-product-to-pull-local:";

    //添加店铺商品总数
    @Deprecated
    public void shopeeOperate(Long shopId, int number, int type, Long itmeId, String cause, String key, String parentLoginId) {
        //加锁
        String l = SYNC_PRODUCT_TO_PULL_LOCAL + parentLoginId + key;
        RLock lock = locker.lock(l);
        try {
            if (StringUtils.isEmpty(key)) {
                return;
            }
            //添加同步数据
            Query query = new Query(Criteria.where("id").is(SYNC_PRODUCT_TO_PULL_LOCAL + parentLoginId + key)); //查询是否存在
            ShopeeSyncDTO shopeeSyncDTO = mongoTemplate.findOne(query, ShopeeSyncDTO.class);
            if (shopeeSyncDTO == null) {
                shopeeSyncDTO = new ShopeeSyncDTO(SYNC_PRODUCT_TO_PULL_LOCAL + parentLoginId + key);
                mongoTemplate.save(shopeeSyncDTO);
            } else {
                Update update = new Update();
                if (shopeeSyncDTO.getIfSync() == 0) update.set("ifSync", 1);
                //修改操作
                Map<Long, ShopeeSyncDTO> map = shopeeSyncDTO.getMap();
                ShopeeSyncDTO shopeeSyncDTO1 = map.get(shopId);
                if (shopeeSyncDTO1 == null) shopeeSyncDTO1 = new ShopeeSyncDTO();

                if (StringUtils.isEmpty(shopeeSyncDTO1.getShopName())) { //查询商品名称
                    ResponseEntity<ShopeeShopDTO> shopInfo = uaaService.getShopInfo(shopId);
                    shopeeSyncDTO1.setShopName(shopInfo.getBody().getShopName());
                }
                if (type == 1) {     //总数添加操作
                    shopeeSyncDTO1.setCount(shopeeSyncDTO1.getCount() + number);
                } else if (type == 2) {   //成功数量操作
                    String status = shopId.toString() + itmeId.toString();
                    shopeeSyncDTO1.setSucceedCount(shopeeSyncDTO1.getSucceedCount() + number);
                    shopeeSyncDTO1.getRepetition().put(status, 1);
                } else if (type == 3) {   //失败数量操作
                    String status = shopId.toString() + itmeId.toString();
                    shopeeSyncDTO1.getRepetition().put(status, 2);
                    shopeeSyncDTO1.setLoserCount(shopeeSyncDTO1.getLoserCount() + number);
                    //失败原因
                    shopeeSyncDTO1.getLoserDetails().add(new ShopeeSyncDTO.LoserDetails(itmeId, cause));
                } else if (type == 4) {
                    shopeeSyncDTO1.setStatus(1);
                    shopeeSyncDTO1.setMsg(messageSource.getMessage(InternationEnum.SHOPEE_REQUEST_ERROR.getCode(), null, LocaleContextHolder.getLocale()));
                }
                int i = shopeeSyncDTO1.getSucceedCount() + shopeeSyncDTO1.getLoserCount();
                if (i > 0) {
                    Double succeedSucceedRatio = (double) i / shopeeSyncDTO1.getCount() * 100;
                    shopeeSyncDTO1.setSucceedRatio((int) Math.floor(succeedSucceedRatio));
                }
                map.put(shopId, shopeeSyncDTO1);
                update.set("map", map);
                update.set("dateTime", DateUtil.now());
                //更新查询返回结果集的第一条
                mongoTemplate.updateFirst(query, update, ShopeeSyncDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            locker.unlock(lock);
        }


    }

    public synchronized void saveShopSumToMongo(Long shopId, int sum, String taskId, String login) {
        Query query = new Query(Criteria.where("id").is(taskId)); //查询是否存在
        ShopeeSyncDTO shopeeSyncDTO = mongoTemplate.findOne(query, ShopeeSyncDTO.class);
        if (null != shopeeSyncDTO) {
            ShopeeSyncDTO shopSyncDTOChild = new ShopeeSyncDTO();
            shopSyncDTOChild.setCount(sum);
            ResponseEntity<ShopeeShopDTO> shopInfo = uaaService.getShopInfo(shopId);
            shopSyncDTOChild.setShopName(shopInfo.getBody().getShopName());
            if (null == shopeeSyncDTO.getMap()) {
                Map<Long, ShopeeSyncDTO> shopeeSyncDTOMap = new HashMap<>();
                shopeeSyncDTOMap.put(shopId, shopSyncDTOChild);
                shopeeSyncDTO.setMap(shopeeSyncDTOMap);
            } else {
                shopeeSyncDTO.getMap().put(shopId, shopSyncDTOChild);
            }
            mongoTemplate.save(shopeeSyncDTO);
        }
    }

    @Resource
    AsyncRemoveDeletedProduct asyncRemoveDeletedProduct;


    /**
     * 删除 以删除 的 包含远程平台商品
     *
     * @param shopeeItemId
     * @param shopId
     */
    public void deleteByShopeeItem(Long shopeeItemId, long shopId) {
        try {
            final Long productId = shopeeProductService.getProductIdByItemId(shopeeItemId);
            if (Objects.nonNull(productId)) {
                asyncRemoveDeletedProduct.removeDeletedShopeeItemData(productId);
            }
        } finally {
            redisLockHelper.minusCounter(SYNC_PRODUCT_TO_LOCAL + shopId);
        }
    }

    /**
     * 只删除本地表的商品
     *
     * @param shopeeItemId
     * @param shopId
     */
    public void deleteLocalByShopeeItem(Long shopeeItemId, long shopId) {
        try {
            final Long productId = shopeeProductService.getProductIdByItemId(shopeeItemId);
            if (Objects.nonNull(productId)) {
                shopeeProductService.deleteLocal(productId);
            }
        } finally {
            redisLockHelper.minusCounter(SYNC_PRODUCT_TO_LOCAL + shopId);
        }
    }

    @Deprecated
    public void deleteByShopeeItem(Long shopeeItemId, long shopId, String key, String loginId) {

        try {
            final Long productId = shopeeProductService.getProductIdByItemId(shopeeItemId);
            if (Objects.nonNull(productId)) {
                asyncRemoveDeletedProduct.removeDeletedShopeeItemData(productId);
                //成功数量+1
            }
            shopeeOperate(shopId, 1, 2, shopeeItemId, null, key, loginId);
        } finally {
            redisLockHelper.minusCounter(SYNC_PRODUCT_TO_LOCAL + shopId);
        }
    }


    private ShopeeProductDTO getProduct(Long id) {
        final Optional<ShopeeProductDTO> productExist = shopeeProductService.find(id);
        if (!productExist.isPresent()) {
            throw new DataNotFoundException("data.not.found.exception.product.not.found", new String[]{"product id : " + id});
        }

        return productExist.get();
    }

    private void fail(long productId, String msg, LocalProductStatus status) {
        final ShopeeProductDTO product = new ShopeeProductDTO();
        product.setId(productId);
        product.setFeature(JSON.toJSONString(new HashMap<String, Object>() {{
            put("error", msg);
        }}));
        refreshStatus(product, status);
    }

    private void fail0(long itemId, long shopId, String msg) {
        try {
            final ShopeeProductDTO product = new ShopeeProductDTO();
            product.setShopeeItemId(itemId);
            product.setShopId(shopId);
            product.setFeature(msg);
            refreshStatus(product, LocalProductStatus.PULL_FAILURE);
        } catch (Exception e) {
            log.error("回写商品失败信息异常", e);
        }
    }

    private void refreshStatus(ShopeeProductDTO product, LocalProductStatus inPush) {
        product.setStatus(inPush);
        shopeeProductService.update(product);
    }

    public boolean checkStatus(ShopeeProductDTO product) {
        final boolean result =
                (product.getStatus().equals(LocalProductStatus.IN_PUBLISH) ||
                        product.getStatus().equals(LocalProductStatus.IN_PULL) ||
                        product.getStatus().equals(LocalProductStatus.IN_PUSH)) &&
                        product.getGmtModified().plusSeconds(30).isAfter(Instant.now());

        if (result) {
            log.warn("[发布-更新-同步] - 商品正在执行操作, 本次任务拒绝执行, productId: {}, status: {}", product.getId(), product.getStatus().status);
        }
        return result;
    }

    private void validate(long productId, ShopeeProductDTO product) {
        /*
         * 提前校验 确保提交到MQ的任务绝大部分是可正常执行的
         */
        shopeeProductService.checkParam(product);
        if (CommonUtils.isBlank(product.getShopeeCategoryId())) {
            throw new IllegalOperationException("shopee.must.attribute");
        }
        shopeeProductAttributeValueService.checkRequired(productId, product.getShopeeCategoryId(), product.getPlatformNodeId());
    }


    @Lock4j(keys = {"#dto.shopId"}, expire = 60000, tryTimeout = 100)
    public boolean pull(ItemSyncDTO dto) {
        final String taskId = dto.getTaskId();
        final Long shopId = dto.getShopId();
        final String login = dto.getLogin();
        int page = 1;
        while (true) {
            try {
                final ShopeeResult<GetItemListResult> items = itemApi.getItemList(GetItemListParam.builder().needDeletedItem(dto.isNeedDeletedItem()).shopId(shopId).page(page).size(SIZE).build());
                page++;
                if (CommonUtils.isNotBlank(items.getError())) {
                    break;
                }
                List<GetItemListResult.ItemsBean> itemsBeanList = items.getData().getItems();
                //发送消息去同步商品
                if (CommonUtils.isNotBlank(itemsBeanList)) {
                    ShopeePullMessageDTO messageDTO = ShopeePullMessageDTO.builder()
                            .login(login)
                            .taskId(taskId)
                            .skipRemove(true)
                            .items(items.getData().getItems())
                            .shopId(shopId)
                            .build();
                    mqProducerService.sendMQ(ShopeePullMessageDTO.TAG, shopId.toString(), messageDTO);
                    //回写同步数量
                    long count = items.getData().getItems().stream().filter(e -> !Objects.equals(ShopeeItemStatus.DELETED.status, e.getStatus())).count();
                    taskExecutorUtils.incrementSyncHash(taskId, "total", (int) count);
                    taskExecutorUtils.incrementSyncShopHash(taskId, "total", shopId, (int) count);
                }
                //如果没有后续的数据直接跳出
                if (!items.getData().isMore()) {
                    break;
                }
            } catch (Exception e) {
                log.error(" shopId:{}, loginId:{}, page:{} exception {}", shopId, login, page, e);
            }
        }
        taskExecutorUtils.incrementSyncHash(taskId, "syncShop", 1);
        return true;
    }
}
