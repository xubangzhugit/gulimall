package com.izhiliu.erp.domain.image;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 图片银行  文件夹管理
 *
 * @author Seriel
 * @create 2019-08-27 9:47
 **/
@TableName("item_image_bank_directory")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ImageBankDirectory extends BaseEntity {
    @TableId(
            type = IdType.INPUT
    )
    private Long id;

    private  Long parentId;

    private  String  name;

    private  Integer  level;

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
