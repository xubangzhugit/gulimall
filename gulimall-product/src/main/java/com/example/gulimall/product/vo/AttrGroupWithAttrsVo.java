package com.example.gulimall.product.vo;

import com.example.gulimall.product.entity.AttrEntity;
import com.example.gulimall.product.entity.AttrGroupEntity;
import lombok.Data;

import java.util.List;

@Data
public class AttrGroupWithAttrsVo extends AttrGroupEntity {

    //保存attr属性实体信息
    private List<AttrEntity> attrs;
}
