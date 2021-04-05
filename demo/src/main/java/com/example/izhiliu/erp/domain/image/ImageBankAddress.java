package com.izhiliu.erp.domain.image;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 图片银行  url 地址
 *
 * @author Seriel
 * @create 2019-08-27 9:45
 **/
@TableName("item_image_bank_address")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ImageBankAddress extends  BaseEntity {
    @TableId(
            type = IdType.INPUT
    )
    private Long id;


    private  Long directoryId;

    private  String  name;

    private  String  url;

    private  Long imageSize;

    private  String LoginId;

    /**
     *   客优云 的 id
     */
    private   Long  kkyId;

    private  Integer status;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
