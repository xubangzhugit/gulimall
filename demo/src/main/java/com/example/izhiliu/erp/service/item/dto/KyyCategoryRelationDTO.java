package com.izhiliu.erp.service.item.dto;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * @author Twilight
 * @date 2021/1/18 17:45
 */

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KyyCategoryRelationDTO{

    private Long id;

    private String login;

    private Long productId;

    private String kyyCategoryId;
}
