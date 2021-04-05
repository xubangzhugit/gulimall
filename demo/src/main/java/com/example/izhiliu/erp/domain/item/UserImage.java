package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A ShopeeProductImage.
 */
@TableName("item_user_image")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class UserImage extends BEntity {

    private static final long serialVersionUID = -2860271856538388549L;

    private String url;

    private String filename;

    private Long size;

    private String loginId;
}
