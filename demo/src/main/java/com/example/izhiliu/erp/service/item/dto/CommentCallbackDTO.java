package com.izhiliu.erp.service.item.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/24 13:32
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentCallbackDTO {
    private List<Long> cmtIds;
    private String commentContent;
    private String login;
}
