package com.izhiliu.erp.web.rest.item;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codahale.metrics.annotation.Timed;
import com.izhiliu.core.common.ValidList;
import com.izhiliu.core.config.internation.InternationUtils;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.common.TaskExecutorUtils;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.BatchBoostItemService;
import com.izhiliu.erp.service.item.ShopeeProductMediaService;
import com.izhiliu.erp.service.item.ShopeeProductMoveService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.business.ShopeeProductMoveServiceImpl;
import com.izhiliu.erp.service.item.cache.PublishVO;
import com.izhiliu.erp.service.item.dto.*;
import com.izhiliu.erp.util.ValidatorUtils;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.param.*;
import com.izhiliu.erp.web.rest.item.result.ShopeeProductMoveResult;
import com.izhiliu.erp.web.rest.item.vm.*;
import com.izhiliu.erp.web.rest.util.HeaderUtil;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import com.izhiliu.erp.web.rest.util.ShopeeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * REST controller for managing ShopeeProduct.
 */
@Validated
@RestController
@RequestMapping("/api")
public class ShopeeProductResource {

    private final Logger log = LoggerFactory.getLogger(ShopeeProductResource.class);

    public static final String ENTITY_NAME = "shopeeProduct";

    private final ShopeeProductService shopeeProductService;

    @Resource
    ShopeeProductMoveService shopeeProductMoveService;
    @Resource
    BatchBoostItemService batchBoostItemService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private TaskExecutorUtils taskExecutorUtils;

    @Resource
    private MongoTemplate mongoTemplate;

    public ShopeeProductResource(ShopeeProductService shopeeProductService) {
        this.shopeeProductService = shopeeProductService;
    }

    @Resource
    private ShopeeProductMediaService shopeeProductMediaService;

    private Executor newFixedThreadPool = Executors.newFixedThreadPool(10);


    /**
     * POST  /shopee-products : Create a new shopeeProduct.
     *
     * @param shopeeProductDTO the shopeeProductDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new shopeeProductDTO, or with status 400 (Bad Request) if the shopeeProduct has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/shopee-products")
    @Timed
    public ResponseEntity<ShopeeProductDTO> createShopeeProduct(@RequestBody ShopeeProductDTO shopeeProductDTO) throws URISyntaxException {
        log.debug("REST request to save ShopeeProduct : {}", shopeeProductDTO);
        if (shopeeProductDTO.getId() != null) {
            throw new BadRequestAlertException("A new shopeeProduct cannot already have an ID", ENTITY_NAME, "bad.request.alert.exception.create.shopee.product.idexists");
        }
        ShopeeProductDTO result = shopeeProductService.save(shopeeProductDTO);
        return ResponseEntity.created(new URI("/api/shopee-products/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
                .body(result);
    }

    /**
     * POST  /shopee-products-source : Create a new shopeeSourceProduct.
     *
     * @param shopeeProductDTO the shopeeProductDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new shopeeProductDTO, or with status 400 (Bad Request) if the shopeeProduct has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/shopee-products-source")
    @Timed
    @Deprecated
    public ResponseEntity<?> createShopeeSourceProduct(@RequestBody @Validated(value = {Default.class}) ShopeeProductInsertDTO shopeeProductDTO, BindingResult result) throws URISyntaxException {
        try {
            log.debug("REST request to save ShopeeProduct : {}", shopeeProductDTO);
            if (ShopeeProduct.VariationTier.ONE.val.equals(shopeeProductDTO.getParam().getVariationTier())) {
                ValidatorUtils.validateBySpring(shopeeProductDTO, Default.class, ShopeeProductInsertDTO.OneInsert.class, ShopeeProductInsertDTO.OneNewInsert.class);
            } else {
                ValidatorUtils.validateBySpring(shopeeProductDTO, Default.class, ShopeeProductInsertDTO.Insert.class, ShopeeProductInsertDTO.NewInsert.class);
            }
            ShopeeProductDTO saveSource = shopeeProductService.saveSource(shopeeProductDTO);
            return ResponseEntity.created(new URI("/api/shopee-products/" + saveSource.getId()))
                    .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, saveSource.getId().toString()))
                    .body(saveSource);
        } catch (BindException e) {
//            e.printStackTrace();
            return ResponseEntity.status(501).body(e.getMessage());
        }

    }

    /**
     * POST  /shopee-products-source : Create a new shopeeSourceProduct.
     *
     * @param shopeeProductDTO the shopeeProductDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new shopeeProductDTO, or with status 400 (Bad Request) if the shopeeProduct has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/v2/shopee-products-source")
    @Timed
    public ResponseEntity<?> createShopeeSourceProductV2(@RequestBody @Validated(value = {Default.class}) ShopeeProductInsertDTO shopeeProductDTO, BindingResult result) throws URISyntaxException {
        try {
            log.debug("REST request to save ShopeeProduct : {}", shopeeProductDTO);
            shopeeProductDTO.getParam().setVersion(true);
            ValidatorUtils.validateBySpring(shopeeProductDTO, Default.class, ShopeeProductInsertDTO.Insert.class, ShopeeProductInsertDTO.NewInsert.class);
            ShopeeProductDTO saveSource = shopeeProductService.saveSource(shopeeProductDTO);
            return ResponseEntity.created(new URI("/api/shopee-products/" + saveSource.getId()))
                    .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, saveSource.getId().toString()))
                    .body(saveSource);
        } catch (BindException e) {
//            e.printStackTrace();
            return ResponseEntity.status(501).body(e.getMessage());
        }

    }


    @GetMapping("/shopee-products-source-id")
    @Timed
    public ResponseEntity getMateDataShopeeSourceProductID() throws URISyntaxException {
        final String param = shopeeProductService.generate().toString();
        return ResponseEntity.ok().body(new HashMap<String, Object>() {{
            put("id", param);
        }});
    }


    /**
     * PUT  /shopee-products : Updates an existing shopeeProduct.
     *
     * @param shopeeProductDTO the shopeeProductDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated shopeeProductDTO,
     * or with status 400 (Bad Request) if the shopeeProductDTO is not valid,
     * or with status 500 (Internal Server Error) if the shopeeProductDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/shopee-products")
    @Timed
    public ResponseEntity<Object> updateShopeeProduct(@RequestBody ShopeeProductDTO shopeeProductDTO) throws URISyntaxException {
        log.debug("REST request to update ShopeeProduct : {}", shopeeProductDTO);
        if (shopeeProductDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "bad.request.alert.exception.idnull");
        }
        if (Objects.isNull(shopeeProductDTO.getDiscountActivity())) {
            shopeeProductDTO.setDiscountActivity(Boolean.FALSE);
        }
        shopeeProductDTO.setCurrency(null);
        return shopeeProductService.checkUpdate(shopeeProductDTO);
    }

    @PutMapping("/v2/shopee-products")
    @Timed
    public ResponseEntity<Object> updateOrSaveShopeeProduct(@RequestBody ShopeeProductDTO shopeeProductDTO) throws URISyntaxException {
        log.debug("REST request to update ShopeeProduct : {}", shopeeProductDTO);
        if (shopeeProductDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "bad.request.alert.exception.idnull");
        }
        if (Objects.isNull(shopeeProductDTO.getDiscountActivity())) {
            shopeeProductDTO.setDiscountActivity(Boolean.FALSE);
        }
        shopeeProductDTO.setCurrency(null);
        return shopeeProductService.checkUpdateOrSave(shopeeProductDTO);
    }

    /**
     * GET  /shopee-products : get all the shopeeProducts.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of shopeeProducts in body
     */
    @GetMapping("/shopee-products")
    @Timed
    public ResponseEntity<List<ShopeeProductDTO>> getAllShopeeProducts(Pageable pageable) {
        log.debug("REST request to get a page of ShopeeProducts");
        IPage<ShopeeProductDTO> page = shopeeProductService.page(PaginationUtil.toIPage(pageable));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shopee-products");
        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
    }


    @GetMapping(path = {"/service/products/shopee-products/batch/detection"})
    @Timed
    public ResponseEntity<List<ShopeeProduct>> shopeeProducts(@RequestParam List<Long> productIds, @RequestParam("loginId") @NotNull String loginId, @RequestParam("status") @NotNull Integer status) {
        if (log.isDebugEnabled()) {
            log.debug("REST request to get ShopeeProduct : {}", productIds);
        }
        List<ShopeeProduct> shopeeProductDTO = shopeeProductService.findList(productIds, loginId, status, null);
        return ResponseEntity.ok(shopeeProductDTO);
    }

    @PostMapping("/service/products/delete/batch/publish")
    @Timed
    public ResponseEntity<Boolean> deleteBatchPublish(@RequestBody @Nonnull List<Long> productIds) {
        shopeeProductService.deleteBatchPublish(productIds);
        return ResponseEntity.ok(true);
    }

    /**
     * GET  /shopee-products/:id : get the "id" shopeeProduct.
     *
     * @param id the id of the shopeeProductDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the shopeeProductDTO, or with status 404 (Not Found)
     */
    @GetMapping(path = {"/shopee-products/{id}", "/service/shopee-products/{id}"})
    @Timed
    public ResponseEntity<ShopeeProductDTO> getShopeeProduct(@PathVariable Long id) {
        log.debug("REST request to get ShopeeProduct : {}", id);
        Optional<ShopeeProductDTO> shopeeProductDTO = shopeeProductService.findThrow(id);
        return ResponseEntity.ok(shopeeProductDTO.orElse(null));
    }


    @PostMapping(path = {"/service/products/batch-publish", "/products/batch-publish"})
    @Timed
    public ResponseEntity<Boolean> batchPublish(@RequestBody @Validated PublishVO publishVO) {
        shopeeProductService.batchPublish(publishVO.getPublishDtos(), publishVO.getStartDateTime(), publishVO.getEndDateTime());
        return ResponseEntity.ok(true);
    }


    @GetMapping(path = {"/v2/products/shopee-products/{id}"})
    @Timed
    public ResponseEntity<ShopeeProductDTO> getShopeeProduct2(@PathVariable Long id) {
        log.debug("REST request to get ShopeeProduct : {}", id);
        Optional<ShopeeProductDTO> shopeeProductDTO = shopeeProductService.findThrow(id);
        return ResponseEntity.ok(shopeeProductDTO.orElse(null));
    }

    /**
     * DELETE  /shopee-products/:id : delete the "id" shopeeProduct.
     *
     * @param id the id of the shopeeProductDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/shopee-products")
    @Timed
    public ResponseEntity<Boolean> deleteShopeeProduct(@RequestBody @NotEmpty List<Long> id) {
        log.debug("REST request to delete ShopeeProduct : {}", id);
        shopeeProductService.delete(id);
        return ResponseEntity.ok(true);
    }


    /**
     * 复制到站点
     */
    @PostMapping("/platform-node-products/saveToPlatformNode")
    @Timed
    public ResponseEntity<String> copyToPlatformNode(@RequestBody @Validated SaveToPlatformNodeParam param) {
        final Long id = shopeeProductService.copyToPlatformNode(param.getProductId(), param.getPlatformNodeId());
        return ResponseEntity.ok("\"" + id + "\"");
    }

    /**
     * 站点间跳转不再copy数据，而是判断
     * 存在数据，直接返回
     * 不存在则直接直接生成ID返回
     */
    @PostMapping("/v2/platform-node-products/saveToPlatformNode")
    @Timed
    public ResponseEntity<Map> copyToPlatformNodeTwo(@RequestBody @Validated SaveToPlatformNodeParam param) {
        final Map resultMap = shopeeProductService.copyToPlatformNodeTwo(param.getProductId(), param.getPlatformNodeId());
        return ResponseEntity.ok(resultMap);
    }


    /**
     * 复制到店铺
     * <p>
     * shopId And shopId
     */
    @PostMapping("/shop-products/saveOrPublishToShop")
    @Timed
    public ResponseEntity<Boolean> saveOrPublishToShop(@RequestBody @Validated SaveOrPublishToShopParam param) {
        param.setAsync(true);
        if (!param.getType()) {
            shopeeProductService.saveToShop(param);
        } else {
            shopeeProductService.publishToShop(param);
        }
        return ResponseEntity.ok(true);
    }

    /**
     * 中间商品优化
     * <p>
     * shopId And shopId
     */
    @PostMapping("/v2/shop-products/saveOrPublishToShop")
    @Timed
    public ResponseEntity<Boolean> saveOrPublishToShopTwo(@RequestBody @Validated SaveOrPublishToShopParam param) {
        param.setAsync(true);
        if (!param.getType()) {
            shopeeProductService.saveToShop(param);
        } else {
            shopeeProductService.publishToShopTwo(param);
        }
        return ResponseEntity.ok(true);
    }

    /**
     * 发布到店铺
     * <p>
     * productId And shopId
     */
    @PostMapping("/shop-products/publish")
    @Timed
//    @RateLimit(limitNum = 5,parameterName = "params")
    public ResponseEntity<Boolean> publishToShop(@RequestBody @Validated(ShopProductParam.ProductAndShop.class) ValidList<ShopProductParam> params) {
        shopeeProductService.publishToShop(params);
        return ResponseEntity.ok(true);
    }

    /**
     * 拉到本地
     * <p>
     * itemId And shopIdxzxz
     */
    @PostMapping("/shop-products/pull")
    @Timed
    public ResponseEntity<Boolean> pullToLocal(@RequestBody @Validated(ShopProductParam.ItemAndShop.class) ValidList<ShopProductParam> params) {
        shopeeProductService.pullToLocal(params);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/shop-products/pullAll")
    @Timed
    public ResponseEntity<Boolean> pullToLocal(@RequestBody @NotEmpty List<Long> shopIds) {
        shopeeProductService.pullToLocalAll(shopIds);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/v2/shop-products/pull")
    @Timed
    @Deprecated
    public ShopSycnResult pullAllTwo(@RequestBody ShopeeSyncDTO shopeeSyncDTO) {

        return shopeeProductService.pullToLocalAllTwo(shopeeSyncDTO.getShopIds(), shopeeSyncDTO.getItmeIds(), shopeeSyncDTO.getKey());
    }


    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {POST} /shop-products/sync/shop 同步店铺商品
     * @apiDescription 同步店铺商品V2接口，结果通过/shop-products/sync/item 获取
     * @apiParam {Object[]} shopIdS 同步店铺id
     * @apiParam {Boolean} needDeletedItem 是否包括删除的商品(默认false)
     * @apiParamExample {json} 示例：
     * {
     * "shopIds":[
     * 198198406,
     * 141878703]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 202
     * {
     * "taskId": "cGFyZW50QGl6aGlsaXUuY29tMTJBNTJF",
     * "syncShop": 2
     * }
     * @apiSuccess {String} taskId 任务id
     * @apiSuccess {Number} syncShop: 同步的店铺数量
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/shop-products/sync/shop")
    @Timed
    public ResponseEntity<TaskExecuteVO> syncShopItem(@RequestBody ItemSyncQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        qo.setTaskId(CommonUtils.getTaskId(qo.getLogin()));
        return ResponseEntity.ok(shopeeProductService.syncByShop(qo));
    }

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {POST} /shop-products/sync/item 同步商品（废弃）
     * @apiDescription 单个/批量同步商品，结果通过/common/processing/v3 获取
     * @apiParam {Object[]} syncItems
     * @apiParam {Number} Object.itemId shopee商品id
     * @apiParam {Number} Object.shopId 店铺id
     * @apiParamExample {json} 示例：
     * {
     * "syncItems":[
     * {
     * "itemId":2153183770,
     * "shopId":141878703
     * },
     * {
     * "itemId":4554862803,
     * "shopId":198198406
     * }]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 202
     * {
     * "taskId": "cGFyZW50QGl6aGlsaXUuY29tNjgzQkUy",
     * "total": 2
     * }
     * @apiSuccess {String} taskId 任务id
     * @apiSuccess {Number} total: 同步的总数量
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/shop-products/sync/item")
    @Timed
    @Deprecated
    public ResponseEntity<TaskExecuteVO> syncByItem(@RequestBody ItemSyncQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        qo.setTaskId(CommonUtils.getTaskId(qo.getLogin()));
        return ResponseEntity.ok(shopeeProductService.syncBatch(qo));
    }

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {POST} /shop-products/sync/processing/{taskId} 获取同步店铺商品结果
     * @apiDescription 获取同步店铺商品结果
     * @apiSuccessExample response
     * HTTP/1.1 202
     * {
     *     "code": 0,
     *     "syncShop": 1,
     *     "total": 120,
     *     "handleTotal": 120,
     *     "success": 120,
     *     "fail": 0,
     *     "shopSyncList": [
     *         {
     *             "shopId": "141878703",
     *             "shopName":"xxxx",
     *             "total": 120,
     *             "handleTotal": 120,
     *             "success": 120
     *         }
     *     ]
     * }
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @GetMapping("/shop-products/sync/processing/{taskId}")
    public ResponseEntity<ProductSyncVO> getSyncProcess(@PathVariable("taskId") String taskId) {
        final String login = SecurityUtils.currentLogin();
        return ResponseEntity.ok().body(shopeeProductService.getSyncProcess(login, taskId));
    }


    @PostMapping("/shop-products/remove")
    @Timed
    public void remove(String key) {
        shopeeProductService.remove(key);
    }


    /**
     * 推送到店铺PlatformNodeProductMapper
     * <p>
     * productId And shopId
     */
    @PostMapping("/shop-products/push")
    @Timed
    public ResponseEntity<Boolean> pushToShop(@RequestBody @Validated(ShopProductParam.ProductAndShop.class) ValidList<ShopProductParam> params) {
        shopeeProductService.pushToShop(params);
        return ResponseEntity.ok(true);
    }

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {POST} /shop-products/push/v2 更新在线商品
     * @apiDescription 局部更新在线商品
     * @apiParam {Object[]} pushType 推送类型: item_image：商品图片; item_info：基本信息; item_price：价格; item_stock：库存; item_logistics：物流信息; item_all： 全部更新;
     * @apiParam {Object[]} params 参数
     * @apiParam {String} Object.productId 商品id
     * @apiParam {String} Object.shopId 店铺
     * @apiParamExample {json} 示例：
     * {
     * "pushType":[
     * "item_price",
     * "item_image",
     * "item_stock",
     * "item_logistics"],
     * "params":[
     * {
     * "productId":"1184277931137011713",
     * "shopId":128338848
     * }]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 202
     * {
     * "code": 0,
     * "taskId": "YWRtaW5AaXpoaWxpdS5jb21BNEE2RTk=",
     * "taskType": "item_update"
     * }
     * @apiSuccess {Number} code 任务执行状态  0: 处理中； 1：成功；
     * @apiSuccess {String} taskId 任务id
     * @apiSuccess {String} taskType: item_update:商品更新
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/shop-products/push/v2")
    @Timed
    public ResponseEntity<TaskExecuteVO> pushToShopV2(@RequestBody @Validated PushToShopeeTaskQO qo) {
        final String login = SecurityUtils.currentLogin();
        final String taskId = CommonUtils.getTaskId(login);
        TaskExecuteVO vo = TaskExecuteVO.builder()
                .code(TaskExecuteVO.CODE_PENDING)
                .taskId(taskId)
                .taskType(TaskExecuteVO.TASK_TYPE_ITEM_UPDATE)
                .build();
        stringRedisTemplate.opsForValue().set(taskId, JSON.toJSONString(vo), 10, TimeUnit.MINUTES);
        qo.setLogin(login);
        qo.setTaskId(taskId);
        qo.setLocale(InternationUtils.getLocale());
        shopeeProductService.pushToShopCommon(qo);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(vo);
    }

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {POST} /shop-products/image/local-to-shopee shopee图片换链
     * @apiDescription shopee图片换链
     * @apiParam {Object[]} params 参数
     * @apiParam {String} Object.productId 商品id
     * @apiParam {String} Object.shopId 店铺
     * @apiParamExample {json} 示例：
     * {
     * "params":[
     * {
     * "productId":"1184277931137011713",
     * "shopId":128338848
     * }]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 202
     * {
     * "code": 0,
     * "taskId": "YWRtaW5AaXpoaWxpdS5jb20zNTg4RTc=",
     * "taskType": "item_image_to_shopee"
     * }
     * @apiSuccess {Number} code 任务执行状态  0: 处理中； 1：成功；
     * @apiSuccess {String} taskId 任务id
     * @apiSuccess {String} taskType: item_update:商品更新
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/shop-products/image/local-to-shopee")
    @Timed
    public ResponseEntity<TaskExecuteVO> localImageToShopee(@RequestBody PushToShopeeTaskQO qo) {
        final String login = SecurityUtils.currentLogin();
        final String taskId = CommonUtils.getTaskId(login);
        TaskExecuteVO vo = TaskExecuteVO.builder()
                .code(TaskExecuteVO.CODE_PENDING)
                .taskId(taskId)
                .taskType(TaskExecuteVO.TASK_TYPE_ITEM_IMAGE_TO_SHOPEE)
                .build();
        stringRedisTemplate.opsForValue().set(taskId, JSON.toJSONString(vo), 10, TimeUnit.MINUTES);
        qo.setLogin(login);
        qo.setTaskId(taskId);
        qo.setLocale(InternationUtils.getLocale());
        shopeeProductService.pushLocalImageToShopee(qo);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(vo);
    }

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {GET} /common/task/{taskId} 任务进度
     * @apiDescription 任务详情，支持通用
     * @apiSuccessExample response
     * HTTP/1.1 200
     * {
     * "code": 1,
     * "data": {
     * "result": true,
     * "sumPayment": 0,
     * "discountFee": 0,
     * "sumCarriage": 0,
     * "orderIds": [
     * ],
     * "sumPaymentNoCarriage": 0
     * },
     * "taskId": "Y3V0dGVyenlAcXEuY29tQzExOTcx",
     * "taskType": "create",
     * "taskDetailList": [
     * {
     * "taskId": "Y3V0dGVyenlAcXEuY29tQzExOTcx",
     * "detailId": "Y3V0dGVyenlAcXEuY29tQzExOTcx3952561734",
     * "detailName": "task.create.alibaba.order",
     * "code": 1
     * },
     * {
     * "taskId": "Y3V0dGVyenlAcXEuY29tQzExOTcx",
     * "detailId": "Y3V0dGVyenlAcXEuY29tQzExOTcx3952561745",
     * "detailName": "task.create.alibaba.order",
     * "code": 1
     * },
     * {
     * "taskId":"YWRtaW5AaXpoaWxpdS5jb20zNTg4RTc=",
     * "detailId":"1184277931137011713",
     * "detailName":"商品1184277931137011713上传图片到shopee",
     * "code":1,
     * "success":1,
     * "total":2,
     * "fail":1,
     * "detailData":[{
     * "shopeeImageUrl":"",
     * "errorDesc":"download fail.",
     * "imageUrl":"cbu01.alicdn.com/img/ibank/2019/495/335/10955533594_1034434799.jpg",
     * "error":"error_param"
     * }]
     * }
     * ]
     * }
     * @apiSuccess {Number} code 任务执行状态  0: 处理中； 1：成功；
     * @apiSuccess {String} taskId 任务id
     * @apiSuccess {String} taskType 任务类型: preview: 下单预览 ; create:采购单下单;  item_update：商品更新
     * @apiSuccess {String} errorMessage 错误信息
     * @apiSuccess {Object[]} taskDetailList 子任务详情
     * @apiSuccess {Number} taskDetailList.total 总数
     * @apiSuccess {Number} taskDetailList.success 成功数量
     * @apiSuccess {Number} taskDetailList.fail 失败数量
     * @apiSuccess {Object[]} taskDetailList.detailData 子任务数据
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @GetMapping("/common/task/{taskId}")
    public ResponseEntity<TaskExecuteVO> getTaskProgress(@PathVariable("taskId") String taskId) {
        final String login = SecurityUtils.currentLogin();
        TaskExecuteVO vo = new TaskExecuteVO();
        String s = CommonUtils.decodeTaskId(taskId, login);
        if (CommonUtils.isBlank(s)) {
            throw new LuxServerErrorException("任务不存在");
        }
        String s1 = stringRedisTemplate.opsForValue().get(taskId);
        if (CommonUtils.isNotBlank(s1)) {
            vo = JSON.parseObject(s1, TaskExecuteVO.class);
        }
        return ResponseEntity.ok(vo);
    }

    /**
     * 上下架商品
     * <p>
     * productId And itemID And shopId
     */
    @PostMapping("/shop-products/unlist")
    @Timed
    public ResponseEntity<Boolean> shopUnList(@RequestBody @Valid ShopUnListParam param) {
        shopeeProductService.shopUnlist(param);
        return ResponseEntity.ok(true);
    }

    @GetMapping(path = {"/shop-products/findByItemId/{itemId}", "/service/shop-products/find-by-itemId/{itemId}"})
    @Timed
    public ResponseEntity<ShopeeProductDTO> findByItemId(@PathVariable("itemId") @NotNull Long itemId) {
        return ResponseEntity.ok(shopeeProductService.selectByItemId(itemId).get());
    }

    /**
     * 通过itemid查询产品
     * @param itemId
     * @return
     */
    @GetMapping("/shop-products/by-item/{itemId}")
    @Timed
    public ResponseEntity<ShopeeProductDTO> findOneByItemId(@PathVariable("itemId") @NotNull Long itemId) {
        Optional<ShopeeProductDTO> optional = shopeeProductService.selectByItemId(itemId);
        if (optional.isPresent()){
            ShopeeProductDTO shopeeProductDTO = optional.get();
            //设置商品媒体资源
            ShopeeProductMediaDTO shopeeProductMediaDTO = shopeeProductMediaService.selectByProductIdNotCache(shopeeProductDTO.getId());
            if (null != shopeeProductMediaDTO) {
                shopeeProductDTO.setImages(shopeeProductMediaDTO.getImages());
                shopeeProductDTO.setSizeChart(shopeeProductMediaDTO.getSizeChart());
                shopeeProductDTO.setPriceRange(shopeeProductMediaDTO.getPriceRange());
                shopeeProductDTO.setDiscountActivityId(Objects.equals(shopeeProductMediaDTO.getDiscountActivityId(), 0L) ? null : shopeeProductMediaDTO.getDiscountActivityId());
                shopeeProductDTO.setDiscountActivityName(shopeeProductMediaDTO.getDiscountActivityName());
            }
            return ResponseEntity.ok(shopeeProductDTO);
        }else {
            return ResponseEntity.ok(new ShopeeProductDTO());
        }
    }

    @PostMapping(path = {"/shop-products/scItem", "/service/shop-products/scItem"})
    @Timed
    public ResponseEntity<List<ScItemDTO>> selectScItem(@RequestBody Map<Long, List<Long>> map) {
        return ResponseEntity.ok(shopeeProductService.selectShopeeProductAllInfo(map));
    }

    //  ----------------------------------------------------------------- 批量保存

    /**
     * 批量拷贝平台商品到站点
     */
    @PostMapping("/shop-products/batch-edit/batchCopyToPlatformNode")
    @Timed
    public ResponseEntity<List<BatchEditProductVM>> batchCopyToPlatformNode(@RequestBody @Validated BatchCopyToPlatformNodeParam param) {
        final List<BatchEditProductVM> products = shopeeProductService.batchCopyToPlatformNode(param);
        return ResponseEntity.ok(products);
    }

    /**
     * 批量获取商品
     */
    @GetMapping("/shop-products/batch-edit/batchGetProduct")
    @Timed
    public ResponseEntity<List<BatchEditProductVM>> batchGetProduct(@RequestParam("productIds") @NotEmpty List<Long> productIds) {
        return ResponseEntity.ok(shopeeProductService.batchGetProduct(productIds));
    }

    /**
     * 异步
     * 批量保存
     */
    @PostMapping("/shop-products/batch-edit/batchSave")
    @Timed
    public ResponseEntity<Boolean> batchSave(@RequestBody @Validated ValidList<BatchEditProductVM> products) {
        return ResponseEntity.ok(shopeeProductService.batchSave(products));
    }

    /**
     * 异步
     * 批量保存到店铺
     */
    @PostMapping("/shop-products/batch-edit/batchSaveToShop")
    @Timed
    public ResponseEntity<Boolean> batchSaveToShop(@RequestBody @Validated BatchEditToShopParam param) {
        return ResponseEntity.ok(shopeeProductService.batchSaveToShop(param));
    }

    @PostMapping("/shop-products/batch-edit/batchSavePriceAndStock")
    @Timed
    public ResponseEntity<Boolean> batchSavePriceAndStock(@RequestBody @Validated BatchSavePriceAndStockParam param) {
        return ResponseEntity.ok(shopeeProductService.batchSavePriceAndStock(param));
    }

    //  ----------------------------------------------------------------- API-V2

    @GetMapping("/v2/platform-products/getAllByCurrentUser")
    @Timed
    public ResponseEntity<List<ProductListVM_V2>> getAllByPlatformV2(@Validated(ProductSearchStrategyDTO.SearchPlatform.class) ProductSearchStrategyDTO productSearchStrategy) {
        productSearchStrategy.setType(ShopeeProduct.Type.PLATFORM.code);
        final IPage<ProductListVM_V2> page = shopeeProductService.searchProductByCurrentUserV2(
                SecurityUtils.getCurrentLogin(),
                productSearchStrategy,
                new Page(productSearchStrategy.getPage() + 1, productSearchStrategy.getSize()));

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/v2/platform-products/getAllByCurrentUser");
        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
    }

    @GetMapping("/v2/shop-products/getAllByCurrentUser")
    @Timed
    public ResponseEntity<List<ProductListVM_V21>> getAllByShopV2(@Validated(ProductSearchStrategyDTO.SearchShop.class) ProductSearchStrategyDTO productSearchStrategy) {
        final IPage<ProductListVM_V21> page = shopeeProductService.searchShopProductByCurrentUser(productSearchStrategy);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/v2/shop-products/getAllByCurrentUser");
        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
    }

    @GetMapping("/v21/shop-products/getAllByCurrentUser")
    @Timed
    public ResponseEntity<List<ProductListVM_V21>> getAllByShopV21(@Validated(ProductSearchStrategyDTO.SearchShop.class) ProductSearchStrategyDTO productSearchStrategy) {
        productSearchStrategy.setType(ShopeeProduct.Type.SHOP.code);
        IPage<ProductListVM_V21> page;
        if (productSearchStrategy.getBoost()) {
            page = batchBoostItemService.searchShopBoostProductByCurrentUser(productSearchStrategy);
        } else {
            page = shopeeProductService.searchShopProductByCurrentUser(productSearchStrategy);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/v2/shop-products/getAllByCurrentUser");
        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
    }

    @GetMapping("/v2/node-products/{productId}")
    @Timed
    public ResponseEntity<List<ProductListVM_V2>> getNodeChilds(@PathVariable("productId") @NotNull Long productId) {
        return ResponseEntity.ok(shopeeProductService.getNodeChilds(productId));
    }

    @GetMapping("/v2/shopee-products/getShops/{productId}")
    @Timed
    public ResponseEntity<List<ShopVM>> getShops(@PathVariable("productId") @NotNull Long productId) {
        return ResponseEntity.ok(shopeeProductService.getShops(productId));
    }

    @PostMapping(value = {
            "/service/products/image-by-skucode",
            "/products/image-by-skucode"
    })
    public ResponseEntity<List<ProductImageResult>> imageBySkuCode(@RequestBody List<ProductImageDto> productImageDtos) {
        return ResponseEntity.ok(shopeeProductMediaService.selectProductImageBySkuCode(productImageDtos));
    }

    @GetMapping("/service/item/select-by-item-id")
    public Optional<ShopeeProductDTO> selectByItemId(@RequestParam("itemId") long itemId) {
        return shopeeProductService.selectByItemId(itemId);
    }

    @PostMapping("/service/item/select-item-by-batch")
    public Optional<List<ShopeeProductDTO>> selectShopeeProductBatch(@RequestBody List<Long> itemIdList) {
        if (!CollectionUtils.isEmpty(itemIdList)) {
            final List<Long> collect = itemIdList.stream().filter(Objects::nonNull).collect(Collectors.toList());
            if (!collect.isEmpty()) {
                return shopeeProductService.selectShopeeProductBatch(collect);
            }
        }
        return Optional.of(Collections.emptyList());
    }


    @PostMapping("/v1/porduct/move-task")
    public ResponseEntity<Boolean> porductPutToProductMoveTask(@Validated(ShopeeProductMoveParam.Insert.class) @RequestBody ShopeeProductMoveParam shopeeProductMoveParam) {
        return ResponseEntity.ok(shopeeProductMoveService.porductPutToProductMoveTask(shopeeProductMoveParam));
    }


    @GetMapping("/v1/porduct/move-task")
    public ResponseEntity<ShopeeProductMoveResult> selectShopeeProductMoveTask() {
        return ResponseEntity.ok(shopeeProductMoveService.selectShopeeProductMoveTask());
    }


    @DeleteMapping("/v1/porduct/move-task")
    public ResponseEntity<Boolean> removeProductMoveTask(@Validated(ShopeeProductMoveParam.Delete.class) ShopeeProductMoveParam shopeeProductMoveParam) {
        return ResponseEntity.ok(shopeeProductMoveService.removeProductMoveTask(shopeeProductMoveParam));
    }

    @PutMapping("/v1/porduct/move-task")
    public ResponseEntity<Boolean> deleteProductMoveTask(@Validated(ShopeeProductMoveParam.Delete.class) @RequestBody ShopeeProductMoveParam shopeeProductMoveParam) {
        return ResponseEntity.ok(shopeeProductMoveService.deleteProductMoveTask(shopeeProductMoveParam));
    }


    @PatchMapping("/v1/porduct/move-task")
    public ResponseEntity<Boolean> patchProductMoveTask(@Validated(ShopeeProductMoveParam.Delete.class) @RequestBody ShopeeProductMoveParam shopeeProductMoveParam) {
        final String key = ShopeeProductMoveServiceImpl.PRODUCT_MOVE_TASK_PREFIX.concat(SecurityUtils.currentLogin());
        return ResponseEntity.ok(shopeeProductMoveService.syncShopeeProductMoveTask(key));
    }

    @PostMapping("/porduct/a")
    public ResponseEntity<List> productA(@RequestBody List<String> urls) {
        List<String> values = urls.parallelStream().map(ShopeeUtils::encode).collect(Collectors.toList());
        return ResponseEntity.ok(values);
    }


}
