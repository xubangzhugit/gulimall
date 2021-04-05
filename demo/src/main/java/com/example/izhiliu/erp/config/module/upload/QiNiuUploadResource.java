package com.izhiliu.erp.config.module.upload;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.izhiliu.erp.config.ApplicationProperties;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/5 18:44
 */
@Component
public class QiNiuUploadResource implements com.izhiliu.erp.common.module.upload.UploadResource {

    public static final String AREA = "http://cdn1.keyouyun.com/";

    /**
     * 过期时间
     */
    private static final Integer EXPIRE_DATE = 7200;

    private static final Logger log = LoggerFactory.getLogger(QiNiuUploadResource.class);

    @Autowired
    private ApplicationProperties properties;

    private UploadManager manager;
    private Auth auth;

    private static String token;
    private static AtomicLong startTime;

    @PostConstruct
    void init() {
        manager = new UploadManager(new Configuration(Zone.zone0()));
        auth = Auth.create(properties.getQiniu().getAccess(), properties.getQiniu().getSecret());
    }

    private void refreshToken() {
        log.info("[刷新Token]");
        startTime = new AtomicLong(System.currentTimeMillis());
        token = this.auth.uploadToken(properties.getQiniu().getBucket(), null, EXPIRE_DATE, null);
    }

    @Override
    public Map<String, Object> upload(String url, String key) {
        Map<String, Object> result = null;
        try {
            final String response = manager.put(HttpRequest.get(url)
                .execute().bodyBytes(), key, token()).bodyString();
            if (response.contains("key")) {
                result = JSON.parseObject(response, Map.class);
                result.put("result", true);
                log.info("[上传成功] - url: {}", url);
                log.info("[上传成功] - key: {}", result.get("key"));
            }
        } catch (QiniuException e) {
            log.error(e.getMessage());
            result = new HashMap<>((int) Math.ceil(1 / .75));
            result.put("result", false);
        }

        return result;
    }

    @Override
    public Map<String, Object> upload(File file, String key) {
        Map<String, Object> result = null;
        try {
            final String response = manager.put(file, key, token()).bodyString();
            if (response.contains("key")) {
                result = JSON.parseObject(response, Map.class);
                result.put("result", true);
                log.info("[上传成功] - url: {}", file.getName());
            }
        } catch (QiniuException e) {
            log.error(e.getMessage());
            result = new HashMap<>((int) Math.ceil(1 / .75));
            result.put("result", false);
        }

        return result;

    }

    @Override
    public Map<String, Object> overrideUpload(File file, String key) {
        Map<String, Object> result = null;
        try {
            // 修改全局上传策略为支持文件覆盖上传，方法执行结束后恢复全局上传策略为默认。
            final String token = this.auth.uploadToken(properties.getQiniu().getBucket(), key, EXPIRE_DATE, null);
            String response = manager.put(file, key, token).bodyString();
            if (response.contains("key")) {
                result = JSON.parseObject(response, Map.class);
                result.put("result", true);
                log.info("[上传成功] - fileName: {}", key);
            }
        } catch (Exception e) {
            log.error("[上传七牛云失败] course: " + e.getCause().toString());
            result = new HashMap<>((int) Math.ceil(1 / .75));
            result.put("result", false);
        } finally {
            refreshToken();
        }
        return result;
    }

    @Override
    public Map<String, Object> upload(byte[] data, String key) {
        Map<String, Object> result = null;
        try {
            final String response = manager.put(data, key, token()).bodyString();
            if (response.contains("key")) {
                result = JSON.parseObject(response, Map.class);
                result.put("result", true);
                log.info("[上传成功]");
            }
        } catch (QiniuException e) {
            log.error(e.getMessage());
            result = new HashMap<>((int) Math.ceil(1 / .75));
            result.put("result", false);
        }

        return result;

    }

    @Override
    public String token() {
        /*
         * 提取两百秒更新
         */
        if (token == null || System.currentTimeMillis() > (startTime.get() + (EXPIRE_DATE * 1000) - (200 * 1000))) {
            refreshToken();
        }
        return token;
    }
}
