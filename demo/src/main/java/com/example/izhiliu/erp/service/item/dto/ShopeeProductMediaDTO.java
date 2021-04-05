package com.izhiliu.erp.service.item.dto;

import com.izhiliu.core.domain.common.BaseEntity;
import com.izhiliu.erp.service.module.metadata.dto.PriceRange;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * A ShopeeProduct. 商品媒体资源表，包括商品图片，视频，尺寸图
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeProductMediaDTO extends BaseEntity {

    private static final long serialVersionUID = -1897240127265116143L;
    private Long productId;

    private String videoInfo;

    private List<String> images;

    private String sizeChart;

    private Long  discountActivityId;

    private Boolean  discountActivity;

    private String  discountActivityName;

    //   批发价  价格区间
    private List<PriceRange> priceRange;


    /**
     *  是否二手
     */
    private Boolean isCondition;

}
