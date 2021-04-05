package com.izhiliu.erp.domain.enums;

/**
 * @Author: louis
 * @Date: 2019/6/25 15:33
 */
public enum InternationEnum {
    ORDER_STATUS_NO_EXIST("order.status.not.exist", "订单状态不存在"),
    LOGISTICS_WRONG("logistics.wrong", "错误的物流"),
    ORDER_NO_MATCHING("order.noMatching", "订单不匹配"),
    SYSTEM_BUSY("system.busy", "系统繁忙，请稍后重试"),
    PACKAGE_NOT_EMPTY("package.notEmpty", "包裹编号不能为空！"),
    PACKAGE_STATUS_ERRO("package.status.erro", "出库失败！原因：包含非「待出库」状态的包裹。"),
    AUTHORITY_NO("authority.no", "没有权限操作"),
    PACKAGE_REPEAT("package.repeat", "不能重复代打包"),
    UAA_NO_LOGIN("uaa.noLogin", "用 uaa 通过店铺ID {} 没有拿到 login，取消同步"),
    COURIES_REPEAT_ADD("courier.repeat", "不能新增已存在的快递单号"),
    RECORD_NOMATCHING("record_noMatching", "未能找到记录"),
    RECEIVED_CANNOT_MODIFY("received.cannot.modify", "您已收件，不能修改此快递单号~"),
    STATUS_NO_MATCHING("status.noMatching", "没有此状态！"),
    PROVIDER_NO_EXIST("provider.noExist", "物流供应商不存在"),
    SHIPPING_ADDRESS_NO_EXIST("shipping.address.noExist", "此送货地址不存在"),
    PICK_LIST_NO_EXIST("pick.list.noExist", "此选取列表模型不存在"),
    BILL_MODEL_NO_EXIST("bill.model.noExist", "这样票据模型就不存在了。"),
    ILLEGAL_OPERATION("illegal.operation", "非法操作"),
    SKUCODE_REPEAT("skucode.repeat", "存在相同skuCode的货品"),
    SKUCODE_NO_EXIST("skucode.noExist", "货品sku编码不存在"),
    WAREHOUSE_EXIST_GOODS_DEL("warehouse.existGoods.del", "此仓库有货品，不能删除"),
    CONNECTOR_ERRO("connector.erro", "不支持的连接符"),
    SHELF_EXIST_GOODS_DEL("shelf.existGoods.del", "有货架位被占用，不能删除货架"),
    WAREHOUSE_NO_EXIST("warehouse.noExist", "仓库找不到"),
    WAREHOUSE_SHELF_NULL_EDIT("warehouse.shelf.null.edit", "编辑失败！仓库编号和货架编号不能为空"),
    SHELF_NO_EXIST("shelf.noExist", "货架不存在"),
    WAREHOUSE_BARCODE_NULL("warehouse.barcode.null", "「生成货架仓位条形码」仓位为空！"),
    BARCODE_BE_FAILED("barcode.beFailed", "条形码生成失败"),
    DEFAULT_WAREHOUSE("default.warehouse", "请先设置默认仓库"),
    WAREHOUSE_GOODS_NO_EXIST("warehouse_goods_noExist", "库存中找不到此货品！"),
    WAREHOUSE_INFO_INCOMPLETE("warehouse_info_incomplete", "不完整的仓位信息"),
    ITEM_NO_EXIST("item.noExist", "找不到货品"),
    PURCHASE_ORDER_EMPTY("purchase.order.empty", "采购单不能为空"),
    WAREHOUSE_IN_FAILED("warehouse.in", "新增入库失败"),
    WAREHOUSE_NO_SKU("warehouse.noSku","仓库中找不到货品"),
    WAREHOUSE_STOCK_NO_ENOUGH("warehouse.stock.noEnough", "货品无库存，不能出库"),
    WAREHOUSE_OUT_QTY_ERRO("warehouse.outQty.erro", "出库数量大于库存数量！剩余库存"),
    WAREHOUSE_OUT_FAILED("warehouse.out.failed", "新增出库失败"),
    SHOPEE_PRODUCT_DELETE("shopee.product.delete", "该商品已被删除, 请返回商品列表刷新页面"),
    CATEGORY_ERRO("category.erro", "类目异常"),
    SYNCING("syncing", "正在同步中,剩余数量"),
    UNIT_ERRO("unit.erro", "单位错误"),
    WAYBILL_NO_MATCHING("waybill.noMatching", "面单未找到，请同步此订单~"),
    REPETITION_SYNC_COMMODITY("repetition.sync.commodity", "不能短时间内重复同一个商品~"),
    SHOPEE_REQUEST_ERROR("shopee.request.error", "商品请求有误~"),
    SEND_MESSAGE_FAILED("send.message.failed", "消息发送失败"),
    SYNC_EXCEPTION("sync.exception", "同步商品未知异常"),
    ITEM_VARIATION_IMAGE_NOTNULL("item.variation.image.notnull", "规格图不能为空"),
    ITEM_DISCOUNT_NO_EXIST("item.discount.no.exist", "折扣活动不为空"),
    ITEM_DISCOUNT_START_TIME_ERROR("item.discount.start.time.error", "折扣开始时间不能小于当前时间"),
    DISCOUNT_HASPARTICIPATED_IN_OTHER_PROMOTION("discount.hasparticipated.in.other.promotion", "当前商品已经参加其他折扣"),
    DISCOUNT_PROMOTION_PRICE_LOWER_THAN_ORIGIN_PRICE("discount.promotion.price.lower.than.origin.price", "折扣价必须低于原价格"),

    ;

    private String code;
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    InternationEnum() {
    }

    InternationEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
