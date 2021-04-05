package com.izhiliu.erp.domain.item;

import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Id;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * describe: 商品搜索策略
 * <p>
 *
 * @author cheng
 * @date 2019/2/18 10:36
 */
@Data
@Accessors(chain = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ProductSearchStrategy implements Serializable {

    private static final long serialVersionUID = 9132561228663966419L;

    @Id
    private String id;
    private String name;
    private String field;
    private String source;
    private List<Long> publishShops;
    private List<Long> unpublishShops;
    private Integer variationTier;

    private List<String> subAccounts;

    private Instant startDate;
    private Instant endDate;

    private Instant ustartDate;
    private Instant uendDate;

    private String keyword;
    private String loginId;
    private Long platformId;
    private Integer type;
    private Integer recently;
    private Integer urecently;
    private Boolean online;

    private LocalProductStatus localStatus;
    private ShopeeItemStatus remoteStatus;
}
