package com.izhiliu.erp.web.rest.itemhistoricaldata;

import com.izhiliu.erp.service.itemhistoricaldata.ItemHistoricalDataService;
import com.izhiliu.erp.service.itemhistoricaldata.dto.ItemHistoricalDataDTO;
import com.izhiliu.erp.service.itemhistoricaldata.dto.ItemHistoricalDataStatusDTO;
import com.izhiliu.erp.web.rest.itemhistoricaldata.qo.ItemHistoricalDataQO;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Validated
@RequestMapping("/api")
@RestController
public class ItemHistoricalDataResource {

    @Resource
    private ItemHistoricalDataService itemHistoricalDataService;


    /**
     * @apiGroup HistoricalPricesAndSales
     * @apiVersion 2.0.0
     * @api {POST}  /historical-dynamic-prices-sales 保存历史动销价格和销量
     * @apiDescription 保存历史动销价格和销量
     * @apiParam    {String} itemId 商品ID
     * @apiParam    {String} shopId 店铺ID
     * @apiParam    {String} url    商品地址
     * @apiParam    {String} updateTime   更新时间(时间戳，精确到秒)
     * @apiParam    {number} minPrice   价格最低
     * @apiParam    {number} maxPrice   价格最高
     * @apiParam    {number} daySales   日销售量
     * @apiParam    {number} allSales   总销售量
     * @apiSuccessExample response
     * HTTP/1.1 200
     * true
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/historical-dynamic-prices-sales")
    public ResponseEntity<Boolean> preserveData(@RequestBody List<ItemHistoricalDataQO> qos){
        return ResponseEntity.ok(itemHistoricalDataService.preserveData(qos));
    }

    /**
     * @apiGroup HistoricalPricesAndSales
     * @apiVersion 2.0.0
     * @api {GET}  /historical-dynamic-prices-sales 获取历史动销价格和销量
     * @apiDescription 获取历史动销价格和销量
     * @apiParam    {String} itemId 商品ID(必选)
     * @apiParam    {String} shopId 店铺ID(必选)
     * @apiParam    {String} beginDateTime 开始时间（必选,时间戳，精确到秒。如：2020-08-18 14:44:00）
     * @apiParam    {String} endDateTime   结束时间（时间戳，精确到秒。如：2020-08-28 14:44:00，不选则默认为当前时间）
     * @apiSuccessExample response
     * HTTP/1.1 200
     * {
     *      itemId:商品id
     *      shopId:店铺id
     *      data:[{
     *          minPrice:  最小价格
     *          maxPrice:  最大价格
     *          daySales:  销售量
     *          gmtDate:   时间(精确到秒)
     *          updateTime: 修改时间（时间戳）
     *      },{
     *         minPrice:  最小价格
     *         maxPrice:  最大价格
     *         daySales:  销售量
     *         gmtDate:   时间(精确到秒)
     *         updateTime: 修改时间（时间戳）
     *      }]
     *      allSales:   总销售量
     *      url:        商品地址
     *      status:    商品状态码
     * },
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @GetMapping("/historical-dynamic-prices-sales")
    public ResponseEntity<ItemHistoricalDataDTO> getData(ItemHistoricalDataQO qo){
        return ResponseEntity.ok(itemHistoricalDataService.getData(qo));
    }

    /**
     * @apiGroup HistoricalPricesAndSales
     * @apiVersion 2.0.0
     * @api {PUT}  /historical-dynamic-prices-sales/status 根据删除状态删除历史价格分表
     * @apiDescription 根据删除状态删除历史价格分表，修改历史价格主表
     * @apiParam    {String} itemId 商品ID(必选)
     * @apiParam    {String} shopId 店铺ID(必选)
     * @apiParam    {String} status deleted(必选，目前只能传deleted)
     * @apiSuccessExample response
     * HTTP/1.1 200
     * true
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PutMapping("/historical-dynamic-prices-sales")
    public ResponseEntity<Boolean> modifyDeletedOrUpdate(@RequestBody List<ItemHistoricalDataQO> qos){
        return ResponseEntity.ok(itemHistoricalDataService.modifyDeletedOrUpdate(qos));
    }

    /**
     * @apiGroup HistoricalPricesAndSales
     * @apiVersion 2.0.0
     * @api {GET}  /historical-dynamic-prices-sales/untreated 获取当天未修改的商品数据
     * @apiParam    {number} size 指定条数(必选)
     * @apiDescription 获取当天未修改的商品数据
     * @apiSuccessExample response
     * HTTP/1.1 200
     * [
     *  {
     *     itemId:  商品id2
     *     shopId:  店铺id2
     *     url:     商品地址2
     *  },
     *  {
     *      itemId:  商品id2
     *      shopId:  店铺id2
     *      url:     商品地址2
     *  }
     * ]
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @GetMapping("/historical-dynamic-prices-sales/untreated")
    public ResponseEntity<List<ItemHistoricalDataStatusDTO>> getUntreatedData(int size){
        return ResponseEntity.ok(itemHistoricalDataService.getUntreatedData(size));
    }
}
