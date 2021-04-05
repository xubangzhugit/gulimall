package com.izhiliu.erp.service.item.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Set;


@Data
@Accessors(chain = true)
public class ProductSearchStrategyDTO implements Serializable {

    private static final long serialVersionUID = 9132561228663966419L;

    public static final   String fields[]={"name","sku_code","shopee_item_id","product_code"};
    /**现货*/
    public static final Integer SPOT = 3;
    /**预售*/
    public static final Integer PRE_SALE = 5;

    private List<String> subAccounts;
    private String id;
    private String name;
    private String field;
    private String source;
    private Integer recently;
    private Integer urecently;

    private List<Long> publishShops;
    private List<Long> unpublishShops;
    private List<Long> collectProductIds;

    /**<=3 现货，>=5 预售*/
    private Integer sendOutTime;


    /**
     *   用来查询skucode 的
     */
    private Set<Long> productIds;

    private Integer variationTier;

    private String startDate;
    private String endDate;

    private String ustartDate;
    private String uendDate;

    private Boolean online;
    private Boolean boost = Boolean.FALSE;
    private String keyword;
    private String loginId;
    @JsonAlias("status")
    private Integer boostStatus;

    @NotNull(groups = SearchPlatform.class)
    private Long platformId;

    @NotNull(groups = Create.class)
    private Integer type;

    private LocalProductStatus localStatus;
    private ShopeeItemStatus remoteStatus;

    @NotNull(groups = {SearchPlatform.class, SearchShop.class})
    private Integer page;

    @NotNull(groups = {SearchPlatform.class, SearchShop.class})
    private Integer size;


    private  boolean isSku;

    /**
     * erp客优云类目idList
     */
    private List<String> kyyCategoryIdList;

    public interface Create {
    }

    public interface SearchPlatform {
    }

    public interface SearchShop {
    }
}
