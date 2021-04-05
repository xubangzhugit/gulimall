package com.izhiliu.erp.service.item.business;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.config.BaseVariable;
import com.izhiliu.core.common.constant.PlatformNodeEnum;
import com.izhiliu.core.config.security.SecurityInfo;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.config.aop.subscribe.SubLimitService;
import com.izhiliu.erp.config.module.currency.CurrencyRateApiImpl;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateResult;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.log.LogConstant;
import com.izhiliu.erp.log.LoggerOp;
import com.izhiliu.erp.service.item.ShopeeProductAttributeValueService;
import com.izhiliu.erp.service.item.ShopeeProductMoveService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.ShopeeSkuAttributeService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.service.item.impl.ShopeeProductSkuServiceImpl;
import com.izhiliu.erp.service.mq.producer.MQProducerService;
import com.izhiliu.erp.util.SnowflakeGenerate;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import com.izhiliu.erp.web.rest.item.param.ShopeeProductMoveParam;
import com.izhiliu.erp.web.rest.item.result.ShopeeProductMoveResult;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 店铺搬家具体实现
 *
 * @author Seriel
 * @create 2019-09-20 14:08
 **/
@Service
@Slf4j
public class ShopeeProductMoveServiceImpl implements ShopeeProductMoveService {

    public final static String PRODUCT_MOVE_TASK_PREFIX = "lux:product_move_task:";
    //    public final static String PRODUCT_MOVE_TASK_QUEUE_PREFIX = "lux:product_move_task:queue:";
    private final static String CURRENT_INDEX = "currentIndex";
    private final static String STATUS = "status";
    private final static String LOCAL_STATUS = "localStatus";
    public final static String STORE_MOVE = "storeMove";
//    private final static String isSeenData = "isSeenData";

    @Resource
    ShopeeProductService shopeeProductService;
    @Resource
    UaaService uaaService;
    @Resource
    SubLimitService subLimitService;
    @Resource
    private CurrencyRateApiImpl currency;
    @Resource
    SnowflakeGenerate snowflakeGenerate;

    StringRedisTemplate redisTemplate;


    @Resource
    MQProducerService mqProducerService;

    @Resource
    protected ShopeeProductAttributeValueService shopeeProductAttributeValueService;

    @Resource
    protected ShopeeProductSkuServiceImpl shopeeProductSkuService;
    @Resource
    protected ShopeeSkuAttributeService shopeeSkuAttributeService;

    private final HashOperations<String, String, String> redisStringHash;
//    private ZSetOperations<String, String> zSet;

    static LoggerOp getLoggerOpObejct() {
        return new LoggerOp().start().setKind(LogConstant.SHOPEE_PRODUCT).setType(LogConstant.SHOP_MOVE).setCode(LogConstant.PUT);
    }

    public ShopeeProductMoveServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        redisStringHash = this.redisTemplate.opsForHash();
    }

    /**
     *   限制次数是 父账号的  但是 查询店铺 是 用的子账号的
     * @param shopeeProductMoveParam
     * @return
     */
    @Override
    public Boolean porductPutToProductMoveTask(ShopeeProductMoveParam shopeeProductMoveParam) {
        final LoggerOp loggerOpObejct = getLoggerOpObejct();
        // 校检数据
        final SecurityInfo loginId1 = getLoginId();
        loggerOpObejct.setLoginId(loginId1.getCurrentLogin());
        final String parentLogin = loginId1.getParentLogin();
        final String key = PRODUCT_MOVE_TASK_PREFIX.concat(parentLogin);
        loggerOpObejct.setMessage(" key {}");
        log.info(loggerOpObejct.toString(),key);
        if (redisTemplate.hasKey(key)) {
            final String status = redisStringHash.get(key, STATUS);
            if(Objects.equals(Integer.valueOf(RUNNING).byteValue(), status)){
                if (log.isErrorEnabled()) {
                    loggerOpObejct.setMessage("店铺搬家 ：   已存在该任务  ！！！key {}").error();
                    log.error(loggerOpObejct.toString(),key);
                }
                return false;
            }
        };

        List<Long> productIds = shopeeProductMoveParam.getProductIds().stream().distinct().collect(Collectors.toList());
        final List<ShopeeProduct> shopeeProducts = shopeeProductService.findList(productIds, parentLogin, null,null );
        if (CollectionUtils.isEmpty(shopeeProducts)) {
            if (log.isErrorEnabled()) {
                loggerOpObejct.setMessage("店铺搬家 ：  本次傳入的  商品都是非法的 參數  ！！！productIds {}").error();
                log.error(loggerOpObejct.toString(),productIds);
            }
            return false;
        }
        final List<ShopeeShopDTO> body = uaaService.getShopeeShopInfoV2(loginId1.getCurrentLogin(), loginId1.isSubAccount()).getBody();
        final Map<Long, String> collectShopInfos = body.stream().collect(Collectors.toMap(ShopeeShopDTO::getShopId, ShopeeShopDTO::getShopName));
        final Set<Long> shopIds = collectShopInfos.keySet();
        final List<Long> currentSelectedShopIds = shopeeProductMoveParam.getToShopIds()
                .stream()
                .filter(shopId -> shopIds.contains(shopId))
                .collect(Collectors.toList());
        final Map<Long, String> countrys = body.stream().collect(Collectors.toMap(ShopeeShopDTO::getShopId, ShopeeShopDTO::getCountry));
        if (CollectionUtils.isEmpty(currentSelectedShopIds)) {
            if (log.isErrorEnabled()) {
                loggerOpObejct.setMessage(" 店铺搬家 ： 本次傳入的  店鋪都是非法的 參數  ！！！ shopIds {}").error();
                log.error(loggerOpObejct.toString(), shopeeProductMoveParam.getToShopIds());
            }
            return false;
        }
        subLimitService.handleLimit(parentLogin,"shopMoveCount",shopeeProducts.size());
        putRedisProductMoveTask(shopeeProducts, currentSelectedShopIds, parentLogin, collectShopInfos, countrys);
        log.info(loggerOpObejct.ok().toString(),key);
        return true;
    }

    private SecurityInfo getLoginId() {
        return SecurityUtils.genInfo();
//        return  "admin@izhiliu.com";
    }

    /**
     * @param shopeeProducts         对应的商品
     * @param currentSelectedShopIds 对应的 要投放到的店铺
     * @param loginId                用户信息
     */
    private void putRedisProductMoveTask(final List<ShopeeProduct> shopeeProducts,
                                         final List<Long> currentSelectedShopIds,
                                         final String loginId,
                                         final Map<Long, String> collectShopInfos,
                                         final Map<Long, String> countrys) {
        final Long objId = snowflakeGenerate.nextId();
        final String key = PRODUCT_MOVE_TASK_PREFIX.concat(loginId);

        final HashMap<String, String> shopMoveInfo = fillShopMoveInfo(shopeeProducts, currentSelectedShopIds, collectShopInfos, countrys, objId);
        redisStringHash.putAll(key, shopMoveInfo);
        redisTemplate.expire(key,5, TimeUnit.DAYS);
        redisStringHash.put(PRODUCT_MOVE_TASK_PREFIX.concat(objId.toString()), key, Boolean.FALSE.toString());
        //  用來做索引  根据task id 找到对应的 内容
        final Set<Long> productIds = shopeeProducts.stream().map(ShopeeProduct::getId).collect(Collectors.toSet());
        for (Long productId : productIds) {
            mqProducerService.sendMQ(BaseVariable.LuxInternalActionVariable.TAG_LUX_SHOP_MOVE_ACTION_VERSION, key, productId);
        }
    }


    private HashMap<String, String> fillShopMoveInfo(List<ShopeeProduct> shopeeProducts, List<Long> currentSelectedShopIds, Map<Long, String> collectShopInfos, Map<Long, String> countrys, Long objId) {
        final Set<Long> productIds = shopeeProducts.stream().map(ShopeeProduct::getId).collect(Collectors.toSet());
        final Set<String> shopNames = shopeeProducts.stream().map(shopeeProduct -> collectShopInfos.get(shopeeProduct.getShopId())).collect(Collectors.toSet());
        final Set<String> toShopNames = currentSelectedShopIds.stream().map(shopIds -> collectShopInfos.get(shopIds)).collect(Collectors.toSet());

        final HashMap<String, String> putValues = new HashMap<>(10);
        putValues.put("shopeeShopNames", JSONArray.toJSONString(shopNames));
        putValues.put("toShopeeShopNames", JSONArray.toJSONString(toShopNames));

        putValues.put("errorMessage", PRODUCT_MOVE_TASK_PREFIX.concat("set:"+objId));
        putValues.put("toShopeeCountrys", JSONArray.toJSONString(countrys));

        putValues.put("shopeeProductIds", JSONArray.toJSONString(productIds));
        putValues.put("currentSelectedShopIds", JSONArray.toJSONString(currentSelectedShopIds));

        putValues.put("collectShopInfos",JSONArray.toJSONString(collectShopInfos));

        putValues.put("currentTaskSize", String.valueOf(currentSelectedShopIds.size() * productIds.size()));
        putValues.put("taskId", String.valueOf(objId));
        putValues.put(CURRENT_INDEX, String.valueOf(CURRENT_INDEX_DEFAULT));
        putValues.put(STATUS, String.valueOf(RUNNING));
        //   内部状态   不用给前端展示   现在用来陈述  当前任务执行的情况
        putValues.put(LOCAL_STATUS, String.valueOf(NOT_RUNNING));
        return putValues;
    }

    @Override
    public ShopeeProductMoveResult selectShopeeProductMoveTask() {
        final String parentLogin = getLoginId().getParentLogin();
        final String key = PRODUCT_MOVE_TASK_PREFIX.concat(parentLogin);
        final Map<String, String> entries = redisStringHash.entries(key);
        if (CollectionUtils.isEmpty(entries)) {
            return ShopeeProductMoveResult.DEFAULT_OBJECT;
        }
        final byte status = MapUtils.getByteValue(entries, STATUS);
//        if (Objects.equals(Integer.valueOf(RUNTIME_END).byteValue(), status)||Objects.equals(Integer.valueOf(RUNTIME_ERROR).byteValue(), status)) {
//            //todo 任务执行完毕   删除 该次任务
//            log.info(" 店铺搬家 ： 任務執行完畢  正在清除相关的数据  【{}】", key);
//            redisTemplate.delete(key);
//        }
        final String finshedTime = MapUtils.getString(entries, "finshedTime");
        final String errorMessage = MapUtils.getString(entries, "errorMessage");
        final JSONObject collectShopInfos = JSONObject.parseObject(MapUtils.getString(entries, "collectShopInfos"));
        //   优化 scan
        final Set<String> members = redisTemplate.opsForSet().members(errorMessage);
        final List<String> errorMessages = members.stream().map(info  -> resolveInfo(info,collectShopInfos)).collect(Collectors.toList());
        final ShopeeProductMoveResult shopeeProductMoveResult = new ShopeeProductMoveResult()
                .setShopName(JSONArray.parseArray(MapUtils.getString(entries, "shopeeShopNames"), String.class))
                .setToShopNames(JSONArray.parseArray(MapUtils.getString(entries, "toShopeeShopNames"), String.class))
                .setTaskId(MapUtils.getLong(entries, "taskId"))
                .setCount(MapUtils.getIntValue(entries, "currentTaskSize"))
                .setErrorMessages(errorMessages)
                .setStatus(status)
                .setFinshedTime(Objects.nonNull(finshedTime) ? Instant.parse(finshedTime) : null)
                .setCurrentIndex(MapUtils.getInteger(entries, CURRENT_INDEX));
        return shopeeProductMoveResult;
    }

    public String resolveInfo(String info,JSONObject collectShopInfos) {
        if (StringUtils.isBlank(info)) {
            return null;
        }
        StringBuilder stringBuilder = new  StringBuilder();
        final String[] split = info.split(":::");
        final String productName = split[1];
        stringBuilder.append("【")
                .append(productName)
                .append("】 in 【");
        final Integer productShopId = Integer.parseInt(split[2]);
        stringBuilder.append(collectShopInfos.get(productShopId))
                .append("】 shop ");

        if (info.startsWith("shop")) {
            stringBuilder.append(" to  【");
            final Integer toShopId =Integer.parseInt(split[4]);
            stringBuilder.append(collectShopInfos.get(toShopId));
            stringBuilder.append("】");
        }
        final String errorMassage = split[3];
        stringBuilder.append("  appear:".concat(errorMassage));
        return stringBuilder.toString();
    }

    @Override
    public Boolean removeProductMoveTask(ShopeeProductMoveParam shopeeProductMoveParam) {
        final String loginId = getLoginId().getCurrentLogin();
        final String key = PRODUCT_MOVE_TASK_PREFIX.concat(loginId);
        final String taskId = shopeeProductMoveParam.getTaskId().iterator().next().toString();
        final String hsahKey = PRODUCT_MOVE_TASK_PREFIX.concat(taskId);
        redisStringHash.put(hsahKey, key, Boolean.TRUE.toString());
        redisTemplate.expire(hsahKey,5, TimeUnit.DAYS);
        mqProducerService.sendMQ(BaseVariable.LuxInternalActionVariable.TAG_LUX_REMOVE_SHOP_MOVE_ACTION_VERSION,key,taskId);
        return true;
    }

    /**
     * <p>
     */
    @Override
    public Boolean syncShopeeProductMoveTask(String key) {

        final List<Long> shopeeProductIds = JSONArray.parseArray(Objects.requireNonNull(redisStringHash.get(key, "shopeeProductIds"), "shopeeProductIds  is  null"), Long.class);
        for (Long shopeeProductId : shopeeProductIds) {
            syncShopeeProductMoveTask(key,shopeeProductId.toString());
        }
             return  true ;
    }
    /**
     * <p>
     */
    @Override
    public Boolean syncShopeeProductMoveTask(String key, String productId) {
        final long shopeeProductId = Long.parseLong(Objects.requireNonNull(productId));
        if (log.isInfoEnabled()) {
            log.info("  key ： 【{}】", key);
        }
        final long taskId = Long.parseLong(Objects.requireNonNull(redisStringHash.get(key, "taskId"), "taskId  is  null"));
        if (redisTemplate.hasKey(key)) {
            final String taskIdKey = PRODUCT_MOVE_TASK_PREFIX.concat(String.valueOf(taskId));
            final String isStop = redisStringHash.get(taskIdKey, key);
            if (Boolean.valueOf(Objects.requireNonNull(isStop))) {
                if (log.isInfoEnabled()) {
                    log.info("  task is stop  ： 【{}】", taskId);
                }
            } else {
                final Optional<ShopeeProductDTO> productExist = shopeeProductService.find(shopeeProductId);
                final ShopeeProductDTO  shopeeProduct = productExist.get();
                if (!productExist.isPresent()) {
                    log.error("[店铺搬家 ：商品不存在]: {}", shopeeProductId);
                }
                //    將内部狀態調整為 正在執行中
                redisStringHash.put(key, LOCAL_STATUS, String.valueOf(RUNNING));
                run( key, shopeeProduct, taskIdKey);
            }
        } else {
            zSetClose(key);
        }
        return true;
    }

    @Override
    public Boolean deleteProductMoveTask(ShopeeProductMoveParam shopeeProductMoveParam) {
        final Map<String, String> entries = redisStringHash.entries(PRODUCT_MOVE_TASK_PREFIX.concat(shopeeProductMoveParam.getTaskId().iterator().next().toString()));
        final String key = entries.keySet().iterator().next();
        //   假設任務沒有執行就 直接清除掉
        if(Objects.equals(redisStringHash.get(key,LOCAL_STATUS),NOT_RUNNING)) {
            final Boolean delete = redisTemplate.delete(key);
            return delete;
        }
        zSetClose(key);
        return true;
    }

    private void zSetClose(String key) {
        if (log.isInfoEnabled()) {
            log.info(PRODUCT_MOVE_TASK_PREFIX + " remove key ： 【{}】", key);
        }
    }


    public void run(final String key, final ShopeeProductDTO shopeeProduct, String taskIdKey) {
        if (log.isInfoEnabled()) {
            log.info(PRODUCT_MOVE_TASK_PREFIX + "  key ： 【{}】 taskIdKey :【{}】", key, taskIdKey);
        }
        final List<Long> currentSelectedShopIds = JSONArray.parseArray(Objects.requireNonNull(redisStringHash.get(key, "currentSelectedShopIds"), "currentSelectedShopIds is  null"), Long.class);
        //  todo 需要货币号 不需要国家号
        final Map<Integer, String> toShopeeCountrys = JSONObject.parseObject(Objects.requireNonNull(redisStringHash.get(key, "toShopeeCountrys"), "currentSelectedShopIds  is null"), HashMap.class);
        try {
            int index = doRun(
                    key,
                    shopeeProduct,
                    currentSelectedShopIds,
                    toShopeeCountrys,
                    () -> Boolean.valueOf(Objects.requireNonNull(redisStringHash.get(taskIdKey, key)))
            );
        } catch (Exception e) {
            log.error(e.getMessage() + " " + shopeeProduct, e);
            final String errorMessageKey = redisStringHash.get(key, "errorMessage");
            redisTemplate.opsForSet().add(errorMessageKey, StringUtils.join("product:::", shopeeProduct.getName(), ":::", shopeeProduct.getShopId(), ":::", e.getMessage()));
            //  2  代表着 错误了
            redisTemplate.expire(errorMessageKey,3,TimeUnit.DAYS);
            redisStringHash.put(key, STATUS, String.valueOf(RUNTIME_ERROR));
        }
    }

    /**
     * @param product
     * @param currentSelectedShopIds
     * @param toShopeeCountrys
     * @return 是否终止   index < 0
     */
    private int doRun(String key,
                      ShopeeProductDTO product,
                      List<Long> currentSelectedShopIds,
                      final Map<Integer, String> toShopeeCountrys,
                      Supplier<Boolean> booleanSupplier) {

        final Long productId = product.getId();
        for (Long currentSelectedShopId : currentSelectedShopIds) {
            try {
                Boolean isStop = booleanSupplier.get();
                //  如果是 同店铺
                if (Objects.equals(product.getShopId(), currentSelectedShopId)) {
                    close(key, isStop);
                    continue;
                }
//                if(true){
//                    throw  new RuntimeException(" 出现了一个 意料之中的意外");
//                }
                if (isStop) {
                    close(key, isStop);
                    return -1;
                } else {
                    final String countrys = toShopeeCountrys.get(currentSelectedShopId.intValue());
                    final ShopeeProduct shopProduct = shopeeProductService.selectShopProductByParentIdAndShopId(productId, currentSelectedShopId).orElse(null);
                    if (shopProduct == null) {
                        // product  里面数据 已变成 copy 对象
                        final ShopeeProductDTO copyProduct = new ShopeeProductDTO();
                        BeanUtils.copyProperties(product, copyProduct);
                        copyToShop(copyProduct, currentSelectedShopId, getCurrency(countrys));
                    }
                    close(key, isStop);
                }
            } catch (Exception e) {
                log.error(e.getMessage() + " " + product, e);
                final String errorMessageKey = redisStringHash.get(key, "errorMessage");
                redisTemplate.opsForSet().add(errorMessageKey, StringUtils.join("shop:::",product.getName() , ":::" + product.getShopId(),":::"," error".concat(e.getMessage()+""),":::"+currentSelectedShopId));
                redisTemplate.expire(errorMessageKey,3,TimeUnit.DAYS);
                //  2  代表着 错误了
                redisStringHash.put(key, STATUS, String.valueOf(RUNTIME_ERROR));
                close(key, false);
            }
        }

        return 1;
    }

    public String getCurrency(String code) {
        if (Objects.isNull(code)) {
            throw new RuntimeException("code  错误");
        }
        final PlatformNodeEnum[] values = PlatformNodeEnum.values();
        for (int i = 0; i < values.length; i++) {
            if (Objects.equals(code, values[i].code)) {
                return values[i].currency;
            }
        }
        throw new RuntimeException("code  没有找到合适的");
    }


    public Long copyToShop(ShopeeProductDTO product, long shopId, String currency) {
            final Long productId = product.getId();
            final boolean crossStation = !product.getCurrency().equals(currency);

            log.info("[店铺搬家 ：生成店铺商品]");
            init(product, shopId, currency);
            if(crossStation){
                product.setLogistics(null);
                product.setCategoryId(null);
            }
            final ShopeeProductDTO copyOfProduct;
            try {
                copyOfProduct = shopeeProductService.save(product);
            } catch (Exception e) {
                log.error("保存商品异常: {}", e);
                throw new IllegalOperationException("internal.service.exception.error.saving.product", true);
            }
            final Long copyOfProductId = copyOfProduct.getId();
            if (crossStation) {
                refreshProductPrice(copyOfProduct, currency);
                log.info("[店铺搬家 ：异常诊断] : productId: {}, copyOfProductId: {}, copyOfProduct: {}", productId, copyOfProduct, currency);
//                shopeeProductAttributeValueService.productResetShopeeCategory(productId, copyOfProductId, copyOfProduct.getCategoryId() == null ? 0L : copyOfProduct.getCategoryId(), toNode.getId());
            } else {
                shopeeProductAttributeValueService.copyShopeeProductAttributeValue(productId, copyOfProductId);
            }
            shopeeSkuAttributeService.copyShopeeSkuAttribute(productId, copyOfProductId);
            this.coverShopeeProductSku(productId, copyOfProductId, crossStation ? currency : null);
            return copyOfProductId;
    }


    private void init(ShopeeProductDTO product, long shopId, String currency) {
        product.setParentId(product.getId());
        product.setId(snowflakeGenerate.nextId());
        product.setStatus(LocalProductStatus.NO_PUBLISH);
        product.setShopeeItemId(null);
        product.setShopeeItemStatus(ShopeeItemStatus.UNKNOWN);
        product.setGmtCreate(Instant.now());
        product.setGmtModified(Instant.now());
        product.setCurrency(currency);
        product.setPlatformNodeId(ShopeeUtil.nodeId(currency));
        product.setShopId(shopId);
        product.setType(ShopeeProduct.Type.SHOP.code);
        product.setOnlineUrl(null);
        //设置商品来源
        product.setCollect(STORE_MOVE);
        /*
         * 线上同步下来的
         */
        if (product.getShopeeItemId() != null && product.getParentId() == null) {
            product.setShopeeItemId(0L);
        }
    }

    /**
     * 马来(MYR)
     * to 新加坡(SGD) 0.332941d
     * to 印尼(IDR) 3460.7844
     */
    protected void refreshProductPrice(ShopeeProductDTO product, String toCurrency) {
        log.info("[跨站拷贝-处理金额]");

        log.info("[原价格] {}:{}", product.getVPrice(), product.getCurrency());
        ///  如果是 越南  或者马来西亚 就 直接舍弃小数
        final boolean roundDown = PlatformNodeEnum.SHOPEE_ID.currency.equals(toCurrency) || PlatformNodeEnum.SHOPEE_VN.currency.equals(toCurrency);
        Float price = shopeeProductService.convertPrice(product.getCurrency(), toCurrency, product.getVPrice());
        if (roundDown) {
            price = price.intValue() - 0f;
        }
        if (Objects.nonNull(product.getOriginalPrice())) {
            Float originalPrice = shopeeProductService.convertPrice(product.getCurrency(), toCurrency, ShopeeUtil.outputPrice(product.getOriginalPrice(), product.getCurrency()));
            if (roundDown) {
                originalPrice = originalPrice.intValue() - 0f;
            }
            product.setOriginalPrice(ShopeeUtil.apiInputPrice(originalPrice, toCurrency));
        }

        log.info("[新价格] {}:{}", price, toCurrency);
        final ShopeeProductDTO shopeeProductDTO = new ShopeeProductDTO();
        shopeeProductDTO.setCurrency(toCurrency);
        shopeeProductDTO.setVPrice(price);
        shopeeProductDTO.setId(product.getId());

        shopeeProductService.update(shopeeProductDTO);
    }


    /**
     * 关闭任务
     *
     * @param key
     * @param forcedStop          强制 停止
     * @return
     */
    private int close(String key, boolean forcedStop) {
        final int currentIndex = Integer.parseInt(Objects.requireNonNull(redisStringHash.get(key, CURRENT_INDEX)))+1;
        redisStringHash.increment(key, CURRENT_INDEX, 1);
        final int currentTaskSize = Integer.parseInt(Objects.requireNonNull(redisStringHash.get(key, "currentTaskSize")));
        if (currentTaskSize <= currentIndex || forcedStop) {
            //  任務 執行完畢  并且 沒有停止
            redisStringHash.put(key, STATUS, String.valueOf(RUNTIME_END));
            zSetClose(key);
        }
        return 1;
    }

    public void coverShopeeProductSku(long productId, long copyProductId, String toCurrency) {
        shopeeProductSkuService.deleteByProduct(copyProductId);

        /*
         * 取出源商品的SKU项, 更换商品D后保存
         */
        final List<ShopeeProductSkuDTO> oldProductSkus = shopeeProductSkuService.pageByProduct(productId, new Page(0, Integer.MAX_VALUE)).getRecords();
        if (oldProductSkus.size() == 0) {
            return;
        }

        CurrencyRateResult result = null;
        final boolean notBlank = StringUtils.isNotBlank(toCurrency);
        if (notBlank) {
            result = currency.currencyConvert(oldProductSkus.get(0).getCurrency(), toCurrency);
        }
        for (ShopeeProductSkuDTO productSku : oldProductSkus) {
            productSku.setProductId(copyProductId);
            if (notBlank) {
                shopeeProductSkuService.checkPrice(toCurrency, result, productSku);
            }
            productSku.setShopeeVariationId(null);
            productSku.setOriginalPrice(null);
        }

        shopeeProductSkuService.superBatchSave(oldProductSkus, 10);
    }


}
