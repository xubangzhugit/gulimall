package com.example.gulimall.gulimall_ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.utils.PageUtils;
import com.example.gulimall.gulimall_ware.entity.WareSkuEntity;

import java.util.Map;

/**
 * 商品库存
 *
 * @author xubangzhu
 * @email 18773037748@gmail.com
 * @date 2020-08-29 19:24:58
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

