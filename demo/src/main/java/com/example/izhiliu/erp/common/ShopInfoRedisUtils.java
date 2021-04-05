package com.izhiliu.erp.common;

import com.alibaba.fastjson.JSON;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: louis
 * @Date: 2019/8/27 21:48
 */
@Component
public class ShopInfoRedisUtils {

    private Logger logger = LoggerFactory.getLogger(ShopInfoRedisUtils.class);
    private final static String KEY = "jax_consumer_shop_info2:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UaaService uaaService;

    public void toSetShopBaseInfo(String redisKey, ShopeeShopDTO shopeeShopDTO) {
        if (null != shopeeShopDTO && CommonUtils.isNotBlank(shopeeShopDTO.getShopId())) {
            ShopeeShopDTO temp = new ShopeeShopDTO();
            temp.setShopId(shopeeShopDTO.getShopId());
            temp.setShopName(shopeeShopDTO.getShopName());
            temp.setLogin(shopeeShopDTO.getLogin());
            temp.setKyyShopId(shopeeShopDTO.getKyyShopId());
            temp.setWiseAccount(shopeeShopDTO.getWiseAccount());
            stringRedisTemplate.opsForValue().set(redisKey, JSON.toJSON(temp).toString(), 30, TimeUnit.MINUTES);
        }
    }

    /**
     * 情况： 1、正常数据  2、获取feign失败，抛出异常，重复消费  3、当前用户解授权了，返回为null，直接放行
     * @param shopId
     * @return
     */
    public ShopeeShopDTO getBaseShopInfo(Long shopId) {
        ShopeeShopDTO shopDTO = new ShopeeShopDTO();
        try {
            if (CommonUtils.isBlank(shopId)) {
                return shopDTO;
            }
            String redisKey = KEY + shopId;
            String s = stringRedisTemplate.opsForValue().get(redisKey);
            // 获取店铺 和当前授权用户 信息
            if (StringUtils.isNotBlank(s)) {
                shopDTO = JSON.parseObject(s, ShopeeShopDTO.class);
                logger.info("获取baseShopInfo_缓存: shopId={},data={}", shopId, getShopeeShopData(shopDTO));
            } else {
                shopDTO = uaaService.getShopInfo(Long.valueOf(shopId)).getBody();

                if (null != shopId && null != shopDTO) {
                    toSetShopBaseInfo(redisKey, shopDTO);
                }
                logger.info("获取baseShopInfo_feign: shopId={},data={}", shopId, getShopeeShopData(shopDTO));
            }
        } catch (Exception e) {
            logger.error("获取店铺信息出错,shopId={}", shopId, e);
            throw new LuxServerErrorException("获取店铺信息失败");
        } finally {
            logger.info("返回店铺信息：shopId={},data={}", shopId, getShopeeShopData(shopDTO));
            if (CommonUtils.isNotBlank(shopDTO) &&
                CommonUtils.isNotBlank(shopDTO.getShopId()) &&
                !shopDTO.getShopId().equals(shopId)) {
                logger.error("订单同步出现shopId不一致,shopId={},data={}", shopId, getShopeeShopData(shopDTO));
                return null;
            }
        }
        return shopDTO;
    }

    /**
     * 获取已授权的店铺id
     * @param shopId
     * @param includChild
     * @return
     */
    public List<String> getShopId(List<String> shopId, boolean includChild) {
        List<String> result = new ArrayList<>();
        String parentLogin = SecurityUtils.currentLogin();
        boolean subAccount = SecurityUtils.isSubAccount();
        List<ShopeeShopDTO> shopeeShopDTOList = uaaService
                .getAllShopeeShopInfo(SecurityUtils.getCurrentLogin(), subAccount)
                .getBody();
        // 所有店铺id集合
        if (CommonUtils.isNotBlank(shopeeShopDTOList)) {
            result = shopeeShopDTOList.stream().map(ShopeeShopDTO::getShopCode).collect(Collectors.toList());
            if (CommonUtils.isNotBlank(shopId)) {
                //过滤主店铺
                result.retainAll(shopId);
                if (includChild) {
                    //子店铺
                    List<String> childShopId = shopeeShopDTOList.stream().filter(e -> CommonUtils.isNotBlank(e.getParentId()))
                            .map(e -> {
                                if (shopId.stream().filter(f -> e.getParentId().toString().equals(f))
                                        .findFirst().isPresent()) {
                                    return e.getShopCode();
                                }
                                return null;
                            }).filter(Objects::nonNull).collect(Collectors.toList());
                    result.addAll(childShopId);
                }
            }
        }
        if (CommonUtils.isBlank(result)) {
            result = Arrays.asList("-1");
        }
        return result;
    }

    private static String getShopeeShopData(ShopeeShopDTO shopeeShopDTO) {
        return CommonUtils.isBlank(shopeeShopDTO) ? null : shopeeShopDTO.toString();
    }
}
