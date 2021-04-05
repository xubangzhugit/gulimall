package com.example.gulimall.product.dao;

import com.example.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性&属性分组关联
 * 
 * @author xubangzhu
 * @email 18773037748@gmail.com
 * @date 2020-08-29 01:29:47
 */
@Mapper
public interface AttrAttrgroupRelationDao extends BaseMapper<AttrAttrgroupRelationEntity> {


    void deleteBatchRelation(@Param("collect") List<AttrAttrgroupRelationEntity> collect);

}
