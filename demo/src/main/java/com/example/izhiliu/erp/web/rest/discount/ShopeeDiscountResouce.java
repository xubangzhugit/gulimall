package com.izhiliu.erp.web.rest.discount;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.codahale.metrics.annotation.Timed;
import com.izhiliu.core.config.internation.InternationUtils;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.common.ShopInfoRedisUtils;
import com.izhiliu.erp.service.discount.DiscountDetailService;
import com.izhiliu.erp.service.discount.DiscountItemService;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountDetailDTO;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemDTO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountItemQO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountPageQO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountQO;
import com.izhiliu.erp.web.rest.discount.vo.DiscountDeatilVO;
import com.izhiliu.erp.web.rest.discount.vo.DiscountItemVO;
import com.izhiliu.erp.web.rest.discount.vo.SyncResultVO;
import com.izhiliu.erp.web.rest.item.vm.TaskExecuteVO;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:52
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class ShopeeDiscountResouce {

    @Resource
    private DiscountDetailService discountDetailService;
    @Resource
    private DiscountItemService discountItemService;
    @Resource
    private ShopInfoRedisUtils shopInfoRedisUtils;

    /**
     * @apiGroup 折扣
     * @apiVersion 2.0.0
     * @api {GET} /shopee-discount/detail 1、分页查询shopee折扣活动详情
     * @apiDescription 条件查询折扣活动详情
     * @apiParam {Number} page
     * @apiParam {Number} size
     * @apiParam {String} discountId 活动id
     * @apiParam {String} likeDiscountName 活动名称
     * @apiParam {String[]} shopIds 店铺id
     * @apiParam {String[]} statuses 状态: expired:已过期; ongoing:进行中; upcoming:即将来临;
     * @apiParam {String} startTimeBegin 开始时间_开始 yyyy-MM-dd hh:ss:mm
     * @apiParam {String} startTimeEnd 开始时间_结束  yyyy-MM-dd hh:ss:mm
     * @apiParam {String} endTimeBegin 结束时间_开始   yyyy-MM-dd hh:ss:mm
     * @apiParam {String} endTime 结束时间_开始   yyyy-MM-dd hh:ss:mm
     * @apiParam {Number} shopeeItemId shopee产品id
     * @apiParam {String} shopeeItemName 商品名称
     * @apiParam {String[]} shopIds 店铺id
     * @apiSuccessExample response
     * HTTP/1.1 200
     * [
     *     {
     *         "login": "parent@izhiliu.com",
     *         "shopId": "141878703",
     *         "shopName": "優戀沫之家",
     *         "discountId": "1155764080",
     *         "discountName": "夏季专场特卖(女装)",
     *         "status": "upcoming",
     *         "startTime": "2020-08-12T00:00:00Z",
     *         "endTime": "2020-08-30T00:00:00Z",
     *         "itemCount": 1
     *     }
     * ]
     * @apiSuccess {String} discountId 折扣活动id
     * @apiSuccess {String} discountName 折扣活动名称
     * @apiSuccess {Number} itemCount 商品数量
     * @apiSuccess {String} status 状态: expired:已过期; ongoing:进行中; upcoming:即将来临;
     * @apiSuccess {Number} itemCount 商品数量
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @GetMapping("/shopee-discount/detail")
    public ResponseEntity<List<DiscountDeatilVO>> queryDiscountPage(DiscountPageQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        qo.setShopIds(shopInfoRedisUtils.getShopId(qo.getShopIds(), false));
        IPage<ShopeeDiscountDetailDTO> page = discountDetailService.queryDiscountDetailPage(qo);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "");
        List<DiscountDeatilVO> result = discountDetailService.conventTOVO(page.getRecords());
        return new ResponseEntity(result, headers, HttpStatus.OK);
    }

    /**
     * @apiGroup 折扣
     * @apiVersion 2.0.0
     * @api {GET} /shopee-discount/item/detail 2、分页查询shopee折扣活动商品
     * @apiDescription 条件查询折扣活动商品详情
     * @apiParam {Number} page
     * @apiParam {Number} size
     * @apiParam {String} discountId 活动id
     * @apiParam {String} shopIds 店铺id
     * @apiParam {Number} shopeeItemId shopee产品id
     * @apiParam {String} shopeeItemName 商品名称
     * @apiSuccessExample response
     * HTTP/1.1 200
     * [
     *     {
     *         "login":"parent@izhiliu.com",
     *         "discountId":"1161629365",
     *         "itemId":4944398203,
     *         "itemName":"法式泡泡袖格子襯衫女夏季2020年新款設計感小眾襯衣修身短袖上衣",
     *         "variationTier":0,
     *         "purchaseLimit":3,
     *         "itemOriginalPrice":135,
     *         "itemPromotionPrice":100,
     *         "stock":160,
     *         "variations":[
     *
     *         ]
     *     },
     *     {
     *         "login":"parent@izhiliu.com",
     *         "discountId":"1161629365",
     *         "itemId":7441234907,
     *         "itemName":"批发精品丝滑冰丝短袖T恤春夏新版女装V领上衣打底修身一件代发",
     *         "variationTier":2,
     *         "purchaseLimit":10,
     *         "itemOriginalPrice":0,
     *         "itemPromotionPrice":0,
     *         "stock":720,
     *         "images":[
     *              "https://s-cf-tw.shopeesz.com/file/3d61cf16b0835aa45b745797425ca434"
     *          ],
     *         "onlineUrl": "https://shopee.tw/实拍8172#半高领糖果色宽松水貂绒毛衣套头加厚马海毛针织打底衫-i.141878703.7047243577",
     *         "currency": "TWD"
     *         "variations":[
     *             {
     *                 "variationId":60215484038,
     *                 "variationName":"冰清藍,M（70-100斤）",
     *                 "variationOriginalPrice":190,
     *                 "variationPromotionPrice":170,
     *                 "variationStock":80,
     *                 "enable":true
     *             },
     *             {
     *                 "variationId":60215484039,
     *                 "variationName":"樹莓粉—L（100-130斤）",
     *                 "variationOriginalPrice":190,
     *                 "variationPromotionPrice":0,
     *                 "variationStock":80,
     *                 "enable":false
     *             }
     *         ]
     *     }
     * ]
     * @apiSuccess {String} discountId 折扣活动id
     * @apiSuccess {String} itemId 商品id
     * @apiSuccess {String} itemName 商品名
     * @apiSuccess {Number} variationTier 类型：0：单品; 2:sku
     * @apiSuccess {Number} itemOriginalPrice 原价(单品)
     * @apiSuccess {Number} itemPromotionPrice 折后价(单品)
     * @apiSuccess {Number} purchaseLimit 限购数量
     * @apiSuccess {String[]} images *商品图片
     * @apiSuccess {String} onlineUrl *在线链接
     * @apiSuccess {String} currency *货币
     * @apiSuccess {Object[]} variations sku信息
     * @apiSuccess {String} Object.variationId sku编码
     * @apiSuccess {String} Object.variationName sku名
     * @apiSuccess {Number} Object.variationOriginalPrice sku原价
     * @apiSuccess {Number} Object.variationPromotionPrice sku折后价
     * @apiSuccess {Number} Object.variationStock sku库存
     * @apiSuccess {Number} Object.enable 是否显示： true:显示; false:不显示;
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @GetMapping("/shopee-discount/item/detail")
    @Timed
    public ResponseEntity<List<DiscountItemVO>> queryItemPage(DiscountPageQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        IPage<ShopeeDiscountItemDTO> page = discountItemService.queryItemPage(qo);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "");
        List<DiscountItemVO> result = discountItemService.conventTOVO(page.getRecords());
        return new ResponseEntity(result, headers, HttpStatus.OK);
    }

    /**
     * @apiGroup 折扣
     * @apiVersion 2.0.0
     * @api {POST} /shopee-discount/sync 3、同步选中折扣
     * @apiDescription 同步选中折扣或缺失折扣
     * @apiParam {Object} syncParamList
     * @apiParam {String} Object.shopId 店铺id
     * @apiParam {String} Object.discountId 折扣活动id
     * @apiParamExample {json} 示例：
     * {
     *   "syncParamList":[
     *     {
     *       "shopId":141878703,
     *       "discountId":"1125635620"
     *     }]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     * {
     *     "taskId": "YWRtaW5AaXpoaWxpdS5jb20yQTMzMTc=",
     *     "count": 1
     * }
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/shopee-discount/sync")
    public ResponseEntity<SyncResultVO> syncShopeeDiscount(@RequestBody DiscountQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        return ResponseEntity.ok(discountDetailService.syncDiscountBatch(qo));
    }

    /**
     * @apiGroup 折扣
     * @apiVersion 2.0.0
     * @api {POST} /shopee-discount/sync-shop 4、同步店铺折扣活动
     * @apiDescription 同步店铺折扣活动
     * @apiParam {Object} syncParamList
     * @apiParam {String} Object.shopId 店铺id
     * @apiParam {String} Object.discountStatus 折扣状态:UPCOMING:即将到来; ONGOING:进行中; EXPIRED:已过期;  ALL:全部
     * @apiParamExample {json} 示例：
     * {
     *   "syncParamList":[
     *     {
     *       "shopId":141878703,
     *       "discountStatus":"ALL"
     *     }]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     * {
     *     "taskId": "cGFyZW50QGl6aGlsaXUuY29tQ0JBQTZE",
     *     "count": 32
     * }
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/shopee-discount/sync-shop")
    @Timed
    public ResponseEntity<SyncResultVO> syncShopShopeeDiscount(@RequestBody DiscountQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        return ResponseEntity.ok(discountDetailService.syncDiscountByShop(qo));
    }

    /**
     * @apiGroup 折扣
     * @apiVersion 2.0.0
     * @api {POST} /shopee-discount/detail 5、创建折扣活动详情
     * @apiDescription 创建折扣活动详情
     * @apiParam {String} shopId 店铺id
     * @apiParam {String} discountName 活动名称
     * @apiParam {String} startTime 开始时间: 格式 YYYY-MM-DD HH:mm:ss 不能小于当前时间
     * @apiParam {String} endTime 结束时间: 格式 YYYY-MM-DD HH:mm:ss
     * @apiParam {Object[]} items 折扣商品
     * @apiParam {String} Object.shopeeItemId 商品id
     * @apiParam {Number} Object.itemPromotionPrice 折后价格
     * @apiParam {Number} Object.purchaseLimit 限购数量
     * @apiParam {Number} Object.type 类型: add:新增 ;
     * @apiParam {Object[]} Object.variations 变种信息
     * @apiParam {String} Object.Object.variationId 变种id
     * @apiParam {String} Object.Object.variationPromotionPrice 变种折后价格
     * @apiParamExample {json} 示例：
     * {
     *   "shopId":"141878703",
     *   "discountName":"夏季特惠8.8专场特卖(女装)",
     *   "startTime":"2020-08-08 00:00:00",
     *   "endTime":"2020-08-30 00:00:00",
     *   "items":[
     *     {
     *       "shopeeItemId":"4944398203",
     *       "itemPromotionPrice":100,
     *       "purchaseLimit":3,
     *       "type":"add"
     *     }]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     * {
     *     "discountId": "1135702458"
     * }
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/shopee-discount/detail")
    @Timed
    public ResponseEntity<DiscountDeatilVO> createDiscount(@RequestBody @Validated DiscountQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        return ResponseEntity.ok(discountDetailService.createDiscountDetail(qo));
    }

    /**
     * @apiGroup 折扣
     * @apiVersion 2.0.0
     * @api {PUT} /shopee-discount/detail 6、修改折扣活动详情
     * @apiDescription 修改折扣活动详情, 僅可縮短折扣活動期間
     * @apiParam {String} discountId 活动id
     * @apiParam {String} discountName 活动名称
     * @apiParam {String} startTime 开始时间: 格式 YYYY-MM-DD HH:mm:ss
     * @apiParam {String} endTime 结束时间: 格式 YYYY-MM-DD HH:mm:ss
     * @apiParamExample {json} 示例：
     * {
     *   "discountId":"1155764080",
     *   "discountName":"夏季折扣",
     *   "startTime":"2020-08-13 08:00:00",
     *   "endTime":"2020-08-30 00:00:00"
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     * true
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PutMapping("/shopee-discount/detail")
    @Timed
    public ResponseEntity<Boolean> modifyDiscountDetail(@RequestBody @Validated DiscountQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        return ResponseEntity.ok(discountDetailService.modifyDisocuntDetail(qo));
    }

    /**
     * @apiGroup 折扣
     * @apiVersion 2.0.0
     * @api {POST} /shopee-discount/item 7、活动商品编辑(耗时过长，作废)
     * @apiDescription 涉及新增、编辑、删除，同一个单品或sku只允许一种状态
     * @apiParam {String} discountId 活动id
     * @apiParam {Object[]} items 折扣商品
     * @apiParam {String} Object.type 状态: add:新增; modify:修改; delete:删除;
     * @apiParam {String} Object.shopeeItemId 商品id
     * @apiParam {Number} Object.itemPromotionPrice 折后价格
     * @apiParam {Number} Object.purchaseLimit 限购数量
     * @apiParam {Object[]} Object.variations 变种信息
     * @apiParam {String} Object.Object.variationId 变种id
     * @apiParam {String} Object.Object.variationPromotionPrice 变种折后价格
     * @apiParamExample {json} 示例：
     * {
     *     "discountId":"1155764080",
     *     "items":[
     *         {
     *             "type":"modify",
     *             "purchaseLimit":2,
     *             "shopeeItemId":"4944398203",
     *             "variations":[
     *
     *             ],
     *             "itemPromotionPrice":"102"
     *         },
     *         {
     *             "type":"add",
     *             "purchaseLimit":2,
     *             "shopeeItemId":"4944398203",
     *             "variations":[
     *
     *             ],
     *             "itemPromotionPrice":"99.99"
     *         },
     *         {
     *             "type":"delete",
     *             "shopeeItemId":"4944398203"
     *         }
     *     ]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     * true
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/shopee-discount/item")
    @Timed
    @Deprecated
    public ResponseEntity<Boolean> handleDiscountItem(@RequestBody @Validated DiscountItemQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        qo.setLocale(InternationUtils.getLocale());
        return ResponseEntity.ok(discountItemService.handleDiscountItem(qo));
    }

    /**
     * @apiGroup 折扣
     * @apiVersion 2.0.0
     * @api {POST} /shopee-discount/item/v2 7.1、活动商品编辑 new+
     * @apiDescription 涉及新增、编辑、删除，同一个单品或sku只允许一种状态，异步,结果通过/lux/api/common/task/{taskId}获取结果
     * @apiParam {String} discountId 活动id
     * @apiParam {Object[]} items 折扣商品
     * @apiParam {String} Object.type 状态: add:新增; modify:修改; delete:删除;
     * @apiParam {String} Object.shopeeItemId 商品id
     * @apiParam {Number} Object.itemPromotionPrice 折后价格
     * @apiParam {Number} Object.purchaseLimit 限购数量
     * @apiParam {Object[]} Object.variations 变种信息
     * @apiParam {String} Object.Object.variationId 变种id
     * @apiParam {String} Object.Object.variationPromotionPrice 变种折后价格
     * @apiParamExample {json} 示例：
     * {
     *     "discountId":"1155764080",
     *     "items":[
     *         {
     *             "type":"modify",
     *             "purchaseLimit":2,
     *             "shopeeItemId":"4944398203",
     *             "variations":[
     *
     *             ],
     *             "itemPromotionPrice":"102"
     *         },
     *         {
     *             "type":"add",
     *             "purchaseLimit":2,
     *             "shopeeItemId":"4944398203",
     *             "variations":[
     *
     *             ],
     *             "itemPromotionPrice":"99.99"
     *         },
     *         {
     *             "type":"delete",
     *             "shopeeItemId":"4944398203"
     *         }
     *     ]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     * {
     *     "code": 0,
     *     "taskId": "cGFyZW50QGl6aGlsaXUuY29tQ0JFMUE1",
     *     "taskType": "discount_item_edit"
     * }
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/shopee-discount/item/v2")
    @Timed
    public ResponseEntity<TaskExecuteVO> handleDiscountItemV2(@RequestBody @Validated DiscountItemQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        qo.setTaskId(CommonUtils.getTaskId(qo.getLogin()));
        qo.setLocale(InternationUtils.getLocale());
        return ResponseEntity.ok(discountItemService.handleDiscountItemV2(qo));
    }

    /**
     * @apiGroup 折扣
     * @apiVersion 2.0.0
     * @api {DELETE} /shopee-discount/detail 8、删除折扣
     * @apiDescription 删除折扣
     * @apiParam {Object[]} paramList
     * @apiParam {String} Object.shopId 店铺id
     * @apiParam {String} Object.discountId 活动id
     * @apiParamExample {json} 示例：
     * {
     *     "discountIds":[
     *         "1155764080"
     *     ]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     * true
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @DeleteMapping("/shopee-discount/detail")
    @Timed
    public ResponseEntity<Boolean> deleteDiscountDetail(@RequestBody @Validated DiscountQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        return ResponseEntity.ok(discountDetailService.deleteDiscountDetail(qo));
    }


    /**
     * @apiGroup 折扣
     * @apiVersion 2.0.0
     * @api {PUT} /shopee-discount/detail/end 9、结束折扣
     * @apiDescription 结束折扣
     * @apiParam {String} discountId 活动id
     * @apiParamExample {json} 示例：
     * {
     *   "discountId":"1135679196"
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     * true
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PutMapping("/shopee-discount/detail/end")
    @Timed
    public ResponseEntity<Boolean> endDiscountDetail(@RequestBody @Validated DiscountQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        return ResponseEntity.ok(discountDetailService.endDiscount(qo));
    }



}
