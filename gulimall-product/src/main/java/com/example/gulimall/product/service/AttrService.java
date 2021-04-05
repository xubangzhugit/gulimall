package com.example.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.utils.PageUtils;
import com.example.gulimall.product.entity.AttrEntity;
import com.example.gulimall.product.vo.AttrGroupRelationVo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author xubangzhu
 * @email 18773037748@gmail.com
 * @date 2020-08-29 01:29:47
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<AttrEntity> getRelationAttr(Long attrgroupid);

    void deleteRelation(AttrGroupRelationVo[] attrgvo);

    PageUtils getNoRelationAttr(Long attrgroupid, Map<String, Object> params);
}

