package com.izhiliu.erp.service.item.impl;

import com.izhiliu.core.common.constant.PlatformNodeEnum;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.config.subscribe.Enum.SubLimitProductConstant;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.core.util.Constants;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.ShopeeCopyProductService;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.module.channel.ShopeeModelChannel;
import com.izhiliu.erp.web.rest.errors.DataNotFoundException;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import com.izhiliu.erp.web.rest.item.param.CopyToShop;
import com.izhiliu.erp.web.rest.item.param.SaveToNode;
import com.izhiliu.erp.web.rest.item.param.SaveToShop;
import com.izhiliu.erp.web.rest.item.result.CopyShopProductResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/9 11:36
 */
@Service
public class ShopeeCopyProductServiceImpl extends ShopeeProductServiceImpl implements ShopeeCopyProductService {

    private final Logger log = LoggerFactory.getLogger(ShopeeCopyProductServiceImpl.class);
    private ExecutorService executor = Executors.newFixedThreadPool(10);

    @Resource
    private ShopeeModelChannel shopeeModelChannel;

    @Override
    public List<Long> copyPlatformToNode(SaveToNode saveToNode) {
        List<Future<Long>> futures = new ArrayList<>();
        for (final Long productId : saveToNode.getProductIds()) {
            futures.add(executor.submit(() -> copyToPlatformNode(productId, saveToNode.getNodeId())));
        }

        return getIds(futures);
    }

    @Override
    public void copyNodeToShop(List<SaveToShop> saveToShops) {
        final String currentLogin = SecurityUtils.currentLogin();
        for (final SaveToShop saveToShop : saveToShops) {
            for (final Long shopId : saveToShop.getShopIds()) {
                executor.execute(() -> copyToShop(saveToShop.getProductId(), shopId,currentLogin));
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void publishNodeToShop(List<SaveToShop> saveToShops) {
        final String currentLogin = SecurityUtils.currentLogin();
        for (final SaveToShop saveToShop : saveToShops) {
            for (final Long shopId : saveToShop.getShopIds()) {
                final Long productId = copyToShop(saveToShop.getProductId(), shopId,currentLogin);
                executor.execute(() -> shopeeModelChannel.publishSupport(productId,shopId,currentLogin));
            }
        }
    }

    @Override
    public List<CopyShopProductResult> copyToShop(List<CopyToShop> copyToShops) {
        List<CopyShopProductResult> results = new ArrayList<>();
        for (final CopyToShop copyToShop : copyToShops) {
            try {
                results.add(executor.submit(() -> copy(copyToShop)).get());
            } catch (Exception e) {
                results.add(CopyShopProductResult
                    .error()
                    .setFromId(copyToShop.getShopProductId())
                    .setShopId(copyToShop.getShopId())
                    .setMessage(e.getMessage()));
            }
        }

        return results;
    }

    public CopyShopProductResult copy(CopyToShop copyToShop) {
        try {
            ShopeeProductDTO product = find(copyToShop.getShopProductId()).orElseThrow(() -> new DataNotFoundException("product.not.found",true));
            PlatformNodeDTO platformNode = platformNodeService.find(copyToShop.getPlatformNodeId()).orElseThrow(() -> new DataNotFoundException("data.not.found.exception.invalid.platform.node.id",true));
            validate(copyToShop.getShopId(), product);

            /*
             * 查询目标店铺是否已经存在子商品
             *  存在: 直接返回子商品ID
             *  不存在: 拷贝商品到目标店铺 返回商品ID
             */

            Optional<ShopeeProduct> shopProduct = selectShopProductByParentIdAndShopId(copyToShop.getShopProductId(), copyToShop.getShopId());
            if (shopProduct.isPresent()) {
                return CopyShopProductResult
                    .ok()
                    .setPublish(product.getPlatformNodeId().equals(copyToShop.getPlatformNodeId()))
                    .setFromId(copyToShop.getShopProductId())
                    .setShopId(copyToShop.getShopId())
                    .setNewId(shopProduct.get().getId());
            } else {
                return CopyShopProductResult
                    .ok()
                    .setPublish(product.getPlatformNodeId().equals(copyToShop.getPlatformNodeId()))
                    .setFromId(copyToShop.getShopProductId())
                    .setShopId(copyToShop.getShopId())
                    .setNewId(copy(copyToShop.getShopId(), copyToShop.getPlatformNodeId(), product, platformNode));
            }
        } catch (Exception e) {
            log.error("[拷贝店铺商品异常]", e);
            return CopyShopProductResult
                .error()
                .setFromId(copyToShop.getShopProductId())
                .setShopId(copyToShop.getShopId())
                .setMessage(e.getMessage());
        }
    }

    @Override
    public Long copyToShop(long shopProductId, long shopId, long platformNodeId) {
        ShopeeProductDTO product = find(shopProductId).orElseThrow(() -> new DataNotFoundException("product.not.found",true));
        PlatformNodeDTO platformNode = platformNodeService.find(platformNodeId).orElseThrow(() -> new DataNotFoundException("data.not.found.exception.invalid.platform.node.id",true));
        validate(shopId, product);

        /*
         * 查询目标店铺是否已经存在子商品
         *  存在: 直接返回子商品ID
         *  不存在: 拷贝商品到目标店铺 返回商品ID
         */

        Optional<ShopeeProduct> shopProduct = selectShopProductByParentIdAndShopId(shopProductId, shopId);
        if (shopProduct.isPresent()) {
            return shopProduct.get().getId();
        } else {
            return copy(shopId, platformNodeId, product, platformNode);
        }
    }

    private void validate(long shopId, ShopeeProductDTO product) {
        final String currentUserLogin = SecurityUtils.currentLogin();
        if (!product.getLoginId().equals(currentUserLogin) && !currentUserLogin.equals(Constants.ANONYMOUS_USER)) {
            throw new IllegalOperationException("illegal.operation.exception",true);
        }
        if (product.getShopId().equals(shopId)) {
            throw new IllegalOperationException("illegal.operation.exception.copy.product.exception",true);
        }
    }

    private long copy(long shopId, long platformNodeId, ShopeeProductDTO product, PlatformNodeDTO platformNode) {
        Long sourcePlatformNodeId = product.getPlatformNodeId();
        Long productId = product.getId();

        product.setParentId(productId);

        // 初始化状态
        product.setStatus(LocalProductStatus.NO_PUBLISH);
        product.setShopeeItemStatus(ShopeeItemStatus.UNKNOWN);

        product.setGmtCreate(null);
        product.setGmtModified(null);
        product.setShopeeItemId(null);
        product.setLogistics(null);
        product.setOnlineUrl(null);

        product.setPlatformNodeId(platformNodeId);
        product.setShopId(shopId);
        save(product);

        if (!sourcePlatformNodeId.equals(platformNodeId)) {
            product.setCategoryId(null);
            product.setShopeeCategoryId(null);
            refreshPrice(product, platformNode.getCurrency());
        } else {
            shopeeProductAttributeValueService.copyShopeeProductAttributeValue(productId, product.getId());
        }
        shopeeSkuAttributeService.copyShopeeSkuAttribute(productId, product.getId());
        shopeeProductSkuService.copyShopeeProductSku(
            productId, product.getId(),
            platformNode.getCurrency(),
            PlatformNodeEnum.SHOPEE_TW.id.equals(platformNodeId));
        return product.getId();
    }

    private List<Long> getIds(List<Future<Long>> futures) {
        return futures.stream()
            .map(this::getId)
            .collect(Collectors.toList());
    }

    private Long getId(Future e) {
        try {
            return (Long) e.get();
        } catch (Exception ex) {
            log.error("[copyToNode]", ex);
            return -1L;
        }
    }
}
