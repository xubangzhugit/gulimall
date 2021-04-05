package com.izhiliu.erp.service.module.metadata.basic;


import java.util.Optional;

/**
 * describe: 平台模型映射器
 * <p>
 *
 * @author cheng
 * @date 2019/1/16 14:46
 */
public interface ModelChannel {

    /**
     * 发布
     */
    boolean publish(Long productId, Long shopId, String loginId);
    /**
     *  发布支持扣费
     */
    boolean publishSupport(Long productId, Long shopId, String loginId);

    /**
     * 更新
     */
    boolean push(Long productId, Long shopId, String loginId);

    /**
     * 同步
     */
    boolean pullTask(Long itemId, Long shopId, String loginId, boolean batch,String taskId,Integer shopeeUpdateTime);

    /**
     * 拉取全部
     */
    boolean pull(Long shopId, String loginId);
}
