package com.izhiliu.erp.web.rest.discount.qo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.izhiliu.erp.web.rest.item.param.BaseItemQO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:48
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DiscountQO extends BaseItemQO {
    private String discountId;

    private List<String> discountIds;

    private String discountName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private List<SyncParam> syncParamList;

    private List<DiscountItemQO.Item> items;



    @Data
    public static class SyncParam {
        private Long shopId;
        private String discountId;
        private String discountStatus;
    }

}
