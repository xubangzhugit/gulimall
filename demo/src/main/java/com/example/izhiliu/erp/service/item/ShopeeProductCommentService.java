package com.izhiliu.erp.service.item;

import com.izhiliu.erp.domain.item.ItemCommentDetail;
import com.izhiliu.erp.service.discount.mq.CommentDetailDTO;
import com.izhiliu.erp.service.discount.mq.CommentSyncDTO;
import com.izhiliu.erp.service.item.dto.CommentCallbackDTO;
import com.izhiliu.erp.web.rest.discount.vo.SyncResultVO;
import com.izhiliu.erp.web.rest.item.param.CommentSyncQO;

import java.util.List;

/**
 * @author Twilight
 * @date 2021/2/7 11:29
 */
public interface ShopeeProductCommentService {

    /**
     * 批量同步产品评论
     * @param commentSyncQO
     * @return
     */
    SyncResultVO syncProductComment(CommentSyncQO commentSyncQO);

    /**
     * 同步店铺产品评论
     * @param commentSyncQO
     * @return
     */
    SyncResultVO syncShopProductComment(CommentSyncQO commentSyncQO);

    /**
     * 同步评论
     * @param dto
     * @return
     */
    boolean syncComment(CommentSyncDTO dto);

    /**
     * 处理评论数据
     * @param dto
     * @return
     */
    boolean handleCommentDetail(CommentDetailDTO dto);

    /**
     * 更具订单ID获取评论信息
     * @param ordersn
     * @return
     */
    List<ItemCommentDetail> getShopProductComment(String login, List<String> ordersn);

    /**
     * 评论成功后回调更新mongodb中的评论信息
     *
     * @param callbackDTOS@return
     */
    Boolean syncUpdateComment(List<CommentCallbackDTO> callbackDTOS);
}
