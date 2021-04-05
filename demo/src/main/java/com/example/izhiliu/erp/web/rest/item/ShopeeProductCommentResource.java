package com.izhiliu.erp.web.rest.item;

import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.domain.item.ItemCommentDetail;
import com.izhiliu.erp.service.item.ShopeeProductCommentService;
import com.izhiliu.erp.service.item.dto.CommentCallbackDTO;
import com.izhiliu.erp.web.rest.discount.vo.SyncResultVO;
import com.izhiliu.erp.web.rest.item.param.CommentSyncQO;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

import javax.validation.Valid;
import java.util.List;

/**
 * @author Twilight
 * @date 2021/2/7 11:29
 */
@RestController
@RequestMapping("/api")
public class ShopeeProductCommentResource {

    @Resource
    private ShopeeProductCommentService shopeeProductCommentService;

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {POST} /product/sync/comment A：批量同步产品评论
     * @apiDescription 批量同步产品评论
     * @apiParam {Object[]} itemInfoList 商品id集合*
     * @apiParamExample {json} 示例：
     * {
     * "itemInfoList":[
     * {
     * "shopId":141878703,
     * "itemId":1125635620
     * }
     * ]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     * {
     * "task": "cGFyZW50QGl6aGlsaXUuY29tQUQ4MDJB"
     * }
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/product/sync/comment")
    public ResponseEntity<SyncResultVO> syncProductComment(@RequestBody @Valid CommentSyncQO commentSyncQO) {
        commentSyncQO.setLogin(SecurityUtils.currentLogin());
        return ResponseEntity.ok(shopeeProductCommentService.syncProductComment(commentSyncQO));
    }

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {POST} /product/sync/shop-comment B：同步店铺产品评论
     * @apiDescription 同步店铺产品评论
     * @apiParam {number[]} shopIdList 店铺id集合*
     * @apiParamExample {json} 示例：
     * {
     * "shopIdList":[123414,4415655,55741669]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     * {
     * "task": "cGFyZW50QGl6aGlsaXUuY29tQUQ4MDJB"
     * }
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/product/sync/shop-comment")
    public ResponseEntity<SyncResultVO> syncShopProductComment(@RequestBody @Validated(CommentSyncQO.SyncShopCheck.class) CommentSyncQO commentSyncQO) {
        commentSyncQO.setLogin(SecurityUtils.currentLogin());
        return ResponseEntity.ok(shopeeProductCommentService.syncShopProductComment(commentSyncQO));
    }

//    /**
//     * @apiGroup 商品
//     * @apiVersion 2.0.0
//     * @api {GET} /service/product/shop-comment B：根据订单ID获取商品评论信息
//     * @apiDescription 根据订单ID获取商品评论信息
//     * @apiParam {number[]} login 用户名*
//     * @apiParam {number[]} ordersn 订单集合*
//     * @apiParamExample {json} 示例：
//     * {
//     * "login":"cutterzy@qq.com"
//     * "ordersn":[201009UNV3W81C,200915SJ5FV4SP,210118KQD07V9B]
//     * }
//     * @apiSuccessExample response
//     * HTTP/1.1 200
//     * {
//     * "task": "cGFyZW50QGl6aGlsaXUuY29tQUQ4MDJB"
//     * }
//     * @apiErrorExample ErrorExample
//     * HTTP/1.1 500
//     */
    @GetMapping("/service/product/shop-comment")
    public ResponseEntity<List<ItemCommentDetail>> getProductComment(@RequestParam("login") String login, @RequestParam("ordersn") List<String> ordersn) {
        return ResponseEntity.ok(shopeeProductCommentService.getShopProductComment(login,ordersn));
    }

    /**
     * 评论成功后回调更新mongodb中的评论信息
     * @param callbackDTOS
     * @return
     */
    @PostMapping("/service/product/shop-comment/callback")
    public ResponseEntity<Boolean> callbackUpdateComment(@RequestBody List<CommentCallbackDTO> callbackDTOS) {
        return ResponseEntity.ok(shopeeProductCommentService.syncUpdateComment(callbackDTOS));
    }
}
