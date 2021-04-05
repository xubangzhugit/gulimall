package com.izhiliu.erp.service.discount;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.izhiliu.core.domain.common.BaseService;
import com.izhiliu.erp.domain.discount.ShopeeDiscountItem;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemDTO;
import com.izhiliu.erp.service.discount.mq.DiscountItemMessageDTO;
import com.izhiliu.erp.service.discount.mq.DiscountItemPushMessageDTO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountItemCount;
import com.izhiliu.erp.web.rest.discount.qo.DiscountItemQO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountPageQO;
import com.izhiliu.erp.web.rest.discount.vo.DiscountItemVO;
import com.izhiliu.erp.web.rest.item.vm.TaskExecuteVO;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:11
 */
public interface DiscountItemService extends BaseService<ShopeeDiscountItem, ShopeeDiscountItemDTO> {


    /**
     * 更新折扣商品
     * @param qo
     * @return
     */
    Boolean handleDiscountItem(DiscountItemQO qo);


    /**
     * 更新折扣商品 v2
     * @param qo
     * @return
     */
    TaskExecuteVO handleDiscountItemV2(DiscountItemQO qo);

    /**
     * 同步更新 折扣商品
     * @param qo
     * @param itemList
     * @return
     */
    Boolean syncUpdateDiscountItem(DiscountItemQO qo);


    /**
     * 通过折扣id获取折扣商品
     * @param login
     * @param discountId
     * @return
     */
    List<ShopeeDiscountItemDTO> getListByDiscountId(String login, String discountId);

    /**
     * 通过api修改折扣商品
     * @param qo
     */
    void callModifyDiscountItemApi(DiscountItemQO qo);

    /**
     * 通过api增加折扣商品
     * @param qo
     */
    void callAddDiscountItemApi(DiscountItemQO qo);

    /**
     * 通过api删除折扣商品
     * @param qo
     */
    void callDeleteDiscountItemApi(DiscountItemQO qo);


    /**
     * 条件获取折扣商品
     * @param qo
     * @return
     */
    List<ShopeeDiscountItemDTO> listByCondition(DiscountPageQO qo);

    /**
     * 获取折扣商品数量统计
     * @param login
     * @param discountIds
     * @return
     */
    List<DiscountItemCount> getItemCount(String login, List<String> discountIds);

    /**
     * 分页查询折扣商品分页
     * @param qo
     * @return
     */
    IPage<ShopeeDiscountItemDTO> queryItemPage(DiscountPageQO qo);

    /**
     * 数据格式转换
     * @param records
     * @return
     */
    List<DiscountItemVO> conventTOVO(List<ShopeeDiscountItemDTO> records);

    /**
     * 通过折扣id批量删除
     * @param login
     * @param discountIds
     * @return
     */
    Boolean deleteByDiscountIds(String login, List<String> discountIds);

    /**
     * 消费端同步在线商品的折扣
     * @param dto
     * @return
     */
    boolean syncProductSku(DiscountItemMessageDTO dto);


    /**
     * 发布商品选择折扣，需要同步折扣商品表
     * @param dto
     * @return
     */
    Boolean publishToSyncDiscount(DiscountItemPushMessageDTO dto);
}


