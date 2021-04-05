package com.izhiliu.erp.domain.item;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Id;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/15 15:31
 */
@Data
@Accessors(chain = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class CurrencyRate implements Serializable {

    private static final long serialVersionUID = -671337597879231533L;

    @Id
    private String id;

    private String from;

    private String to;

    private BigDecimal rate;

    private Instant lastSyncTime;
}
