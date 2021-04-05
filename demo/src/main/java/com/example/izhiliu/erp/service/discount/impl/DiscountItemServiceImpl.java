package com.izhiliu.erp.service.discount.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.izhiliu.core.domain.common.BaseServiceNoLogicImpl;
import com.izhiliu.erp.common.BatchSaveExecutor;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.common.TaskExecutorUtils;
import com.izhiliu.erp.config.internation.HandleMessageSource;
import com.izhiliu.erp.domain.discount.ShopeeDiscountDetail;
import com.izhiliu.erp.domain.discount.ShopeeDiscountItem;
import com.izhiliu.erp.domain.enums.InternationEnum;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.repository.discount.DiscountItemRepository;
import com.izhiliu.erp.service.discount.DiscountDetailService;
import com.izhiliu.erp.service.discount.DiscountItemService;
import com.izhiliu.erp.service.discount.DiscountItemVariationService;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountDetailDTO;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemDTO;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemVariationDTO;
import com.izhiliu.erp.service.discount.mapper.DiscountItemMapper;
import com.izhiliu.erp.service.discount.mq.DiscountItemMessageDTO;
import com.izhiliu.erp.service.discount.mq.DiscountItemPushMessageDTO;
import com.izhiliu.erp.service.item.ShopeeProductMediaService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.ShopeeProductSkuService;
import com.izhiliu.erp.service.item.dto.ShopeeProductMediaDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.service.item.module.channel.ShopeeModelChannel;
import com.izhiliu.erp.service.mq.producer.MQProducerService;
import com.izhiliu.erp.web.rest.discount.qo.DiscountItemCount;
import com.izhiliu.erp.web.rest.discount.qo.DiscountItemQO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountPageQO;
import com.izhiliu.erp.web.rest.discount.vo.DiscountItemVO;
import com.izhiliu.erp.web.rest.discount.vo.DiscountVariationVO;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.vm.TaskExecuteVO;
import com.izhiliu.erp.web.rest.item.vm.VariationMV_V2;
import com.izhiliu.open.shopee.open.sdk.api.discount.ShopeeDiscountApi;
import com.izhiliu.open.shopee.open.sdk.api.discount.param.*;
import com.izhiliu.open.shopee.open.sdk.api.discount.result.AddDiscountResult;
import com.izhiliu.open.shopee.open.sdk.api.discount.result.GetDiscountDetailResult;
import com.izhiliu.open.shopee.open.sdk.api.discount.result.UpdateDiscountResult;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.result.GetItemDetailResult;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:17
 */
@Service
@Slf4j
public class DiscountItemServiceImpl extends BaseServiceNoLogicImpl<ShopeeDiscountItem, ShopeeDiscountItemDTO,
        DiscountItemRepository, DiscountItemMapper> implements DiscountItemService {

    ForkJoinPool forkJoinPool = new ForkJoinPool(10);
    ForkJoinPool forkJoinPool2 = new ForkJoinPool(10);
    private ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(3);

    ThreadPoolExecutor executor = new ThreadPoolExecutor(
            5,
            10,
            1, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadFactoryBuilder().setNameFormat("discount-item-%d").build());



    @Resource
    private ShopeeDiscountApi shopeeDiscountApi;
    @Resource
    private DiscountDetailService discountDetailService;
    @Resource
    private BatchSaveExecutor batchSaveExecutor;
    @Resource
    private ShopeeProductService shopeeProductService;
    @Resource
    private ShopeeModelChannel shopeeModelChannel;
    @Resource
    private MQProducerService mqProducerService;
    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;
    @Resource
    private DiscountItemVariationService discountItemVariationService;
    @Resource
    private DiscountItemService discountItemService;
    @Resource
    private ItemApi itemApi;
    @Resource
    private ShopeeProductMediaService shopeeProductMediaService;
    @Resource
    private HandleMessageSource handleMessageSource;
    @Resource
    private TaskExecutorUtils taskExecutorUtils;




    @Override
    @Deprecated
    public Boolean handleDiscountItem(DiscountItemQO qo) {
        final String login = qo.getLogin();
        final String discountId = qo.getDiscountId();
        final List<DiscountItemQO.Item> items = qo.getItems();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(discountId) || CommonUtils.isBlank(items)) {
            return false;
        }
        ShopeeDiscountDetailDTO discountDetailDTO = discountDetailService.findOne(new QueryWrapper<ShopeeDiscountDetail>()
                .eq("login", login)
                .eq("discount_id", discountId))
                .orElseThrow(() -> new LuxServerErrorException(handleMessageSource.getMessage(InternationEnum.ITEM_DISCOUNT_NO_EXIST.getCode())));
        qo.setShopId(Long.valueOf(discountDetailDTO.getShopId()));
        //并发调取api
        Map<String, List<DiscountItemQO.Item>> stringListMap = items.stream()
                .filter(e -> CommonUtils.isNotBlank(e.getType()))
                .collect(Collectors.groupingBy(DiscountItemQO.Item::getType));
        ForkJoinTask<?> submit = forkJoinPool.submit(() -> stringListMap.entrySet().parallelStream().forEach(e -> callToApi(e, qo)));
        while (!submit.isDone()){}

        //使用延迟线程，shopee接口数据有延迟
        scheduledThreadPool.schedule(() -> {
            GetDiscountDetailParam param = GetDiscountDetailParam.builder()
                    .discountId(Long.parseLong(discountId))
                    .shopId(Long.parseLong(discountDetailDTO.getShopId()))
                    .build();
            GetDiscountDetailResult data = shopeeDiscountApi.getDiscountDetailAll(param).getData();
            qo.setSyncItems(data.getItems());
            discountItemService.syncUpdateDiscountItem(qo);
        }, 2, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public TaskExecuteVO handleDiscountItemV2(DiscountItemQO qo) {
        final String login = qo.getLogin();
        final String discountId = qo.getDiscountId();
        final List<DiscountItemQO.Item> items = qo.getItems();
        final String taskId = qo.getTaskId();
        TaskExecuteVO vo = TaskExecuteVO.builder()
                .taskId(taskId)
                .taskType(TaskExecuteVO.TASK_TYPE_DISCOUNT_ITEM_EDIT)
                .code(TaskExecuteVO.CODE_PENDING)
                .build();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(discountId) || CommonUtils.isBlank(items)) {
            throw new LuxServerErrorException("param error");
        }
        ShopeeDiscountDetailDTO discountDetailDTO = discountDetailService.findOne(new QueryWrapper<ShopeeDiscountDetail>()
                .eq("login", login)
                .eq("discount_id", discountId))
                .orElseThrow(() -> new LuxServerErrorException(handleMessageSource.getMessage(InternationEnum.ITEM_DISCOUNT_NO_EXIST.getCode())));
        qo.setShopId(Long.valueOf(discountDetailDTO.getShopId()));

        //保存任务
        Map<String, List<DiscountItemQO.Item>> stringListMap = items.stream()
                .filter(e -> CommonUtils.isNotBlank(e.getType()))
                .collect(Collectors.groupingBy(DiscountItemQO.Item::getType));
        //初始化任务
        initTask(stringListMap, qo);

        executor.execute(() -> {
            ForkJoinTask<?> submit = forkJoinPool.submit(() -> stringListMap.entrySet().parallelStream().forEach(e -> callToApi(e, qo)));
            while (!submit.isDone()){}
            //任务结束
            endTask(qo);
            //使用延迟线程，shopee接口数据有延迟
            scheduledThreadPool.schedule(() -> {
                GetDiscountDetailParam param = GetDiscountDetailParam.builder()
                        .discountId(Long.parseLong(discountId))
                        .shopId(Long.parseLong(discountDetailDTO.getShopId()))
                        .build();
                GetDiscountDetailResult data = shopeeDiscountApi.getDiscountDetailAll(param).getData();
                qo.setSyncItems(data.getItems());
                discountItemService.syncUpdateDiscountItem(qo);
            }, 2, TimeUnit.SECONDS);
        });
        return vo;
    }

    private void endTask(DiscountItemQO qo) {
        TaskExecuteVO.TaskDetail taskDetail = TaskExecuteVO.TaskDetail.builder()
                .taskId(qo.getTaskId())
                .endCode(TaskExecuteVO.CODE_SUCCESS)
                .build();
        taskExecutorUtils.handleTaskSet(taskDetail);
    }

    private void initTask(Map<String, List<DiscountItemQO.Item>> stringListMap, DiscountItemQO qo) {
        final String taskId = qo.getTaskId();
        final Locale locale = qo.getLocale();
        stringListMap.entrySet().forEach(e -> {
            TaskExecuteVO.TaskDetail taskDetail = TaskExecuteVO.TaskDetail.builder()
                    .taskId(taskId)
                    .code(TaskExecuteVO.CODE_PENDING)
                    .detailId(taskId.concat(e.getKey()))
                    .detailName(getTaskDetailName(e.getKey(), locale))
                    .build();
            taskExecutorUtils.handleTaskSet(taskDetail);
        });
    }

    private String getTaskDetailName(String key, Locale locale) {
        return handleMessageSource.getMessage("discount.operate.".concat(key), locale);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Boolean syncUpdateDiscountItem(DiscountItemQO qo) {
        final String login = qo.getLogin();
        final String discountId = qo.getDiscountId();
        final Long shopId = qo.getShopId();
        final List<GetDiscountDetailResult.Item> itemList = qo.getSyncItems();
        Boolean needClean = CommonUtils.isNotBlank(qo.getNeedToClean()) ? qo.getNeedToClean() : Boolean.TRUE;
        //删除前先获取元数据
        DiscountPageQO queryQO = new DiscountPageQO();
        queryQO.setLogin(login);
        queryQO.setDiscountId(discountId);
        List<ShopeeDiscountItemDTO> shopeeDiscountItemDTOS = listByCondition(queryQO);
        qo.setOriginItems(shopeeDiscountItemDTOS);
        //删除
        repository.deleteByLoginAndDiscountId(login, discountId);
        if (CommonUtils.isNotBlank(itemList)) {
            List<ShopeeDiscountItemDTO> collect = itemList.stream().map(e -> {
                ShopeeDiscountItemDTO shopeeDiscountItemDTO = ShopeeDiscountItemDTO.builder()
                        .login(login)
                        .discountId(discountId)
                        .itemId(e.getItemId())
                        .itemName(e.getItemName())
                        .variationTier(CommonUtils.isNotBlank(e.getVariations()) ? 2 : 0)
                        .purchaseLimit(e.getPurchaseLimit())
                        .itemOriginalPrice((long) (e.getItemOriginalPrice() * 100))
                        .itemPromotionPrice((long) (e.getItemPromotionPrice() * 100))
                        .stock(e.getStock())
                        .build();
                return shopeeDiscountItemDTO;
            }).collect(Collectors.toList());
            batchSaveExecutor.getDiscountItemResult(collect);
        }
        //同步商品的折扣信息
        syncShopeeItem(qo);
        return true;
    }


    private void syncShopeeItem(DiscountItemQO qo) {
        List<GetDiscountDetailResult.Item> itemList = qo.getSyncItems();
        List<ShopeeDiscountItemDTO> originItems = qo.getOriginItems();
        Boolean needToSyncItem = CommonUtils.isBlank(qo.getNeedToSyncItem()) ? true : qo.getNeedToSyncItem();
        if (CommonUtils.isBlank(itemList) && CommonUtils.isBlank(originItems)) {
            return;
        }
        //同步后的商品id
        List<Long> shopeeItemId = itemList.stream()
                .map(GetDiscountDetailResult.Item::getItemId)
                .distinct()
                .collect(Collectors.toList());
        //同步前的商品id
        List<Long> originItemId = originItems.stream().map(ShopeeDiscountItemDTO::getItemId).collect(Collectors.toList());
        //综合影响到的商品Id
        List<Long> itemIds = Stream.of(shopeeItemId, originItemId)
                .flatMap(e -> e.stream())
                .distinct()
                .collect(Collectors.toList());

        List<ShopeeProduct> shopeeProductDTOList = shopeeProductService.findShopeeProductList(itemIds);

        //同步折扣商品sku
        discountItemVariationService.syncVariations(qo);

        if (needToSyncItem) {
            shopeeProductDTOList.forEach(item -> {
                final Long itemId = item.getShopeeItemId();
                final String loginId = item.getLoginId();
                final Long productId = item.getId();
                Long shopId = item.getShopId();
                if (CommonUtils.isBlank(shopId) || CommonUtils.isBlank(itemId) || CommonUtils.isBlank(loginId)) {
                    return;
                }
                DiscountItemMessageDTO dto = DiscountItemMessageDTO.builder()
                        .login(loginId)
                        .shopeeItemId(itemId)
                        .productId(productId)
                        .shopId(shopId)
                        .build();
                mqProducerService.sendMQ(DiscountItemMessageDTO.TAG, DiscountItemMessageDTO.KEY + productId, dto);
            });
        }
    }


    private void callToApi(Map.Entry<String, List<DiscountItemQO.Item>> e, DiscountItemQO qo) {
        DiscountItemQO discountItemQO = new DiscountItemQO();
        BeanUtils.copyProperties(qo, discountItemQO);
        discountItemQO.setItems(e.getValue());
        final String taskId = discountItemQO.getTaskId();
        TaskExecuteVO.TaskDetail taskDetail = TaskExecuteVO.TaskDetail.builder()
                .taskId(taskId)
                .code(TaskExecuteVO.CODE_SUCCESS)
                .detailId(taskId + e.getKey())
                .detailName(getTaskDetailName(e.getKey(), qo.getLocale()))
                .build();
        try {
            switch (e.getKey()) {
                case DiscountItemQO.TYPE_ADD:
                    callAddDiscountItemApi(discountItemQO);
                    break;
                case DiscountItemQO.TYPE_DELETE:
                    callDeleteDiscountItemApi(discountItemQO);
                    break;
                case DiscountItemQO.TYPE_MODIFY:
                    callModifyDiscountItemApi(discountItemQO);
                    break;
            }
        } catch (LuxServerErrorException e1) {
            taskDetail.setCode(TaskExecuteVO.CODE_FAILD);
            taskDetail.setErrorMessage(e1.getTitle());
        } catch (Exception e1) {
            taskDetail.setCode(TaskExecuteVO.CODE_FAILD);
            taskDetail.setErrorMessage("系统繁忙");
        } finally {
            taskExecutorUtils.handleTaskSet(taskDetail);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void callModifyDiscountItemApi(DiscountItemQO qo) {
        final List<DiscountItemQO.Item> items = qo.getItems();
        final Long shopId = qo.getShopId();
        final String discountId = qo.getDiscountId();
        final String login = qo.getLogin();
        if (CommonUtils.isBlank(items) || CommonUtils.isBlank(discountId)) {
            return;
        }
        List<DiscountItem> discountItemList = items.stream()
                .filter(e -> Objects.equals(DiscountItemQO.TYPE_MODIFY, e.getType()))
                .map(item -> {
            DiscountItem discountItem = DiscountItem.builder()
                    .purchaseLimit(item.getPurchaseLimit())
                    .itemId(item.getShopeeItemId())
                    .itemPromotionPrice(item.getItemPromotionPrice())
                    .variations(item.getVariations())
                    .build();
            return discountItem;
        }).collect(Collectors.toList());
        UpdateDiscountParam param = UpdateDiscountParam.builder()
                .shopId(shopId)
                .discountId(Long.parseLong(discountId))
                .items(discountItemList)
                .build();
        ShopeeResult<UpdateDiscountResult> result = shopeeDiscountApi.updateDiscountItems(param);
        if (result.isResult() && CommonUtils.isNotBlank(result.getData().getErrors())) {
            log.error("更新折扣商品出错,login={},disocuntId={},login={},error={}", login, discountId, login, result.getData().getErrors().toString());
            throw new LuxServerErrorException(result.getData().getErrors().stream().map(UpdateDiscountResult.Error::getErrorMsg)
                    .collect(Collectors.joining("\n")));
        }
    }

    /**
     * 删除折扣商品
     * @param qo
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void callDeleteDiscountItemApi(DiscountItemQO qo) {
        final List<DiscountItemQO.Item> items = qo.getItems();
        final Long shopId = qo.getShopId();
        final String discountId = qo.getDiscountId();
        final String login = qo.getLogin();
        final String taskId = qo.getTaskId();
        if (CommonUtils.isBlank(items) || CommonUtils.isBlank(discountId)) {
            return;
        }
        ForkJoinTask<List<Long>> submit = forkJoinPool2.submit(() ->
                items.parallelStream()
                        .filter(e -> Objects.equals(DiscountItemQO.TYPE_DELETE, e.getType()))
                        .map(discountItemDTO -> {
                    final Long shopeeItemId = discountItemDTO.getShopeeItemId();
                    if (CommonUtils.isNotBlank(discountItemDTO.getVariations())) {
                        discountItemDTO.getVariations().forEach(f -> {
                            DeleteDiscountParam param = DeleteDiscountParam.builder()
                                    .itemId(shopeeItemId)
                                    .shopId(shopId)
                                    .discountId(Long.parseLong(discountId))
                                    .variationId(f.getVariationId())
                                    .build();
                            shopeeDiscountApi.deleteDiscountItem(param);
                        });
                    } else {
                        DeleteDiscountParam param = DeleteDiscountParam.builder()
                                .itemId(shopeeItemId)
                                .shopId(shopId)
                                .discountId(Long.parseLong(discountId))
                                .variationId(discountItemDTO.getVariationId())
                                .build();
                        shopeeDiscountApi.deleteDiscountItem(param);
                    }
                    return shopeeItemId;
                }).filter(CommonUtils::isNotBlank).collect(Collectors.toList()));
        while (!submit.isDone()) {}
        try {
            submit.get();
        } catch (Exception e) {
            log.error("并行删除折扣商品出错,login={},discountId={},itemId={}", login, discountId, items.toString(), e);
        }
    }

    @Override
    public List<ShopeeDiscountItemDTO> listByCondition(DiscountPageQO qo) {
        QueryWrapper<ShopeeDiscountItem> qw = new QueryWrapper<ShopeeDiscountItem>().eq("login", qo.getLogin());
        if (CommonUtils.isNotBlank(qo.getShopeeItemId())) {
            qw.eq("item_id", qo.getShopeeItemId());
        }
        if (CommonUtils.isNotBlank(qo.getShopeeItemName())) {
            qw.likeRight("item_name", qo.getShopeeItemName());
        }
        if (CommonUtils.isNotBlank(qo.getDiscountId())) {
            qw.eq("discount_id", qo.getDiscountId());
        }
        return list(qw);
    }

    @Override
    public List<DiscountItemCount> getItemCount(String login, List<String> discountIds) {
        return repository.getItemCount(login, discountIds);
    }

    @Override
    public IPage<ShopeeDiscountItemDTO> queryItemPage(DiscountPageQO qo) {
        final String login = qo.getLogin();
        QueryWrapper<ShopeeDiscountItem> qw = new QueryWrapper<ShopeeDiscountItem>().eq("login", login);
        if (CommonUtils.isNotBlank(qo.getShopeeItemName())) {
            qw.likeRight("item_name", qo.getShopeeItemName());
        }
        if (CommonUtils.isNotBlank(qo.getShopeeItemId())) {
            qw.eq("item_id", qo.getShopeeItemId());
        }
        if (CommonUtils.isNotBlank(qo.getDiscountId())) {
            qw.eq("discount_id", qo.getDiscountId());
        }
        return page(new Page<>(qo.getPage() + 1, qo.getSize()), qw);
    }

    @Override
    public List<DiscountItemVO> conventTOVO(List<ShopeeDiscountItemDTO> records) {
        if (CommonUtils.isBlank(records)) {
            return new ArrayList<>();
        }
        final String login = records.get(0).getLogin();
        /**
         * 获取商品折扣sku
         */
        List<String> discountIds = records.stream().map(ShopeeDiscountItemDTO::getDiscountId)
                .collect(Collectors.toList());
        List<ShopeeDiscountItemVariationDTO> variationDTOList = discountItemVariationService.listByDiscountIdAndLogin(login, discountIds);

        //获取全sku
        List<VariationMV_V2> skuInfo = getSkuInfo(records);

        List<DiscountItemVO> result = records.stream().map(e -> {
            DiscountItemVO vo = new DiscountItemVO();
            BeanUtils.copyProperties(e, vo);
            List<DiscountVariationVO> variations = getVariations(skuInfo, variationDTOList, e);
            vo.setVariations(variations);
            vo.setItemOriginalPrice(e.getItemOriginalPrice() / 100f);
            vo.setItemPromotionPrice(e.getItemPromotionPrice() / 100f);
            return vo;
        }).collect(Collectors.toList());

        return result;
    }

    private List<DiscountVariationVO> getVariations(List<VariationMV_V2> skuInfo, List<ShopeeDiscountItemVariationDTO> variationDTOList, ShopeeDiscountItemDTO e) {
        List<DiscountVariationVO> collect = skuInfo.stream()
                .filter(f -> Objects.equals(f.getShopeeItemId(), e.getItemId()) && CommonUtils.isNotBlank(f.getShopeeVariationId()))
                .map(f -> {
                    DiscountVariationVO variationVO = DiscountVariationVO.builder()
                            .variationId(f.getShopeeVariationId())
                            .variationName(f.getName())
                            .variationOriginalPrice(CommonUtils.isNotBlank(f.getPrice()) ? f.getPrice() : 0)
                            .variationStock(CommonUtils.isNotBlank(f.getStock()) ? f.getStock() : 0)
                            .enable(false)
                            .build();
                    variationDTOList.stream()
                            .filter(x -> x.getVariationId() == f.getShopeeVariationId().longValue())
                            .findFirst()
                            .ifPresent(x -> {
                                variationVO.setVariationId(CommonUtils.isNotBlank(x.getVariationId()) ? x.getVariationId() : 0);
                                variationVO.setVariationName(x.getVariationName());
                                variationVO.setVariationOriginalPrice(x.getVariationOriginalPrice() / 100f);
                                variationVO.setVariationPromotionPrice(x.getVariationPromotionPrice() / 100f);
                                variationVO.setVariationStock(x.getVariationStock());
                                variationVO.setEnable(true);
                            });
                    return variationVO;
                }).collect(Collectors.toList());

        if (e.getVariationTier() == 2 && CommonUtils.isBlank(collect)) {
            //商品在我们系统未同步
            return variationDTOList.stream()
                    .filter(x -> Objects.equals(x.getItemId(), e.getItemId()))
                    .map(x -> {
                        DiscountVariationVO variationVO = DiscountVariationVO.builder()
                                .variationId(x.getVariationId())
                                .variationName(x.getVariationName())
                                .variationOriginalPrice(x.getVariationOriginalPrice() / 100f)
                                .variationPromotionPrice(x.getVariationPromotionPrice() / 100f)
                                .variationStock(x.getVariationStock())
                                .enable(true)
                                .build();
                        return variationVO;
                    }).collect(Collectors.toList());

        }
        return collect;
    }

    @Override
    public Boolean deleteByDiscountIds(String login, List<String> discountIds) {
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(discountIds)) {
            return false;
        }
        return repository.deleteByLoginAndDiscountIds(login, discountIds);
    }

    @Override
    public boolean syncProductSku(DiscountItemMessageDTO dto) {
        final String login = dto.getLogin();
        final Long shopId = dto.getShopId();
        final Long shopeeItemId = dto.getShopeeItemId();
        final Long productId = dto.getProductId();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(shopId) || CommonUtils.isBlank(shopeeItemId) || CommonUtils.isBlank(productId)) {
            return false;
        }
        List<ShopeeProductSkuDTO> skuListByProductId = shopeeProductSkuService.getSkuListByProductId(productId);
        if (CommonUtils.isBlank(skuListByProductId)) {
            return true;
        }
        ShopeeResult<GetItemDetailResult> itemDetail = itemApi.getItemDetail(shopId, shopeeItemId);
        if (CommonUtils.isNotBlank(itemDetail.getError())) {
            return true;
        }
        GetItemDetailResult.ItemBean item = itemDetail.getData().getItem();
        List<GetItemDetailResult.ItemBean.VariationBean> variations = item.getVariations();
        if (CommonUtils.isNotBlank(variations)) {
            //多sku
            skuListByProductId.forEach(e -> {
                variations.stream().filter(f -> Objects.equals(e.getShopeeVariationId(), f.getVariationId()))
                        .findFirst()
                        .ifPresent(f -> {
                            boolean variationDiscount = isDiscount(f.getDiscountId());
                            e.setPrice(f.getPrice());
                            e.setOriginalPrice(f.getOriginalPrice());
                            e.setDiscount(variationDiscount ? item.getPrice() : null);
                            e.setDiscountId(variationDiscount ? Long.valueOf(f.getDiscountId()) : null);
                        });
            });
        } else {
            //单品
            skuListByProductId.forEach(e -> {
                e.setPrice(item.getPrice());
                e.setOriginalPrice(item.getOriginalPrice());
                e.setDiscount(isDiscount(item.getDiscountId()) ? item.getPrice() : null);
                e.setDiscountId(isDiscount(item.getDiscountId()) ? Long.valueOf(item.getDiscountId()) : null);
            });
        }
        shopeeProductSkuService.batchUpdate(skuListByProductId);

        Long discountId = skuListByProductId.stream()
                .map(ShopeeProductSkuDTO::getDiscountId)
                .filter(this::isDiscount)
                .findFirst()
                .orElseGet(() -> 0L);

        //修改附属表折扣信息
        ShopeeProductMediaDTO shopeeProductMedia = new ShopeeProductMediaDTO();
        shopeeProductMedia.setProductId(productId);
        shopeeProductMedia.setDiscountActivityId(discountId);
        shopeeProductMedia.setDiscountActivity(isDiscount(discountId));
        shopeeProductMediaService.updateByProductId(shopeeProductMedia);
        return true;
    }

    @Override
    public Boolean publishToSyncDiscount(DiscountItemPushMessageDTO dto) {
        final String login = dto.getLogin();
        final Long shopeeItemId = dto.getShopeeItemId();
        final Long shopId = dto.getShopId();
        final Long discountId = dto.getDiscountId();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(shopeeItemId) ||
                CommonUtils.isBlank(shopId) || CommonUtils.isBlank(discountId)) {
            return true;
        }
        GetDiscountDetailParam param = GetDiscountDetailParam.builder()
                .discountId(discountId)
                .shopId(shopId)
                .build();
        ShopeeResult<GetDiscountDetailResult> result = shopeeDiscountApi.getDiscountDetailAll(param);
        if (CommonUtils.isNotBlank(result.getError())) {
            log.error("发布商品同步商品信息出错,login={},discountId={},shopId={},itemId={},msg={}", login, discountId, shopeeItemId, shopeeItemId, result.getError().getMsg());
            return true;
        }
        GetDiscountDetailResult data = result.getData();
        DiscountItemQO discountItemQO = DiscountItemQO.builder()
                .login(login)
                .shopId(shopId)
                .discountId(String.valueOf(discountId))
                .syncItems(data.getItems())
                .needToSyncItem(false)
                .build();
        return discountItemService.syncUpdateDiscountItem(discountItemQO);
    }

    private boolean isDiscount(int discountId) {
        return CommonUtils.isNotBlank(discountId) && discountId != 0;
    }

    private boolean isDiscount(Long discountId) {
        return CommonUtils.isNotBlank(discountId) && discountId != 0;
    }

    private List<VariationMV_V2> getSkuInfo(List<ShopeeDiscountItemDTO> records) {
        if (CommonUtils.isBlank(records)) {
            return new ArrayList<>();
        }
        List<Long> shopeeItemIds = records.stream().map(ShopeeDiscountItemDTO::getItemId)
                .collect(Collectors.toList());
        List<ShopeeProduct> shopeeProductList = shopeeProductService.findShopeeProductList(shopeeItemIds);
        if (CommonUtils.isBlank(shopeeProductList)) {
            return new ArrayList<>();
        }
        List<Long> productIds = shopeeProductList.stream().map(ShopeeProduct::getId)
                .collect(Collectors.toList());
        List<VariationMV_V2> variationMV = shopeeProductSkuService.selectVariationMV_V2ByProductIds(productIds);
        variationMV.forEach(e -> {
            shopeeProductList.stream().filter(f -> Objects.equals(e.getProductId(), f.getId()))
                    .findFirst()
                    .ifPresent(f -> e.setShopeeItemId(f.getShopeeItemId()));
        });
        //获取商品图片
        List<ShopeeProductMediaDTO> shopeeProductMediaDTOS = shopeeProductMediaService.selectMainImagsByProductId(productIds);
        records.forEach(e -> {
            shopeeProductList.stream().filter(f -> Objects.equals(e.getItemId(), f.getShopeeItemId()))
                    .findFirst()
                    .ifPresent(f -> {
                        e.setProductId(f.getId());
                        e.setOnlineUrl(f.getOnlineUrl());
                        e.setCurrency(f.getCurrency());
                    });
            shopeeProductMediaDTOS.stream().filter(f -> Objects.equals(e.getProductId(), f.getProductId()))
                    .findFirst()
                    .ifPresent(f -> e.setImages(f.getImages()));
        });
        return variationMV;
    }

    /**
     * 调用新增折扣商品接口
     * @param qo
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void callAddDiscountItemApi(DiscountItemQO qo) {
        final List<DiscountItemQO.Item> items = qo.getItems();
        final Long shopId = qo.getShopId();
        final String discountId = qo.getDiscountId();
        final String taskId = qo.getTaskId();
        if (CommonUtils.isBlank(items) || CommonUtils.isBlank(discountId)) {
            return;
        }
        AtomicInteger i = new AtomicInteger(0);
        AtomicInteger number = new AtomicInteger(1);
        List<DiscountItem> discountItemList = items.stream()
                .filter(e -> Objects.equals(DiscountItemQO.TYPE_ADD, e.getType()))
                .map(item -> {
                    DiscountItem discountItem = DiscountItem.builder()
                            .purchaseLimit(item.getPurchaseLimit())
                            .itemId(item.getShopeeItemId())
                            .itemPromotionPrice(item.getItemPromotionPrice())
                            .variations(item.getVariations())
                            .purchaseLimit(item.getPurchaseLimit())
                            .sort(i.get())
                            .build();
                    if (number.get() % 5 == 0) {
                        i.getAndIncrement();
                    }
                    number.getAndIncrement();
                    return discountItem;
                }).collect(Collectors.toList());
        Map<Integer, List<DiscountItem>> collect = discountItemList.stream().collect(Collectors.groupingBy(DiscountItem::getSort));
        ForkJoinTask<List<ShopeeResult<AddDiscountResult>>> submit = forkJoinPool2.submit(() -> collect.entrySet().parallelStream().map(e -> {
            AddDiscountParam param = AddDiscountParam.builder()
                    .shopId(shopId)
                    .discountId(Long.parseLong(discountId))
                    .items(e.getValue())
                    .build();
            return shopeeDiscountApi.addDiscountItem(param);
//            return new ShopeeResult<AddDiscountResult>();
        }).collect(Collectors.toList()));
        while (!submit.isDone()){}
        try {
            String errorMsg = submit.get().stream().map(result -> {
                if (result.isResult() && CommonUtils.isNotBlank(result.getData().getErrors())) {
                    StringBuffer sb = new StringBuffer();
                    log.error("新增折扣商品api出错,shopId={},discountId={},errorMsg={}", shopId, discountId, result);
                    return result.getData().getErrors().stream().map(this::errorhandleResultError)
                            .collect(Collectors.joining("\n"));
                }
                if (CommonUtils.isNotBlank(result.getError()) && CommonUtils.isNotBlank(result.getError().getMsg())) {
                    return result.getError().getMsg();
                }
                return null;
            }).filter(CommonUtils::isNotBlank).collect(Collectors.joining(","));
            if (CommonUtils.isNotBlank(errorMsg)) {
                throw new LuxServerErrorException(errorMsg);
            }
        } catch (LuxServerErrorException e) {
            throw new LuxServerErrorException(e.getTitle());
        } catch (Exception e) {
            throw new LuxServerErrorException("系统繁忙");
        }



    }

    @Override
    public List<ShopeeDiscountItemDTO> getListByDiscountId(String login, String discountId) {
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(discountId)) {
            return new ArrayList<>();
        }
        return list(new QueryWrapper<ShopeeDiscountItem>().eq("login", login).eq("discount_id", discountId));
    }

    private String errorhandleResultError(AddDiscountResult.Error error) {
        if (error.getErrorMsg().indexOf("has participated in other promotion") != -1) {
            return handleMessageSource.getMessage(InternationEnum.DISCOUNT_HASPARTICIPATED_IN_OTHER_PROMOTION.getCode(), new String[]{String.valueOf(error.getItemId())});
        } else if (error.getErrorMsg().indexOf("Promotion price must be lower than origin price") != -1) {
            return handleMessageSource.getMessage(InternationEnum.DISCOUNT_PROMOTION_PRICE_LOWER_THAN_ORIGIN_PRICE.getCode(), new String[]{String.valueOf(error.getItemId())});
        }
        return error.getErrorMsg();
    }

}
