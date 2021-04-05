package com.izhiliu.erp.common.module.upload;

import java.io.File;
import java.util.Map;

/**
 * describe: 上传图片接口
 * <p>
 *
 * @author cheng
 * @date 2019/1/5 18:42
 */
public interface UploadResource {

    /**
     * 上传文件
     *
     * @param url 链接
     * @param key key
     * @return
     */
    Map<String, Object> upload(String url, String key);

    /**
     * 上传文件
     *
     * @param file 文件
     * @param key  key
     * @return
     */
    Map<String, Object> upload(File file, String key);

    /**
     * 覆盖上传文件（覆盖上传之后再次访问不会生效，需要刷新缓存，刷新缓存办法：携带随机 URL 参数刷新）
     * <p>
     * 刷新 CDN 缓存注意事项：如果携带之前没用过的参数访问，CDN 会强制回源站中取回最新的资源。如果该参数之前用过，拿到的是旧数据。
     * <p>
     * 实例：http://img2.keyouyun.com/test.img?v={当前时间戳}
     * <p>
     * modify by harry
     *
     * @param file 文件
     * @param key  key
     * @return
     */
    Map<String, Object> overrideUpload(File file, String key);

    Map<String, Object> upload(byte[] data, String key);

    String token();
}
