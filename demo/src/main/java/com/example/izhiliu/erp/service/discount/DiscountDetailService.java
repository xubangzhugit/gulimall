package com.izhiliu.erp.service.discount;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.izhiliu.core.domain.common.BaseService;
import com.izhiliu.erp.domain.discount.ShopeeDiscountDetail;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountDetailDTO;
import com.izhiliu.erp.service.discount.mq.DiscountDetailMessageDTO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountPageQO;
import com.izhiliu.erp.web.rest.discount.qo.DiscountQO;
import com.izhiliu.erp.web.rest.discount.vo.DiscountDeatilVO;
import com.izhiliu.erp.web.rest.discount.vo.SyncResultVO;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:11
 */
public interface DiscountDetailService extends BaseService<ShopeeDiscountDetail, ShopeeDiscountDetailDTO> {


    /**
     * 创建折扣
     * @param qo
     * @return
     */
    DiscountDeatilVO createDiscountDetail(DiscountQO qo);

    /**
     * 同步选中折扣
     * @param qo
     * @return
     */
    SyncResultVO syncDiscountBatch(DiscountQO qo);

    /**
     * 同步店铺
     * @param qo
     * @return
     */
    SyncResultVO syncDiscountByShop(DiscountQO qo);

    /**
     * 同步折扣消费端
     * @param dto
     * @return
     */
    Boolean syncDiscountDetail(DiscountDetailMessageDTO dto);

    /**
     * 同步折扣详情
     * @param qo
     * @return
     */
    Boolean syncDiscountDetail(DiscountQO qo);

    /**
     * 分页查询订单详情
     * @param qo
     * @return
     */
    IPage<ShopeeDiscountDetailDTO> queryDiscountDetailPage(DiscountPageQO qo);

    /**
     * 封装查询结果
     * @param records
     * @return
     */
    List<DiscountDeatilVO> conventTOVO(List<ShopeeDiscountDetailDTO> records);

    /**
     * 修改折扣详情
     * @param qo
     * @return
     */
    Boolean modifyDisocuntDetail(DiscountQO qo);

    /**
     * 删除折扣
     * @param qo
     * @return
     */
    Boolean deleteDiscountDetail(DiscountQO qo);

    /**
     * 结束折扣
     * @param qo
     * @return
     */
    Boolean endDiscount(DiscountQO qo);
}
