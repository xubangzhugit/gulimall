package com.izhiliu.erp.service.item.impl;

import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.config.item.ItemDtsConfig;
import com.izhiliu.erp.config.mq.vo.ShopeeActionVO;
import com.izhiliu.erp.config.strategy.ShopeePushContent;
import com.izhiliu.erp.service.item.BaseShopeePushService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeePushResult;
import com.izhiliu.erp.service.mq.producer.MQProducerService;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.param.PushToShopeeTaskQO;
import com.izhiliu.erp.web.rest.item.param.ShopProductParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * shopee推送商品物流信息
 * @Author: louis
 * @Date: 2020/7/14 16:56
 */
@Component
@Slf4j
public class ShopeePushItemAllServiceImpl implements BaseShopeePushService {

    @PostConstruct
    public void init() {
        ShopeePushContent.pushServiceMap.put(PushToShopeeTaskQO.ITEM_ALL, this);
    }
    @Resource
    private MQProducerService mqProducerService;
    @Resource
    private ItemDtsConfig itemDtsConfig;

    @Override
    public ShopeePushResult doPush(ShopProductParam param) {
        log.info("shopee推送更新全部信息");
        final ShopeeProductDTO product = param.getProduct();
        final Long productId = param.getProductId();
        final Long shopId = param.getShopId();
        ShopeePushResult result = ShopeePushResult.builder()
                .productId(param.getProductId())
                .pushType(PushToShopeeTaskQO.ITEM_ALL)
                .build();
        try {
            //检查是否开启acm配置文件获取dts
            Integer dts = product.getSendOutTime();
            String dtsByConfig = itemDtsConfig.getDtsByConfig();
            if (CommonUtils.isNotBlank(dtsByConfig)){
                dts = Integer.valueOf(dtsByConfig);
            }
            final ShopeeActionVO action = ShopeeActionVO.builder()
                    .action(ShopeeActionVO.Action.PUSH.getCode())
                    .shopId(shopId)
                    .productId(productId)
                    .loginId(product.getLoginId())
                    .dts(dts)
                    .build();
            mqProducerService.sendMQ("SHOPEE_ACTION_PUSH", shopId + ":" + productId, action);
        } catch (LuxServerErrorException e) {
            result.setErrorMessage(e.getTitle());
        } catch (Exception e) {
            log.error("shopee推送更新全部信息出错,productId:{}", param.getProductId(), e);
            result.setErrorMessage(CommonUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "系统繁忙");
        }
        return result;
    }
}
