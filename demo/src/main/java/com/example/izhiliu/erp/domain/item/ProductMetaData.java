package com.izhiliu.erp.domain.item;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * A ProductMetaData.
 * <p>
 * TODO 源数据是存放在 MongoDB 的. 该数据是采集来的原始数据模型
 */
@Data
@Accessors(chain = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ProductMetaData implements Serializable {

    private static final long serialVersionUID = -4207893198602572214L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "url")
    private String url;

    @Column(name = "name")
    private String name;

    @Column(name = "platform")
    private String platform;

    @Column(name = "suffix")
    private String suffix;

    @Column(name = "description")
    private String description;

    @Column(name = "price")
    private Long price;

    @Column(name = "currency")
    private String currency;

    @Column(name = "sold")
    private Integer sold;

    @Column(name = "collect_time")
    private Instant collectTime;

    @Column(name = "status")
    private Integer status;

    @Column(name = "images")
    private List<String> images;

    /**
     * 将images拆开为主图和详情描述图
     */
    @Column(name = "main_images")
    private List<String> mainImages;

    @Column(name = "desc_images")
    private List<String> descImages;

    @Column(name = "login_id")
    private String loginId;

    @Column(name = "platform_id")
    private Long platformId;

    @Column(name = "platform_node_id")
    private Long platformNodeId;

    @Column(name = "key")
    private String key;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "json")
    private String json;

    @Column(name = "categoryId")
    private Long   categoryId;
}
