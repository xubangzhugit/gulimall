package com.izhiliu.erp.web.rest.item;

import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.service.item.KyyCategoryRelationService;
import com.izhiliu.erp.service.item.KyyCategoryService;
import com.izhiliu.erp.web.rest.item.param.KyyCategoryQO;
import com.izhiliu.erp.web.rest.item.param.KyyCategoryRelationQO;
import com.izhiliu.erp.web.rest.item.vm.KyyCategoryVO;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @author Twilight
 * @date 2021/1/18 16:40
 */
@RestController
@RequestMapping("/api")
public class KyyCategoryResource {

    @Resource
    private KyyCategoryService kyyCategoryService;
    @Resource
    private KyyCategoryRelationService kyyCategoryRelationService;

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {POST} /kyy/category a:添加和修改客优云类目
     * @apiDescription 添加和修改客优云类目
     * @apiParam {string} kyyCategoryId 修改的时候传,添加不传
     * @apiParam {string} categoryName 类目名*
     * @apiParam {number} categoryLevel 分类级别(1-一级分类 2-二级分类 3-三级分类)*
     * @apiParam {string} parentId 父分类id* 如果为一级分类传0
     * @apiSuccessExample response
     * HTTP/1.1 200
     * {
     *     "kyyCategoryId":"112233",
     *     "categoryName":"穿搭",
     *     "categoryLevel":1,
     *     "parentId":"0"
     * }
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/kyy/category")
    public ResponseEntity<KyyCategoryVO> saveKyyCategory(@RequestBody @Valid KyyCategoryQO qo){
        qo.setLogin(SecurityUtils.getCurrentLogin());
        return ResponseEntity.ok(kyyCategoryService.saveKyyCategory(qo));
    }

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {GET} /kyy/category b:查询客优云类目
     * @apiDescription 查询客优云类目
     * @apiParam {string} parentId 父分类id* 如果为一级分类传0
     * @apiSuccessExample response
     * HTTP/1.1 200
     * [
     *      {
     *          "kyyCategoryId":"112233",
     *          "categoryName":"穿搭",
     *          "categoryLevel":1,
     *          "parentId":"0",
     *          "categoryRank":0
     *      },
     *      {
     *          "kyyCategoryId":"445566",
     *          "categoryName":"餐具",
     *          "categoryLevel":1,
     *          "parentId":"0",
     *          "categoryRank":0
     *      }
     * ]
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @GetMapping("/kyy/category")
    public ResponseEntity<List<KyyCategoryVO>> getKyyCategory(@RequestParam("parentId") String parentId){
        String login = SecurityUtils.getCurrentLogin();
        return ResponseEntity.ok(kyyCategoryService.getKyyCategory(parentId, login));
    }

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {GET} /kyy/category/forebears/{productId} d:根据商品id查询客优云类目层级
     * @apiDescription 根据商品id查询客优云类目层级
     * @apiParam {string} id 客优云类目id
     * @apiSuccessExample response
     * HTTP/1.1 200
     * [
     *      {
     *          "kyyCategoryId":"112233",
     *          "categoryName":"穿搭",
     *          "categoryLevel":1,
     *          "parentId":"0"
     *      },
     *      {
     *          "kyyCategoryId":"445566",
     *          "categoryName":"裤子",
     *          "categoryLevel":2,
     *          "parentId":"112233"
     *      },
     *      {
     *     "kyyCategoryId":"778899",
     *     "categoryName":"九分裤",
     *     "categoryLevel":3,
     *     "parentId":"445566"
     *      }
     * ]
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @GetMapping("/kyy/category/forebears/{productId}")
    public ResponseEntity<List<KyyCategoryVO>> getKyyCategoryForebears(@PathVariable("productId") String productId){
        String login = SecurityUtils.getCurrentLogin();
        return ResponseEntity.ok(kyyCategoryService.getKyyCategoryForebears(productId, login));
    }

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {DELETE} /kyy/category c:删除客优云类目
     * @apiDescription 删除客优云类目
     * @apiParam {string[]} kyyCategoryIdList 客优云类目id*
     * @apiSuccessExample response
     * HTTP/1.1 200
     * true
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     * false
     */
    @DeleteMapping("/kyy/category")
    public ResponseEntity<Boolean> deleteKyyCategory(@RequestBody @Validated(KyyCategoryQO.DeleteKyyCategory.class) KyyCategoryQO qo){
        qo.setLogin(SecurityUtils.getCurrentLogin());
        return ResponseEntity.ok(kyyCategoryService.deleteKyyCategory(qo));
    }


    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {POST} /kyy/item/category e:商品添加和修改客优云分类
     * @apiDescription 商品添加和修改客优云分类
     * @apiParam {Object[]} productBeanList 请求对象*
     * @apiParamExample {json} 示例：
     * {
     *   "productBeanList":[
     *          {
     *              "productId":"1358455226121",
     *              "kyyCategoryId":"25456274542554"
     *         },
     *         {
     *              "productId":"1358455226121",
     *              "kyyCategoryId":"25456274542554"
     *         }
     *   ]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     * true
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     * false
     */
    @PostMapping("/kyy/item/category")
    public ResponseEntity<Boolean> itemAddKyyCategory(@RequestBody @Valid KyyCategoryRelationQO qo){
        qo.setLogin(SecurityUtils.getCurrentLogin());
        return ResponseEntity.ok(kyyCategoryRelationService.itemAddKyyCategory(qo));
    }
}
