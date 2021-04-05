package com.izhiliu.erp.web.rest.vm;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Item {

    /**
     * 发布失败
     */
    private Long fail;

    /**
     * 待发布
     */
    private Long pending;
}
