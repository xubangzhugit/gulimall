package com.izhiliu.erp.web.rest.provider.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author harry
 * @since 2019-05-22 10:40
 */
@Data
@Accessors(chain = true)
public class HomeTodoVO {

    private Item item;

    @Data
    @Accessors(chain = true)
    public static class Item {

        /**
         * 发布失败
         */
        private Long fail;

        /**
         * 待发布
         */
        private Long pending;
    }


}
