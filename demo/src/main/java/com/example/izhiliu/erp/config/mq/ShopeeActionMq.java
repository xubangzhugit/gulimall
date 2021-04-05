package com.izhiliu.erp.config.mq;

import com.izhiliu.core.Exception.AbstractException;
import com.izhiliu.core.config.subscribe.Enum.SubLimitProductConstant;
import com.izhiliu.erp.config.aop.subscribe.SubLimitService;
import com.izhiliu.erp.config.mq.vo.ShopeeActionVO;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.log.LogConstant;
import com.izhiliu.erp.log.LoggerOp;
import com.izhiliu.erp.service.item.ShopeeProductAttributeValueService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.ShopeeProductSkuService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import com.izhiliu.erp.service.mq.producer.MQProducerService;
import com.izhiliu.erp.web.rest.errors.DataNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Optional;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/9 19:00
 */
//@Component
@Deprecated
public class ShopeeActionMq {

    private static final Logger log = LoggerFactory.getLogger(ShopeeActionMq.class);

    @Resource
    private ShopeeProductService shopeeProductService;


    @Resource
    private ShopeeProductAttributeValueService shopeeProductAttributeValueService;


    @Resource
    private HandleProductExceptionInfo handleProductExceptionInfo;

    @Resource
    private MQProducerService mqProducerService;
    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;

    @Resource
    SubLimitService subLimitService;

    public void publish(long productId, String currentLogin) {
        final LoggerOp loggerOp = getLoggerOpObejct();
        loggerOp.setMessage("[开始发布] - 产品编号:{}");
        log.info(loggerOp.toString(), productId);
        try {
            final ShopeeProductDTO product = getProduct(productId);
            if (product.getShopeeItemId() != null) {
                loggerOp.setMessage("[此产品已发布, 无法再次发布] - 产品编号:{}").setStatusPlus(LoggerOp.Status.ERROR);
                log.warn(loggerOp.toString(), productId);
                return;
            }

            if (checkStatus(product)) {
                loggerOp.setMessage("[发布] - 商品正在执行操作, 本次任务拒绝执行, productId: {}, status: {}").setStatusPlus(LoggerOp.Status.ERROR);
                log.warn(loggerOp.toString(),  product.getId(), product.getStatus().status);
                return;
            }

//            if (ShopeeProduct.VariationTier.TWO.val.equals(product.getVariationTier()) && PlatformNodeEnum.SHOPEE_TW.id.equals(product.getPlatformNodeId())) {
//                throw new IllegalOperationException(handleProductExceptionInfo.doMessage("illegal.operation.exception.tw.double.sku"),true);
//            }

            /*
             * 提前校验 确保提交到MQ的任务绝大部分是可正常执行的
             */
            validate(productId, product);

            shopeeProductSkuService.checkPublishParam(productId);

            final ShopeeActionVO action = ShopeeActionVO.builder()
                .action(ShopeeActionVO.Action.PUBLISH.getCode())
                .shopId(product.getShopId())
                .productId(productId)
                .loginId(product.getLoginId())
                .build();

            mqProducerService.sendMQ("SHOPEE_ACTION_PUBLISH", product.getShopId() + ":" + productId, action);
        } catch (Exception e) {
            loggerOp.setMessage("[发布商品异常]").setStatusPlus(LoggerOp.Status.ERROR);
            log.error(loggerOp.toString(), e);
            subLimitService.doAfterThrowing(currentLogin, SubLimitProductConstant.RELEASE_OFFER_COUNT);
            fail(productId, handleProductExceptionInfo.doMessage(e.getMessage()), LocalProductStatus.PUBLISH_FAILURE);
        }
    }

    LoggerOp getLoggerOpObejct() {
        return new LoggerOp().setStatusPlus(LoggerOp.Status.START).setKind(LogConstant.SHOPEE_PRODUCT).setType("create").setCode(LogConstant.PUBLISH);
    }

    public void push(long productId) {
        try {
            final ShopeeProductDTO product = getProduct(productId);
            if (product.getShopeeItemId() == null) {
                log.warn("[此产品尚未发布, 无法更新] - 产品编号:{}", productId);
                return;
            }

            /*
             * 正在操作
             */
            if (checkStatus(product)) {
                return;
            }
            validate(productId, product);

            final ShopeeActionVO action = ShopeeActionVO.builder()
                .action(ShopeeActionVO.Action.PUSH.getCode())
                .shopId(product.getShopId())
                .productId(productId)
                .loginId(product.getLoginId())
                .build();

            mqProducerService.sendMQ("SHOPEE_ACTION_PUSH", product.getShopId() + ":" + productId, action);
        }  catch (AbstractException e) {
            log.error("[更新商品异常]", e);
            fail(productId, handleProductExceptionInfo.doMessage(e.getMessage(),e.getParam()), LocalProductStatus.PUSH_FAILURE);
        }
    }

    public void pull(long itemId, long shopId, String loginId) {
        try {
            /*
             * 重复同步
             */
            final Optional<ShopeeProductDTO> productExist = shopeeProductService.selectByItemId(itemId);
            if (productExist.isPresent()) {
                if (productExist.get().getStatus().equals(LocalProductStatus.IN_PULL)) {
                    return;
                }
            }

            final ShopeeActionVO action = ShopeeActionVO.builder()
                .action(ShopeeActionVO.Action.PULL.getCode())
                .shopId(shopId)
                .itemId(itemId)
                .loginId(loginId)
                .build();

            mqProducerService.sendMQ("SHOPEE_ACTION_PULL", shopId + ":" + itemId, action);
        }  catch (Exception e) {
            log.error("[同步商品异常]", e);
        }
    }

    private ShopeeProductDTO getProduct(Long id) {
        final Optional<ShopeeProductDTO> productExist = shopeeProductService.find(id);
        if (!productExist.isPresent()) {
            throw new DataNotFoundException("data.not.found.exception.product.not.found", new String[]{"product id : " + id});
        }

        return productExist.get();
    }

    private boolean checkStatus(ShopeeProductDTO product) {
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
        shopeeProductAttributeValueService.checkRequired(productId, product.getShopeeCategoryId(), product.getPlatformNodeId());
    }

    private void fail(long productId, String msg, LocalProductStatus status) {
        final ShopeeProductDTO product = new ShopeeProductDTO();
        product.setId(productId);
        product.setFeature(msg);
        refreshStatus(product, status);
    }

    private void refreshStatus(ShopeeProductDTO product, LocalProductStatus inPush) {
        product.setStatus(inPush);
        shopeeProductService.update(product);
    }
}
