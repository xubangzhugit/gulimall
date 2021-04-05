package com.izhiliu.erp.service.item.impl;

import com.alibaba.fastjson.JSON;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.service.discount.impl.DiscountPriceServiceImpl;
import com.izhiliu.erp.service.item.PlatformNodeService;
import com.izhiliu.erp.service.item.ShopeeBasicDataService;
import com.izhiliu.erp.service.item.ShopeeCategoryService;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;
import com.izhiliu.erp.service.item.dto.ShopeeAttributeDTO;
import com.izhiliu.erp.web.rest.errors.DataNotFoundException;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import com.izhiliu.erp.web.rest.item.param.LogisticsQueryQO;
import com.izhiliu.erp.web.rest.item.result.LogisticsVO;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.param.GetAttributesParam;
import com.izhiliu.open.shopee.open.sdk.api.item.result.GetAttributesResult;
import com.izhiliu.open.shopee.open.sdk.api.logistic.LogisticApi;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import com.izhiliu.open.shopee.open.sdk.entity.logistics.LogisticsResult;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/3/4 9:47
 */
@Service
public class ShopeeBasicDataServiceImpl implements ShopeeBasicDataService, Serializable {

    private Logger logger = LoggerFactory.getLogger(ShopeeBasicDataServiceImpl.class);

    @Resource
    private ItemApi itemApi;


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MessageSource messageSource;
    @Resource
    private LogisticApi logisticApi;

    @Resource
    private PlatformNodeService platformNodeService;

    @Resource
    private ShopeeCategoryService shopeeCategoryService;

    @Resource
    private UaaService uaaService;

    private static final String INVALID_CATEGORY = "invalid category id";

    private static final String CACHE_KEY_SHOPEE_ATTRIBUTES = "shopeeAttributes2:";

    private static final String CACHE_KEY_SHOPEE_LOGISTICS = "shopeeLogistics2:";

    ForkJoinPool forkJoinPool = new ForkJoinPool(20);


    @Override
    public List<ShopeeAttributeDTO> getAllAttributeByCategoryId(Long categoryId, Long platformNodeId) {
        String attributeJson = stringRedisTemplate.opsForValue().get(CACHE_KEY_SHOPEE_ATTRIBUTES + categoryId);
        if (StringUtils.isNotBlank(attributeJson)) {
            return JSON.parseArray(attributeJson, ShopeeAttributeDTO.class);
        }
        return handle(categoryId, platformNodeId);
    }

    @Override
    public List<ShopeeAttributeDTO> getAllAttributeByCategoryIdReCache(Long categoryId, Long platformNodeId) {
        return handle(categoryId, platformNodeId);
    }

    private List<ShopeeAttributeDTO> handle(Long categoryId, Long platformNodeId) {
        final PlatformNodeDTO platformNode = platformNodeService.find(platformNodeId).orElseThrow(() -> new DataNotFoundException("data.not.found.exception.platform_node_id.not.found", new String[]{platformNodeId + ""}));

        final ShopeeResult<GetAttributesResult> englishResult = itemApi.getAttributes(GetAttributesParam.builder().categoryId(categoryId).isCb(true).country(platformNode.getCode()).language("en").build());
        if (!englishResult.isResult()) {
            if (englishResult.getError().getMsg().contains(INVALID_CATEGORY)) {
                if (!shopeeCategoryService.findDeletedByPlatformNodeAndShopeeCategory(platformNodeId, categoryId).isPresent()) {
                    shopeeCategoryService.invalidCategory(platformNodeId, categoryId);
                }
                throw new DataNotFoundException("shopee.invalid.category",true);
            }
            return new ArrayList<>();
        }

        final List<GetAttributesResult.AttributesBean> englishAttributes = englishResult.getData().getAttributes();
        final ArrayList<ShopeeAttributeDTO> attributes = new ArrayList<>(englishAttributes.stream().map(this::getAttribute).sorted((a1, a2) -> a2.getEssential().compareTo(a1.getEssential())).collect(Collectors.toList()));

        if (attributes.size() > 0) {
            stringRedisTemplate.opsForValue().set(CACHE_KEY_SHOPEE_ATTRIBUTES + categoryId, JSON.toJSONString(attributes));
        }

        return attributes;
    }

    private ShopeeAttributeDTO getAttribute(GetAttributesResult.AttributesBean englishAttribute) {
        final ShopeeAttributeDTO dto = new ShopeeAttributeDTO();
        dto.setId(englishAttribute.getAttributeId());
        dto.setName(englishAttribute.getAttributeName());
        dto.setInputType(englishAttribute.getInputType());
        dto.setAttributeType(englishAttribute.getAttributeType());
        dto.setOptions(englishAttribute.getOptions());
        dto.setEssential(englishAttribute.isMandatory() ? 1 : 0);
        return dto;
    }

    @Override
    public List<LogisticsResult.LogisticsBean> getLogistics(List<Long> shopIds) {
//        final String loginId = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new DataNotFoundException("没有查询到对应的用户"));
        /*
         * 策略: 去重展示全部
         * 保存: 全传, 再拉一次物流渠道 去除不属于店铺的
         */
        return shopIds.parallelStream()
            .map(this::getLogisticsInfoByShopId)
            .flatMap(List::stream)
            .filter(LogisticsResult.LogisticsBean::getEnabled)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    @SneakyThrows
    public List<LogisticsVO> getLogisticsV3(LogisticsQueryQO qo) {
        List<Long> shopIds = qo.getShopIds();
        if (CommonUtils.isBlank(shopIds)) {
            return new ArrayList<>();
        }
        ForkJoinTask<List<LogisticsVO>> submit = forkJoinPool.submit(() ->
                shopIds.parallelStream()
                        .map(shopId -> {
                            List<LogisticsResult.LogisticsBean> logisticsInfoByShopId = this.getLogisticsInfoByShopId(shopId)
                                    .stream()
                                    .filter(LogisticsResult.LogisticsBean::getEnabled)
                                    .collect(Collectors.toList());
                            LogisticsVO vo = LogisticsVO.builder()
                                    .shopId(shopId)
                                    .logisticsList(logisticsInfoByShopId)
                                    .build();
                            return vo;
                        }).collect(Collectors.toList()));
        return submit.get();
    }

    @Override
    public boolean refreshLogistics(List<Long> shopIds) {
        final String loginId = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new DataNotFoundException("没有查询到对应的用户"));
        try {
            for (Long shopId : shopIds) {
                DiscountPriceServiceImpl.executorService.execute(() -> push(shopId,loginId));
            }
            return true;
        } catch (Exception e) {
           logger.error(e.getMessage(),e);
           return  false;
        }
    }

    @Override
    public boolean refreshAllAttributeByCategoryIdReCache(Long categoryId, Long platformNodeId) {
        try {
            DiscountPriceServiceImpl.executorService.execute(() -> handle(categoryId,platformNodeId));
            return true;
        } catch (Exception e) {
           logger.error(e.getMessage(),e);
            return  false;
        }
    }


    /**
     *    根据 shopIf  获取物流信息
     * @param shopId
     * @return
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<LogisticsResult.LogisticsBean> getLogisticsInfoByShopId(Long shopId) {

        final List<LogisticsResult.LogisticsBean> logistics = new ArrayList<>();
        try {
            String logisticJson = stringRedisTemplate.opsForValue().get(CACHE_KEY_SHOPEE_LOGISTICS + shopId);
            if (StringUtils.isNotBlank(logisticJson)) {
                return JSON.parseArray(logisticJson, LogisticsResult.LogisticsBean.class);
            }

            final ShopeeResult<LogisticsResult> response = logisticApi.getLogistics(shopId);
            if (response.isResult()) {
                //取可用的
                logistics.addAll(response.getData().getLogistics().stream().filter(LogisticsResult.LogisticsBean::getEnabled).collect(Collectors.toList()));
            } else {
                if (null != response.getError() && null != response.getError().getMsg() && response.getError().getMsg().contains("not authorized")) {
                    String shopName;
                    try {
                        shopName = Objects.requireNonNull(uaaService.getShopInfo(shopId).getBody()).getShopName();
                    } catch (Exception ex) {
                        throw new IllegalOperationException("illegal.operation.exception.shop.deauthorization",new String[]{""+shopId});
                    }
                    throw new IllegalOperationException("illegal.operation.exception.shop.deauthorization",new String[]{""+shopName});
                } else {
                    logger.error("get logistics shopId: {}  response:{}", shopId, response);
                }
            }
            //  有的 话 就一直持久化  除非是 手动刷新
            if (logistics.size() > 0) {
                stringRedisTemplate.opsForValue().set(CACHE_KEY_SHOPEE_LOGISTICS + shopId, JSON.toJSONString(logistics));
            }
//            else {
//                stringRedisTemplate.opsForValue().set(CACHE_KEY_SHOPEE_LOGISTICS + shopId, JSON.toJSONString(logistics), 5, TimeUnit.MINUTES);
//            }
        } catch (Exception e) {
            logger.error("get logistics shopId: {}  response:{}", shopId, e);
        }
        return logistics;
    }


    private List<LogisticsResult.LogisticsBean>  pull(Long shopId){
        String logisticJson = stringRedisTemplate.opsForValue().get(CACHE_KEY_SHOPEE_LOGISTICS + shopId);
        if (StringUtils.isNotBlank(logisticJson)) {
            return JSON.parseArray(logisticJson, LogisticsResult.LogisticsBean.class);
        }
        return Collections.emptyList();
    }

    private List<LogisticsResult.LogisticsBean>  push(Long shopId,String loginId){
        try {
            List<LogisticsResult.LogisticsBean> logistics = null;
            final ShopeeResult<LogisticsResult> response = logisticApi.getLogistics(shopId);
            if (response.isResult()) {
                //取可用的
                logistics = response.getData().getLogistics().stream().filter(LogisticsResult.LogisticsBean::getEnabled).collect(Collectors.toList());
            } else {
                if (null != response.getError() && null != response.getError().getMsg() && response.getError().getMsg().contains("not authorized")) {
                    String shopName = "Undefined";
                    try {
                        final List<ShopeeShopDTO> body = uaaService.getShopName(Arrays.asList(shopId), loginId).getBody();
                        if (!CollectionUtils.isEmpty(body)) {
                            shopName = body.iterator().next().getShopName();
                        }
                    } catch (Exception ex) {
                        throw new IllegalOperationException("illegal.operation.exception.shop.deauthorization",new String[]{""+shopId});
                    }
                    throw new IllegalOperationException("illegal.operation.exception.shop.deauthorization",new String[]{shopId+":"+shopName});
                } else {
                    logger.error("get logistics error  shopId: {}  response:{}", shopId, response,loginId);
                }
            }

            if (!CollectionUtils.isEmpty(logistics)) {
                //  有的 话 就一直持久化  除非是 手动刷新
                stringRedisTemplate.opsForValue().set(CACHE_KEY_SHOPEE_LOGISTICS + shopId, JSON.toJSONString(logistics));
                return  logistics;
            }
        } catch (Exception e) {
            logger.error("get logistics shopId: {}  response:{}", shopId, e);
        }
        return Collections.emptyList();
    }

}
