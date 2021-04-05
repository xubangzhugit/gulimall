package com.izhiliu.erp.web.rest.item.param;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Twilight
 * @date 2021/1/19 9:29
 */
@Data
public class KyyCategoryRelationQO {

    private String login;

    @Valid
    private List<ProductBean> productBeanList;

    @Data
    public static class ProductBean{

        @NotNull
        private Long productId;

        @NotBlank
        private String kyyCategoryId;
    }

}
