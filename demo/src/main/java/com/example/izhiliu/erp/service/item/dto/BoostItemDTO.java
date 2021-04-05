package com.izhiliu.erp.service.item.dto;

import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Accessors(chain = true)
public class BoostItemDTO   implements Serializable,BaseDto {


    private Long id;

    /**
     * 用户
     */
    private String login;

    /**
     * 商品id
     */
    private Long productId;

    /**
     * 所属店铺
     */
    private Long shopId;

    /**
     * 所属平台
     */
    private Long platformId;

    /**
     * 所属站点
     */
    private Long platformNodeId;

    /**
     * shopee 商品ID
     */
    private Long shopeeItemId;

    /**
     *
     */
    private LocalDateTime gmtCreate;

    /**
     *
     */
    private LocalDateTime gmtModified;

    /**
     * 扩展
     */
    private String feature;

    /**
     * 状态
     */
    private Byte status;

    /**
     * 0 正常  1 删除
     */
    private Byte deleted;


    public void setId(Long id) {
        this.id = id;
    }

    private static final long serialVersionUID = 1L;
}



