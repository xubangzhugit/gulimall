package com.izhiliu.erp.web.rest.item;

import com.izhiliu.core.common.ValidList;
import com.izhiliu.erp.service.item.ShopeeCopyProductService;
import com.izhiliu.erp.web.rest.item.param.CopyToShop;
import com.izhiliu.erp.web.rest.item.param.SaveToNode;
import com.izhiliu.erp.web.rest.item.param.SaveToShop;
import com.izhiliu.erp.web.rest.item.result.CopyShopProductResult;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * describe:
 * 虾皮拷贝商品接口
 *
 * <p>
 *
 * @author cheng
 * @date 2019/4/9 11:31
 */
@Validated
@RestController
@RequestMapping("/api")
public class ShopeeCopyProductResource {

    @Resource
    private ShopeeCopyProductService shopeeCopyProductService;

    /**
     * 将平台商品拷贝到站点
     *
     * document: https://www.yuque.com/izhiliu/hg4a8o/ur2962#554fc93a
     */
    @PostMapping("/shopee-copy-product/copyPlatformToNode")
    public ResponseEntity<List<String>> copyPlatformToNode(@RequestBody @Validated SaveToNode saveToNode) {
        return ResponseEntity.ok(shopeeCopyProductService.copyPlatformToNode(saveToNode).stream()
            .map(Objects::toString)
            .collect(Collectors.toList()));
    }

    /**
     * 将站点商品拷贝到店铺
     *
     * document: https://www.yuque.com/izhiliu/hg4a8o/ur2962#9b56d1ca
     */
    @PostMapping("/shopee-copy-product/copyNodeToShop")
    public ResponseEntity<Boolean> copyNodeToShop(@RequestBody @Validated ValidList<SaveToShop> saveToShops) {
        shopeeCopyProductService.copyNodeToShop(saveToShops);
        return ResponseEntity.ok(true);
    }

    /**
     * 将站点商品发布到店铺
     *
     * document: https://www.yuque.com/izhiliu/hg4a8o/ur2962#9b56d1ca
     */
    @PostMapping("/shopee-copy-product/publishNodeToShop")
    public ResponseEntity<Boolean> publishNodeToShop(@RequestBody @Validated ValidList<SaveToShop> saveToShops) {
        shopeeCopyProductService.publishNodeToShop(saveToShops);
        return ResponseEntity.ok(true);
    }

    /**
     * 拷贝店铺商品
     *
     * document: https://www.yuque.com/izhiliu/hg4a8o/vs8om1
     */
    @PostMapping("/shopee-copy-product/copyToShop")
    public ResponseEntity<List<CopyShopProductResult>> copyToShop(@RequestBody @Validated ValidList<CopyToShop> toShops) {
        return ResponseEntity.ok(shopeeCopyProductService.copyToShop(toShops));
    }
}
