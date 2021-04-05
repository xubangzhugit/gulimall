package com.izhiliu.erp.service.discount.mq;

import com.izhiliu.open.shopee.open.sdk.api.item.result.GetCommentResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Twilight
 * @date 2021/2/7 17:23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentDetailDTO {

    private String login;

    private String shopId;

    private List<GetCommentResult.ItemCmtListBean> itemCmtList;
}
