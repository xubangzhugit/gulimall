package com.izhiliu.erp.service.discount.impl;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.core.domain.common.BaseServiceNoLogicImpl;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.common.ShopInfoRedisUtils;
import com.izhiliu.erp.common.TaskExecutorUtils;
import com.izhiliu.erp.config.internation.HandleMessageSource;
import com.izhiliu.erp.domain.discount.ShopeeDiscountDetail;
import com.izhiliu.erp.domain.enums.DiscountStatus;
import com.izhiliu.erp.domain.enums.InternationEnum;
import com.izhiliu.erp.repository.discount.DiscountDetailRepository;
import com.izhiliu.erp.service.discount.DiscountDetailService;
import com.izhiliu.erp.service.discount.DiscountItemService;
import com.izhiliu.erp.service.discount.DiscountItemVariationService;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountDetailDTO;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemDTO;
import com.izhiliu.erp.service.discount.mapper.DiscountDetailMapper;
import com.izhiliu.erp.service.discount.mq.DiscountDetailConsumer;
import com.izhiliu.erp.service.discount.mq.DiscountDetailMessageDTO;
import com.izhiliu.erp.service.mq.producer.MQProducerService;
import com.izhiliu.erp.web.rest.discount.qo.DiscountItemCount;
import com.izhiliu.erp.web.rest.discount.qo.DiscountItemQO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountPageQO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountQO;
import com.izhiliu.erp.web.rest.discount.vo.DiscountDeatilVO;
import com.izhiliu.erp.web.rest.discount.vo.SyncResultVO;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.open.shopee.open.sdk.api.discount.ShopeeDiscountApi;
import com.izhiliu.open.shopee.open.sdk.api.discount.param.*;
import com.izhiliu.open.shopee.open.sdk.api.discount.result.*;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:17
 */
@Service
@Slf4j
public class DiscountDetailServiceImpl extends BaseServiceNoLogicImpl<ShopeeDiscountDetail, ShopeeDiscountDetailDTO,
        DiscountDetailRepository, DiscountDetailMapper> implements DiscountDetailService {

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 100;
    private static final Long KEEP_ALIVE_TIME = 1L;

    ThreadPoolExecutor executor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactoryBuilder()
                    .setNameFormat("discount-%d").build());

    ForkJoinPool forkJoinPool = new ForkJoinPool(3);
    ForkJoinPool forkJoinPool1 = new ForkJoinPool(3);

    @Resource
    private ShopeeDiscountApi shopeeDiscountApi;
    @Resource
    private ShopInfoRedisUtils shopInfoRedisUtils;
    @Resource
    private MQProducerService mqProducerService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private TaskExecutorUtils taskExecutorUtils;
    @Resource
    private DiscountItemService discountItemService;
    @Resource
    private DiscountItemVariationService discountItemVariationService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private EnvironmentHelper environmentHelper;
    @Resource
    private HandleMessageSource handleMessageSource;
    @Resource
    private UaaService uaaService;


    @Override
    @Lock4j(keys = {"#qo.login"}, expire = 60000, tryTimeout = 100)
    public DiscountDeatilVO createDiscountDetail(DiscountQO qo) {
        final String login = qo.getLogin();
        final Long shopId = qo.getShopId();
        final String discountName = qo.getDiscountName();
        final LocalDateTime startTime = qo.getStartTime();
        final LocalDateTime endTime = qo.getEndTime();
        final List<DiscountItemQO.Item> items = qo.getItems();
        if (CommonUtils.isBlank(shopId) || CommonUtils.isBlank(discountName) || CommonUtils.isBlank(startTime)
                || CommonUtils.isBlank(endTime)) {
            return new DiscountDeatilVO();
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new LuxServerErrorException(handleMessageSource.getMessage(InternationEnum.ITEM_DISCOUNT_START_TIME_ERROR.getCode()));
        }
        AddDiscountParam param = AddDiscountParam.builder()
                .discountName(discountName)
                .shopId(shopId)
                .startTime(startTime.toEpochSecond(ZoneOffset.of("+8")))
                .endTime(endTime.toEpochSecond(ZoneOffset.of("+8")))
                .build();
        ShopeeResult<AddDiscountResult> addDiscountResultShopeeResult = shopeeDiscountApi.addDiscount(param);

        if (CommonUtils.isNotBlank(addDiscountResultShopeeResult.getError())) {
            throw new LuxServerErrorException(addDiscountResultShopeeResult.getError().getMsg());
        }
        final String discountId = String.valueOf(addDiscountResultShopeeResult.getData().getDiscountId());
        DiscountDeatilVO vo = DiscountDeatilVO.builder()
                .discountId(discountId)
                .build();

        if (CommonUtils.isNotBlank(items)) {
            DiscountItemQO itemQO = DiscountItemQO.builder()
                    .login(login)
                    .shopId(shopId)
                    .discountId(discountId)
                    .items(qo.getItems())
                    .build();
            discountItemService.callAddDiscountItemApi(itemQO);
        }
        if (CommonUtils.isNotBlank(discountId)) {
            qo.setDiscountId(discountId);
            executor.execute(() -> this.syncDiscountDetail(qo));
        }
        return vo;
    }

    @Override
    @Lock4j(keys = {"#qo.login"}, expire = 60000, tryTimeout = 100)
    public SyncResultVO syncDiscountBatch(DiscountQO qo) {
        final String login = qo.getLogin();
        final String taskId = CommonUtils.getTaskId(login);
        final List<DiscountQO.SyncParam> syncParamList = qo.getSyncParamList();
        SyncResultVO vo = SyncResultVO.builder()
                .taskId(taskId)
                .count(syncParamList.size())
                .build();
        boolean b = syncParamList.stream().allMatch(e -> CommonUtils.isBlank(e.getShopId()) || CommonUtils.isNotBlank(e.getDiscountId()));
        if (!b) {
            throw new LuxServerErrorException("error params");
        }
        //发送同步消息
        if (CommonUtils.isNotBlank(syncParamList)) {
            DiscountDetailMessageDTO messageDTO = DiscountDetailMessageDTO.builder()
                    .login(login)
                    .taskId(taskId)
                    .syncParam(syncParamList)
                    .build();
            log.info("同步折扣信息,login={},taskId={}", login, taskId);
            mqProducerService.sendMQ(DiscountDetailConsumer.TAG, taskId, messageDTO);
        }

        taskExecutorUtils.syncStart(taskId);
        return vo;
    }


    @Override
    @Lock4j(keys = {"#qo.login"}, expire = 60000, tryTimeout = 100)
    public SyncResultVO syncDiscountByShop(DiscountQO qo) {
        final String login = qo.getLogin();
        final String taskId = CommonUtils.getTaskId(login);
        final List<DiscountQO.SyncParam> syncParamList = qo.getSyncParamList();
        SyncResultVO vo = SyncResultVO.builder()
                .taskId(taskId)
                .count(0)
                .build();
        if (CommonUtils.isBlank(syncParamList)) {
            return vo;
        }
        boolean b = syncParamList.stream().allMatch(e -> CommonUtils.isBlank(e.getShopId()) || CommonUtils.isNotBlank(e.getDiscountStatus()));
        if (!b) {
            throw new LuxServerErrorException("error params");
        }
        ForkJoinTask<List<DiscountQO.SyncParam>> submit = forkJoinPool.submit(() ->
                syncParamList.parallelStream().flatMap(syncParam -> {
                    final Long shopId = syncParam.getShopId();
                    GetDiscountsListParam param = GetDiscountsListParam.builder()
                            .shopId(shopId)
                            .discountStatus(syncParam.getDiscountStatus())
                            .build();
                    List<GetDiscountsListResult.Discount> discount = shopeeDiscountApi.getDiscountsListAll(param).getData().getDiscount();
                    List<DiscountQO.SyncParam> result = discount.stream().map(e -> {
                        DiscountQO.SyncParam newParam = new DiscountQO.SyncParam();
                        newParam.setShopId(shopId);
                        newParam.setDiscountId(String.valueOf(e.getDiscountId()));
                        return newParam;
                    }).collect(Collectors.toList());
                    return result.stream();
                }).collect(Collectors.toList())
        );
        while (!submit.isDone()) {}

        try {
            List<DiscountQO.SyncParam> syncParams = submit.get();
            vo.setCount(syncParams.size());
            DiscountDetailMessageDTO messageDTO = DiscountDetailMessageDTO.builder()
                    .login(login)
                    .taskId(taskId)
                    .syncParam(syncParams)
                    .build();
            mqProducerService.sendMQ(DiscountDetailConsumer.TAG, taskId, messageDTO);
        } catch (Exception e) {
            log.error("并行getDiscountsListAll出错,login={},taskId={}", login, taskId, e);
        }
        taskExecutorUtils.syncStart(taskId);
        return vo;
    }


    @Override
    public Boolean syncDiscountDetail(DiscountDetailMessageDTO dto) {
        final String login = dto.getLogin();
        final String taskId = dto.getTaskId();
        final List<DiscountQO.SyncParam> syncParam = dto.getSyncParam();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(taskId) || CommonUtils.isBlank(syncParam)) {
            return true;
        }
        forkJoinPool1.execute(() ->
                syncParam.parallelStream().forEach(param -> {
                    final Long shopId = param.getShopId();
                    final String discountId = param.getDiscountId();
                    try {
                        DiscountQO build = DiscountQO.builder()
                                .login(login)
                                .shopId(shopId)
                                .discountId(discountId)
                                .build();
                        syncDiscountDetail(build);
                        taskExecutorUtils.syncProcessing(taskId);
                    } catch (Exception e) {
                        log.error("消费同步discountDetail出错,login={},taskId={},shopId={},discountId={}", login, taskId, shopId, discountId, e);
                    }
                })
        );
        return true;
    }

    @Override
    public Boolean syncDiscountDetail(DiscountQO qo) {
        final String login = qo.getLogin();
        final Long shopId = qo.getShopId();
        final String discountId = qo.getDiscountId();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(shopId) || CommonUtils.isBlank(discountId)) {
            return false;
        }
        GetDiscountDetailParam param = GetDiscountDetailParam.builder()
                .shopId(shopId)
                .discountId(Long.parseLong(discountId))
                .build();
        ShopeeResult<GetDiscountDetailResult> discountDetail = shopeeDiscountApi.getDiscountDetailAll(param);
        if (CommonUtils.isNotBlank(discountDetail.getError())) {
            String msg = discountDetail.getError().getMsg();
            if (msg.contains("The info you queried doesn't exist in database")) {
                //已经删除了
                DiscountQO deleteQO = DiscountQO.builder()
                        .login(login)
                        .shopId(shopId)
                        .discountIds(Arrays.asList(discountId))
                        .build();
                return deleteDiscountDetail(deleteQO);
            }
            //店铺授权过期
            if (msg.contains("your access to shop has expired") || msg.contains("partner and shop has no linked")){
                return true;
            }
            throw new LuxServerErrorException(msg);
        }
        GetDiscountDetailResult discountDetailData = discountDetail.getData();
        ShopeeShopDTO baseShopInfo = shopInfoRedisUtils.getBaseShopInfo(shopId);
        if (CommonUtils.isBlank(baseShopInfo)) {
            return true;
        }
        if (CommonUtils.isNotBlank(baseShopInfo.getLogin()) && !login.equals(baseShopInfo.getLogin())) {
            log.error("同步折扣发现店铺login不匹配,login={},shopId={},shopLoign={}", login, shopId, baseShopInfo.getLogin());
            return true;
        }
        ShopeeDiscountDetailDTO dto = ShopeeDiscountDetailDTO.builder()
                .login(login)
                .shopId(String.valueOf(shopId))
                .shopName(baseShopInfo.getShopName())
                .discountId(discountId)
                .discountName(discountDetailData.getDiscountName())
                .status(discountDetailData.getStatus())
                .startTime(Instant.ofEpochSecond(discountDetailData.getStartTime()))
                .endTime(Instant.ofEpochSecond(discountDetailData.getEndTime()))
                .build();
        findOne(new QueryWrapper<ShopeeDiscountDetail>()
                .eq("login", login)
                .eq("discount_id", discountId))
                .ifPresent(e -> dto.setId(e.getId()));

        saveOrUpdate(dto);
        //同步折扣商品
        DiscountItemQO discountItemQO = DiscountItemQO.builder()
                .login(login)
                .shopId(shopId)
                .discountId(discountId)
                .syncItems(discountDetailData.getItems())
                .build();
        return discountItemService.syncUpdateDiscountItem(discountItemQO);
    }

    @Override
    public IPage<ShopeeDiscountDetailDTO> queryDiscountDetailPage(DiscountPageQO qo) {
        final String login = qo.getLogin();
        QueryWrapper<ShopeeDiscountDetail> qw = new QueryWrapper<ShopeeDiscountDetail>()
                .orderByDesc("start_time")
                .eq("login", login);
        if (CommonUtils.isNotBlank(qo.getShopeeItemId()) || CommonUtils.isNotBlank(qo.getShopeeItemName())) {
            List<ShopeeDiscountItemDTO> shopeeDiscountItemDTOS = discountItemService.listByCondition(qo);
            if (CommonUtils.isBlank(shopeeDiscountItemDTOS)) {
                qo.setDiscountId("-1");
            }
            qw.in("discount_id", shopeeDiscountItemDTOS.stream().map(ShopeeDiscountItemDTO::getDiscountId).distinct().collect(Collectors.toList()));
        }
        if (CommonUtils.isNotBlank(qo.getDiscountId())) {
            qw.eq("discount_id", qo.getDiscountId());
        }
        if (CommonUtils.isNotBlank(qo.getDiscountName())) {
            qw.eq("discount_name", qo.getDiscountName());
        }
        if (CommonUtils.isNotBlank(qo.getLikeDiscountName())) {
            qw.likeRight("discount_name", qo.getLikeDiscountName());
        }
        if (CommonUtils.isNotBlank(qo.getStatuses())) {
            qw.in("status", qo.getStatuses());
        }
        if (CommonUtils.isNotBlank(qo.getStartTimeBegin())) {
            qw.gt("start_time", qo.getStartTimeBegin());
        }
        if (CommonUtils.isNotBlank(qo.getStartTimeEnd())) {
            qw.lt("start_time", qo.getStartTimeEnd());
        }
        if (CommonUtils.isNotBlank(qo.getEndTimeBegin())) {
            qw.gt("end_time", qo.getStartTimeBegin());
        }
        if (CommonUtils.isNotBlank(qo.getEndTime())) {
            qw.lt("end_time", qo.getEndTime());
        }
        if (CommonUtils.isNotBlank(qo.getShopIds())) {
            qw.in("shop_id", qo.getShopIds());
        }
        return page(new Page<>(qo.getPage() + 1, qo.getSize()), qw);
    }

    @Override
    public List<DiscountDeatilVO> conventTOVO(List<ShopeeDiscountDetailDTO> records) {
        if (CommonUtils.isBlank(records)) {
            return new ArrayList<>();
        }
        List<String> collect = records.stream().map(ShopeeDiscountDetailDTO::getDiscountId)
                .collect(Collectors.toList());
        List<DiscountItemCount> itemCount = discountItemService.getItemCount(records.get(0).getLogin(), collect);
        List<DiscountDeatilVO> result = records.stream().map(e -> {
            DiscountDeatilVO vo = new DiscountDeatilVO();
            BeanUtils.copyProperties(e, vo);
            itemCount.stream()
                    .filter(f -> e.getDiscountId().equals(f.getDiscountId()))
                    .findFirst()
                    .ifPresent(f -> vo.setItemCount(f.getCounts()));
            return vo;
        }).collect(Collectors.toList());
        return result;
    }

    @Override
    public Boolean modifyDisocuntDetail(DiscountQO qo) {
        final String login = qo.getLogin();
        final String discountId = qo.getDiscountId();
        final String discountName = qo.getDiscountName();
        final LocalDateTime startTime = qo.getStartTime();
        final LocalDateTime endTime = qo.getEndTime();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(discountId) || CommonUtils.isBlank(startTime) ||
                CommonUtils.isBlank(endTime) || CommonUtils.isBlank(discountName)) {
            return false;
        }
        ShopeeDiscountDetailDTO discountDetailDTO = findOne(new QueryWrapper<ShopeeDiscountDetail>()
                .eq("login", login)
                .eq("discount_id", discountId))
                .orElseThrow(() -> new LuxServerErrorException(handleMessageSource.getMessage(InternationEnum.ITEM_DISCOUNT_NO_EXIST.getCode())));
        UpdateDiscountParam param = UpdateDiscountParam.builder()
                .shopId(Long.parseLong(discountDetailDTO.getShopId()))
                .discountId(Long.parseLong(discountId))
                .discountName(discountName)
                .startTime(startTime.toEpochSecond(ZoneOffset.of("+8")))
                .endTime(endTime.toEpochSecond(ZoneOffset.of("+8")))
                .build();
        ShopeeResult<UpdateDiscountResult> result = shopeeDiscountApi.updateDiscount(param);
        if (CommonUtils.isNotBlank(result) && CommonUtils.isNotBlank(result.getError())) {
            throw new LuxServerErrorException(result.getError().getMsg());
        }
        executor.execute(() -> {
            discountDetailDTO.setDiscountName(discountName);
            discountDetailDTO.setStartTime(startTime.toInstant(ZoneOffset.of("+8")));
            discountDetailDTO.setEndTime(endTime.toInstant(ZoneOffset.of("+8")));
            update(discountDetailDTO);
        });
        return true;
    }

    @Override
    public Boolean deleteDiscountDetail(DiscountQO qo) {
        final String login = qo.getLogin();
        final List<String> discountIds = qo.getDiscountIds();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(discountIds)) {
            return false;
        }
        List<ShopeeDiscountDetailDTO> list = list(new QueryWrapper<ShopeeDiscountDetail>()
                .eq("login", login)
                .in("discount_id", discountIds));
        if (CommonUtils.isBlank(list)) {
            return false;
        }
        ForkJoinTask<List<ShopeeResult<DeleteDiscountResult>>> submit = forkJoinPool.submit(() ->
                list.parallelStream().map(e -> {
                    DeleteDiscountParam param = DeleteDiscountParam.builder()
                            .shopId(Long.parseLong(e.getShopId()))
                            .discountId(Long.parseLong(e.getDiscountId()))
                            .build();
                    ShopeeResult<DeleteDiscountResult> result = shopeeDiscountApi.deleteDiscount(param);
                    if (CommonUtils.isNotBlank(result.getError())
                            && result.getError().getMsg().contains("The info you queried doesn't exist in database")) {
                        //shopee后台已删除，我们系统还存在,允许删除
                        DeleteDiscountResult deleteDiscountResult = new DeleteDiscountResult();
                        deleteDiscountResult.setDiscountId(Long.parseLong(e.getDiscountId()));
                        result.setData(deleteDiscountResult);
                        result.setError(null);
                    }
                    return result;
                }).collect(Collectors.toList())
        );
        while (!submit.isDone()){}
        //删除逻辑
        return handleDelete(login, submit, list);
    }

    @Override
    public Boolean endDiscount(DiscountQO qo) {
        final String login = qo.getLogin();
        final String discountId = qo.getDiscountId();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(discountId)) {
            return false;
        }
        ShopeeDiscountDetailDTO discountDetailDTO = findOne(new QueryWrapper<ShopeeDiscountDetail>()
                .eq("login", login)
                .eq("discount_id", discountId))
                .orElseThrow(() -> new LuxServerErrorException(handleMessageSource.getMessage(InternationEnum.ITEM_DISCOUNT_NO_EXIST.getCode())));
        UpdateDiscountParam param = UpdateDiscountParam.builder()
                .shopId(Long.parseLong(discountDetailDTO.getShopId()))
                .discountId(Long.parseLong(discountId))
                .endDiscount(true)
                .build();
        ShopeeResult<UpdateDiscountResult> result = shopeeDiscountApi.updateDiscount(param);
        if (CommonUtils.isNotBlank(result.getError())) {
            throw new LuxServerErrorException(result.getError().getMsg());
        }
        discountDetailDTO.setEndTime(Instant.now());
        discountDetailDTO.setStatus(DiscountStatus.EXPIRED.getCode());
        return update(discountDetailDTO);
    }


    /**
     * 定时任务
     * 扫描还未开始的折扣任务
     */
    @Scheduled(cron = "0 0/1 * * * ? ")
    public void upcomingDiscount() {
        if (environmentHelper.isEnvironment(EnvironmentHelper.DEV)) {
            return;
        }
        RLock rLock = redissonClient.getLock("upcomingDiscount");
        if (rLock.isLocked()) {
            return;
        }
        if (rLock.tryLock()) {
            log.info("正在扫描折扣信息");
            try {
                QueryWrapper<ShopeeDiscountDetail> qw = new QueryWrapper<ShopeeDiscountDetail>()
                        .nested(e -> e.eq("status", DiscountStatus.UPCOMING.getCode()).lt("start_time", Instant.now()))
                        .or()
                        .nested(f -> f.eq("status", DiscountStatus.ONGOING.getCode()).lt("end_time", Instant.now()))
                        .last("limit 0,500");

                List<ShopeeDiscountDetailDTO> list = list(qw);
                if (CommonUtils.isBlank(list)) {
                    return;
                }
                Map<String, List<ShopeeDiscountDetailDTO>> collect =
                        list.stream().collect(Collectors.groupingBy(ShopeeDiscountDetailDTO::getLogin));
                //获取无效数据的id集合
                Set<Long> ids = new HashSet<Long>();
                collect.entrySet().forEach(e->{
                    final String login = e.getKey();
                    List<DiscountQO.SyncParam> syncParams = e.getValue().stream()
                            .filter(f -> {
                                final Long shopId = Long.valueOf(f.getShopId());
                                ShopeeShopDTO baseShopInfo = shopInfoRedisUtils.getBaseShopInfo(shopId);
                                //解除授权或者login不匹配
                                if (CommonUtils.isBlank(baseShopInfo) || !Objects.equals(login, baseShopInfo.getLogin())) {
                                    ids.add(f.getId());
                                    return false;
                                }
                                return true;
                            })
                            .map(f -> {
                                DiscountQO.SyncParam syncParam = new DiscountQO.SyncParam();
                                syncParam.setShopId(Long.valueOf(f.getShopId()));
                                syncParam.setDiscountId(f.getDiscountId());
                                return syncParam;
                            })
                            .collect(Collectors.toList());
                    if (CommonUtils.isNotBlank(syncParams)){
                        DiscountQO qo = DiscountQO.builder()
                                .login(login)
                                .syncParamList(syncParams)
                                .build();
                        syncDiscountBatch(qo);
                    }
                });
                //批量删除无效的数据
                if (ids!=null && ids.size() > 0){
                    repository.batchDeleteIds(ids);
                }
            } catch ( Exception e) {
                log.error("定时任务扫描折扣信息出错", e);
            }finally {
                rLock.unlock();
            }
        }
    }

    private boolean handleDelete(String login, ForkJoinTask<List<ShopeeResult<DeleteDiscountResult>>> submit, List<ShopeeDiscountDetailDTO> list) {
        try {
            submit.get().stream().filter(e -> CommonUtils.isNotBlank(e.getError()))
                    .findFirst()
                    .ifPresent(e ->{
                        throw new LuxServerErrorException(e.getError().getMsg());
                    });

            List<String> deleteIds = submit.get().stream()
                    .filter(e -> CommonUtils.isBlank(e.getError()))
                    .map(e -> String.valueOf(e.getData().getDiscountId()))
                    .collect(Collectors.toList());
            if (CommonUtils.isBlank(deleteIds)) {
                return false;
            }
            List<Long> ids = list.stream().filter(e -> deleteIds.contains(e.getDiscountId()))
                    .map(ShopeeDiscountDetailDTO::getId)
                    .collect(Collectors.toList());
            if (CommonUtils.isNotBlank(ids)) {
                delete(ids);
            }
            //删除对应商品表和sku表
            executor.execute(() -> {
                discountItemService.deleteByDiscountIds(login, deleteIds);
                discountItemVariationService.deleteByDiscountIds(login, deleteIds);
            });
        } catch (Exception e) {
            log.error("并发删除折扣出错,login={},discountIds={}", login, list.toString(), e);
            throw new LuxServerErrorException(e.getMessage());
        }
        return true;
    }
}
