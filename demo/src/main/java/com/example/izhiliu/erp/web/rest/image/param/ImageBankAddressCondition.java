package com.izhiliu.erp.web.rest.image.param;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.izhiliu.erp.web.rest.common.PageRequest;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 图片地址 查询条件
 *
 * @author Seriel
 * @create 2019-08-28 9:43
 **/
@Data
@Accessors( chain = true)
public class ImageBankAddressCondition  extends PageRequest {

    /**
     *      文件夹ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long folderId;

    /**
     * 图片名称关键字
     */
    private String keyword;

}
