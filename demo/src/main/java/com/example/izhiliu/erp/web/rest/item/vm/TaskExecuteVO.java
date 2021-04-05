package com.izhiliu.erp.web.rest.item.vm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/7/15 11:52
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskExecuteVO {

    public static Integer CODE_PENDING = 0;
    public static Integer CODE_SUCCESS = 1;
    public static Integer CODE_FAILD = 2;

    public static String TASK_TYPE_ITEM_UPDATE = "item_update";
    public static String TASK_TYPE_ITEM_IMAGE_TO_SHOPEE = "item_image_to_shopee";
    public static String TASK_TYPE_DISCOUNT_ITEM_EDIT = "discount_item_edit";

    // 0: 处理中； 1：成功； 2: 失败
    private Integer code;
    private Object data;
    private String taskId;
    // create or preview
    private String taskType;
    private String errorMessage;
    private List<TaskDetail> taskDetailList;
    private Long total;
    private int syncShop;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TaskDetail {
        private String taskId;
        private String detailId;
        private String detailName;
        private Integer code;
        private String errorMessage;
        private Integer endCode;
        private Object endData;
        private String taskType;
        private int success;
        private int total;
        private int fail;
        private List detailData;
    }
}
