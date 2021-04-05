package com.izhiliu.erp.domain.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.time.Instant;

/**
 * @author Twilight
 * @date 2021/2/7 17:39
 */
@Data
@Accessors(chain = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemCommentDetail implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "login")
    private String login;

    @Column(name = "shop_id")
    private String shopId;

    @Column(name = "item_id")
    private String itemId;

    @Column(name = "cmt_id")
    private Long cmtId;

    @Column(name = "ordersn")
    private String ordersn;

    @Column(name = "comment")
    private String comment;

    @Column(name = "buyer_username")
    private String buyerUsername;

    @Column(name = "rating_star")
    private Integer ratingStar;

    @Column(name = "cmt_reply")
    private String cmtReply;

    @Column(name = "create_time")
    private Instant createTime;

}
