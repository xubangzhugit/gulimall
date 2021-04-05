package com.izhiliu.erp.service.item.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.common.TaskExecutorUtils;
import com.izhiliu.erp.domain.item.ItemCommentDetail;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.repository.item.ItemCommentDetailRepository;
import com.izhiliu.erp.repository.item.ShopeeProductRepository;
import com.izhiliu.erp.service.discount.mq.*;
import com.izhiliu.erp.service.item.ShopeeProductCommentService;
import com.izhiliu.erp.service.item.dto.AutoCommentDTO;
import com.izhiliu.erp.service.item.dto.CommentCallbackDTO;
import com.izhiliu.erp.service.item.dto.ProductSearchStrategyDTO;
import com.izhiliu.erp.service.item.dto.ShopeePullMessageDTO;
import com.izhiliu.erp.service.mq.producer.MQProducerService;
import com.izhiliu.erp.web.rest.discount.vo.SyncResultVO;
import com.izhiliu.erp.web.rest.item.param.CommentSyncQO;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.param.GetCommentParam;
import com.izhiliu.open.shopee.open.sdk.api.item.result.GetCommentResult;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Twilight
 * @date 2021/2/7 15:36
 */
@Slf4j
@Service
public class ShopeeProductCommentServiceImpl implements ShopeeProductCommentService {

    public static final String TAG_CHECK_AUTO_COMMENT = "TAG_CHECK_AUTO_COMMENT";
    public static final String KEY_AUTO_COMMENT = "AUTO_COMMENT:";

    @Resource
    private MQProducerService mqProducerService;
    @Resource
    protected ItemApi itemApi;
    @Resource
    private TaskExecutorUtils taskExecutorUtils;
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private ItemCommentDetailRepository itemCommentDetailRepository;
    @Resource
    private ShopeeProductRepository shopeeProductRepository;

    private static final int CORE_POOL_SIZE = 10;
    private static final int MAX_POOL_SIZE = 20;
    private static final int QUEUE_CAPACITY = 50;
    private static final Long KEEP_ALIVE_TIME = 1L;

    ThreadPoolExecutor executor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactoryBuilder()
                    .setNameFormat("syncShopComment").build());

    ForkJoinPool forkJoinPool = new ForkJoinPool(10);

    /**
     * 批量同步产品评论
     *
     * @param commentSyncQO
     * @return
     */
    @Override
    public SyncResultVO syncProductComment(CommentSyncQO commentSyncQO) {
        final String login = commentSyncQO.getLogin();
        final List<CommentSyncQO.ItemInfo> itemInfoList = commentSyncQO.getItemInfoList();
        final String taskId = CommonUtils.getTaskId(login);
        taskExecutorUtils.initSyncTask(taskId, new Long((long) itemInfoList.size()), null);
        SyncResultVO vo = SyncResultVO.builder()
                .taskId(taskId)
                .build();
        CommentSyncDTO dto = CommentSyncDTO
                .builder()
                .login(login)
                .taskId(taskId)
                .itemInfoList(itemInfoList)
                .build();
        log.info("同步商品评论信息,login:{},key:{}", login, taskId);
        mqProducerService.sendMQ(CommentSyncConsumer.TAG, taskId, dto);
        return vo;
    }

    /**
     * 同步店铺产品评论
     *
     * @param commentSyncQO
     * @return
     */
    @Override
    public SyncResultVO syncShopProductComment(CommentSyncQO commentSyncQO) {
        final String taskId = CommonUtils.getTaskId(commentSyncQO.getLogin());
        executor.execute(() -> handleSyncShopProductComment(taskId, commentSyncQO));
        return SyncResultVO.builder()
                .taskId(taskId)
                .build();
    }

    private boolean handleSyncShopProductComment(String taskId, CommentSyncQO commentSyncQO) {
        //获取店铺在线商品
        final String login = commentSyncQO.getLogin();
        final List<Long> shopIdList = commentSyncQO.getShopIdList();
        ProductSearchStrategyDTO productSearchStrategyDTO = new ProductSearchStrategyDTO();
        productSearchStrategyDTO.setPublishShops(shopIdList);
        productSearchStrategyDTO.setOnline(true);
        long pageNum = 0;
        long size = 50;
        long total = 0;
        //初始化进度条
        taskExecutorUtils.initSyncTask(taskId, total, null);
        while (true) {
            pageNum += 1;
            final Page page = new Page(pageNum, size);
            try {
                IPage<ShopeeProduct> shopeeProductIPage = shopeeProductRepository.searchShopProductByLoginId(page, login, productSearchStrategyDTO);
                total = shopeeProductIPage.getTotal();
                List<ShopeeProduct> records = shopeeProductIPage.getRecords();
                taskExecutorUtils.incrementSyncHash(taskId, "total", records.size());
                List<CommentSyncQO.ItemInfo> itemInfoList = records.stream().map(e -> {
                    CommentSyncQO.ItemInfo itemInfo = new CommentSyncQO.ItemInfo();
                    itemInfo.setShopId(e.getShopId());
                    itemInfo.setItemId(e.getShopeeItemId());
                    return itemInfo;
                }).collect(Collectors.toList());
                if (CommonUtils.isBlank(itemInfoList)) {
                    break;
                }
                CommentSyncDTO dto = CommentSyncDTO
                        .builder()
                        .login(login)
                        .taskId(taskId)
                        .itemInfoList(itemInfoList)
                        .build();
                log.info("同步店铺商品评论信息,login:{},key:{}", login, taskId);
                mqProducerService.sendMQ(CommentSyncConsumer.TAG, taskId, dto);
            } catch (Exception e) {
                break;
            }
            if (total <= pageNum * size) {
                break;
            }
        }
        return true;
    }

    /**
     * 同步评论
     *
     * @param dto
     * @return
     */
    @Override
    public boolean syncComment(CommentSyncDTO dto) {
        final String login = dto.getLogin();
        final String taskId = dto.getTaskId();
        final List<CommentSyncQO.ItemInfo> itemInfoList = dto.getItemInfoList();
        ForkJoinTask<Integer> submit = forkJoinPool.submit(() ->
                itemInfoList
                        .parallelStream()
                        .map(e -> {
                            try {
                                handleSyncComment(e, login);
                                return 1;
                            } catch (Exception e1) {
                                return 0;
                            }
                        })
                        .reduce(Integer::sum)
                        .get()
        );

        Integer successNum = 0;
        try {
            successNum = submit.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        //总进度
        taskExecutorUtils.incrementSyncHash(taskId, "success", successNum);
        taskExecutorUtils.incrementSyncHash(taskId, ShopeePullMessageDTO.HANDLE_TOTAL, itemInfoList.size());
        return true;
    }

    /**
     * 处理评论数据
     * 1、查询数据
     * 2、去除已存在数据
     * 3、添加数据
     * @param dto
     * @return
     */
    @Override
    public boolean handleCommentDetail(CommentDetailDTO dto) {
        final String login = dto.getLogin();
        final String shopId = dto.getShopId();
        final List<GetCommentResult.ItemCmtListBean> itemCmtList = dto.getItemCmtList();
        List<Long> cmtIds = itemCmtList.stream().map(GetCommentResult.ItemCmtListBean::getCmtId).collect(Collectors.toList());
        List<ItemCommentDetail> itemCommentDetails = mongoTemplate.find(new Query().addCriteria(Criteria.where("cmtId").in(cmtIds)), ItemCommentDetail.class);
        //添加数据
        List<ItemCommentDetail> itemCommentDetailList = itemCmtList
                .stream()
                .filter(e -> !itemCommentDetails.stream().anyMatch(i -> Objects.equals(i.getCmtId(), e.getCmtId())))
                .map(e -> {
                    return ItemCommentDetail
                            .builder()
                            .cmtId(e.getCmtId())
                            .buyerUsername(e.getBuyerUsername())
                            .comment(e.getComment())
                            .itemId(e.getItemId().toString())
                            .shopId(shopId)
                            .login(login)
                            .ordersn(e.getOrdersn())
                            .ratingStar(e.getRatingStar())
                            .cmtReply(CommonUtils.isBlank(e.getCmtReply()) ? null : JSONObject.toJSONString(e.getCmtReply()))
                            .createTime(Instant.ofEpochSecond(e.getCreateTime()))
                            .build(); })
                .collect(Collectors.toList());
        List<ItemCommentDetail> itemCommentDetails1 = itemCommentDetailRepository.saveAll(itemCommentDetailList);
        log.info("保存商品评论数据,login:{},shopId:{},data:{} ", login, shopId, itemCommentDetails1);
        //获取未回复的评论,发送mq检查是否自动评价
        List<ItemCommentDetail> list = itemCmtList
                .stream()
                .filter(e -> CommonUtils.isBlank(e.getCmtReply()))
                .map(e -> {
                    return ItemCommentDetail
                            .builder()
                            .cmtId(e.getCmtId())
                            .buyerUsername(e.getBuyerUsername())
                            .itemId(e.getItemId().toString())
                            .ordersn(e.getOrdersn())
                            .ratingStar(e.getRatingStar())
                            .createTime(Instant.ofEpochSecond(e.getCreateTime()))
                            .build(); })
                .collect(Collectors.toList());
        if (CommonUtils.isBlank(list)) {
            return true;
        }
        AutoCommentDTO build = AutoCommentDTO
                .builder()
                .login(login)
                .shopId(shopId)
                .cmtIdList(list)
                .build();
        log.info("获取到未回复的商品评价,login:{},key:{},data:{}", login, KEY_AUTO_COMMENT + login, build);
        mqProducerService.sendMQ(TAG_CHECK_AUTO_COMMENT, KEY_AUTO_COMMENT + login, build);
        return true;
    }

    /**
     * 1、循环获取商品评论
     * 2、发送mq消费
     *
     * @param itemInfo
     */
    private void handleSyncComment(CommentSyncQO.ItemInfo itemInfo, String login) {
        final Long shopId = itemInfo.getShopId();
        final Long itemId = itemInfo.getItemId();
        GetCommentParam param = GetCommentParam
                .builder()
                .shopId(shopId)
                .itemId(itemId)
                .page(0)
                .build();
        while (true) {
            ShopeeResult<GetCommentResult> result = itemApi.getComments(param);
            if (!result.isResult()) {
                log.error("获取商品评论失败,shopId:{},itemId:{},param:{}", shopId, itemId, param);
                break;
            }
            GetCommentResult commentResult = result.getData();
            if (CommonUtils.isBlank(commentResult.getItemCmtList())) {
                break;
            }
            //发送处理评论消息mq
            String requestId = commentResult.getRequestId();
            CommentDetailDTO dto = CommentDetailDTO
                    .builder()
                    .login(login)
                    .shopId(shopId.toString())
                    .itemCmtList(commentResult.getItemCmtList())
                    .build();
            log.info("处理商品评论数据,login:{},key:{}", login, requestId);
            mqProducerService.sendMQ(CommentDetailConsumer.TAG, requestId, dto);
            //判断是否继续循环
            if (!commentResult.getMore()) {
                break;
            }
        }
    }

    @Override
    public List<ItemCommentDetail> getShopProductComment(String login, List<String> ordersn) {
        List<ItemCommentDetail> itemCommentDetails = mongoTemplate.find(new Query().addCriteria(Criteria.where("ordersn")
                .in(ordersn).and("login").is(login)), ItemCommentDetail.class);
        return itemCommentDetails;
    }

    @Override
    public Boolean syncUpdateComment(List<CommentCallbackDTO> callbackDTOS) {
        callbackDTOS.parallelStream().forEach(e -> {
            Query query = new Query().addCriteria(Criteria.where("cmtId").in(e.getCmtIds())
                    .and("login").is(e.getLogin()));
            GetCommentResult.ItemCmtListBean.CmtReplyBean cmtReplyBean = new GetCommentResult.ItemCmtListBean.CmtReplyBean();
            cmtReplyBean.setReply(e.getCommentContent());
            cmtReplyBean.setIsHidden(false);
            Update update = new Update().set("cmtReply", JSON.toJSONString(cmtReplyBean));
            mongoTemplate.updateMulti(query, update, ItemCommentDetail.class);
        });
        return true;
    }


}
