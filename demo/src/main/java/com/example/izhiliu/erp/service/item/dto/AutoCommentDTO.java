package com.izhiliu.erp.service.item.dto;

import com.izhiliu.erp.domain.item.ItemCommentDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Twilight
 * @date 2021/2/23 9:37
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AutoCommentDTO {

    private String login;

    private String shopId;

    private List<ItemCommentDetail> cmtIdList;
}
