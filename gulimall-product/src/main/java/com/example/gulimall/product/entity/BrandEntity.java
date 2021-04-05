package com.example.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import com.example.valid.AddGroup;
import com.example.valid.ListValue;
import com.example.valid.UpdateGroup;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;

/**
 * 品牌
 * 
 * @author xubangzhu
 * @email 18773037748@gmail.com
 * @date 2020-08-29 01:29:47
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * $column.comments
	 * groups: 表名那种情况下注解生效
	 * message: jsr303 数据校验出错是的提示消息
	 * 在控制器上添加@Validated(value = {AddGroup.class})
	 */
	@Null(message = "添加，品牌idw为空",groups = {AddGroup.class})
	@NotNull(message = "修改，品牌id不能为空",groups = {UpdateGroup.class})
	@TableId
	private Long brandId;
	/**
	 * $column.comments
	 */
	@NotBlank(message = "品牌名不能为空",groups = {AddGroup.class,UpdateGroup.class})
	private String name;
	/**
	 * $column.comments
	 */
	@NotEmpty
	@URL(message = "logo必须是合法的地址")
	private String logo;
	/**
	 * $column.comments
	 */
	private String descript;
	/**
	 * $column.comments
	 * 自定义校验注解
	 */
	@ListValue(value = {0,1})
	private Integer showStatus;
	/**
	 * $column.comments
	 */
	@NotEmpty
	@Pattern(regexp = "^[a-zA-Z]&",message = "开头必须是a-z或者A-Z开头")
	private String firstLetter;
	/**
	 * $column.comments
	 */
	@NotNull
	@Min(value = 0,message = "排序必须大于0")
	private Integer sort;

}
