package com.izhiliu.erp.service.discount.mq;

import com.izhiliu.erp.web.rest.discount.qo.DiscountQO;
import com.izhiliu.erp.web.rest.item.param.CommentSyncQO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Twilight
 * @date 2021/2/7 16:08
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentSyncDTO {

    private String login;
    private String taskId;
    private List<CommentSyncQO.ItemInfo> itemInfoList;
}
