package com.izhiliu.erp.service.module.metadata.basic;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/27 15:29
 */
public interface MetaDataMap<T, V> {

    V map(T[] t);

    /**
     *   获取类目信息
     * @param content  上下文
     * @return  类目id
     */
    Long getCategoryId(String content);
}
