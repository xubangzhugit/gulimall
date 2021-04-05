package com.izhiliu.erp.domain.itemhistoricaldata;

import com.izhiliu.erp.web.rest.itemhistoricaldata.vm.ItemHistoricalDataVM;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.List;

@Data
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemHistoricalDataRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name ="itemId")
    private String itemId;

    @Column(name ="shopId")
    private String shopId;

    /**
     * 总销量
     */
    @Column(name ="allSales")
    private Long allSales;

    @Column(name ="data")
    private List<ItemHistoricalDataVM> data;

    @Column(name = "url")
    private String url;

    @Column(name ="status")
    private String status;
}
