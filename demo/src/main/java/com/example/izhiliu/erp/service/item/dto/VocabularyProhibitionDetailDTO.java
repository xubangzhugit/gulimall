package com.izhiliu.erp.service.item.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 14:51
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VocabularyProhibitionDetailDTO {
    /**
     * 词汇ID
     */
    private Long id;
    /**
     * 词汇类型ID
     */
    private Long vocabularyId;
    /**
     * 登录名
     */
    private String login;
    /**
     * 词汇名称
     */
    private String vocabularyName;

    private Integer deleted;
}
