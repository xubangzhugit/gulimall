package com.izhiliu.erp.web.rest.item;

/**
 * describe: 消息队列类型
 * <p>
 *
 * @author cheng
 * @date 2019/2/21 16:53
 */
public enum ErpActionEnum {
    /**
     * TO_ERP_COLLECT_MSG
     */
    COLLECT_SHOPEE(1, "COLLECT_SHOPEE", "采集虾皮商品"),
    COLLECT_ALIBABA(2, "COLLECT_ALIBABA", "采集1688商品"),
    COLLECT_LAZADA(3, "COLLECT_LAZADA", "采集来赞达商品"),
    COLLECT_EXPRESS(4, "COLLECT_EXPRESS", "采集速卖通商品"),
    COLLECT_TB(5, "COLLECT_TAOBAO", "采集淘宝商品"),
    COLLECT_TM(6, "COLLECT_TIANMAO", "采集天猫商品"),
    COLLECT_PDD(7, "COLLECT_PDD", "采集拼多多商品"),
    COLLECT_E7(17, "COLLECT_E7", "采集17商品"),

    SHOPEE_HEARTBEAT_PACKAGE(512, "SHOPEE_HEARTBEAT_PACKAGE", "虾皮登陆态心跳包");

    public Integer code;
    public String tag;
    public String info;

    ErpActionEnum(Integer code, String tag, String info) {
        this.code = code;
        this.tag = tag;
        this.info = info;
    }}
