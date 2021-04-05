package com.izhiliu.erp.service.item.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 14:49
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VocabularyProhibitionDTO {
    /**
     * 词汇类型ID
     */
    private Long id;
    /**
     * 登录名
     */
    private String login;
    /**
     * 词汇类型名称
     */
    private String vocabularyType;
}
