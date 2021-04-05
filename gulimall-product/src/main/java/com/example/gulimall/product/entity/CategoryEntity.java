package com.example.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 商品三级分类
 * 
 * @author xubangzhu
 * @email 18773037748@gmail.com
 * @date 2020-08-29 01:29:47
 */
@Data
@TableName("pms_category")
public class CategoryEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * $column.comments
	 */
	@TableId
	private Long catId;
	/**
	 * $column.comments
	 */
	private String name;
	/**
	 * $column.comments
	 */
	private Long parentCid;
	/**
	 * $column.comments
	 */
	private Integer catLevel;
	/**
	 * $column.comments
	 * 该注解配合
	 * logic-delete-value: 1     #配置mybatis plus 逻辑删除字段值
	 * logic-not-delete-value: 0#配置mybatis plus 逻辑不删除字段值
	 * 使用，
	 * value和delval 的值用于处理配置的值与数据库中的值不一样的情况
	 */
	@TableLogic(value = "1",delval = "0")
	private Integer showStatus;
	/**
	 * $column.comments
	 */
	private Integer sort;
	/**
	 * $column.comments
	 */
	private String icon;
	/**
	 * $column.comments
	 */
	private String productUnit;
	/**
	 * $column.comments
	 */
	private Integer productCount;
	@JsonInclude(value = JsonInclude.Include.NON_EMPTY)//该注解当字段值为空时，不序列化到前端
	@TableField(exist = false)
    private List<CategoryEntity> childer;
}
