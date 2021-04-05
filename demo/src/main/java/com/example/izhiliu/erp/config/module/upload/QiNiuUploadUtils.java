package com.izhiliu.erp.config.module.upload;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.izhiliu.erp.config.ApplicationProperties;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.BatchStatus;
import com.qiniu.util.Auth;
import jdk.nashorn.internal.runtime.regexp.joni.Region;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 七牛云 文件上传
 *
 * @author Seriel
 * @create 2019-08-30 11:45
 **/
@Slf4j
@Component
public class QiNiuUploadUtils  {

    public static final String AREA = "http://cdn1.keyouyun.com/";
    /**
     * 过期时间
     */
    private static final Integer EXPIRE_DATE = 7200;


    @Autowired
    private ApplicationProperties properties;

    private static UploadManager uploadManager;
    private static BucketManager bucketManager;
    private static Auth auth;

    private static String token;
    private static AtomicLong startTime;


    public QiNiuUploadUtils(ApplicationContext applicationContext) {
        this.properties = applicationContext.getBean(ApplicationProperties.class);
        init();
    }



   private void init() {
       auth = Auth.create(properties.getQiniu().getAccess(), properties.getQiniu().getSecret());
       uploadManager = new UploadManager(new Configuration(Zone.zone0()));
       bucketManager = new BucketManager(auth,new Configuration(Zone.zone0()));
    }

    private void refreshToken() {
        log.info("[刷新Token]");
        startTime = new AtomicLong(System.currentTimeMillis());
        token = this.auth.uploadToken(properties.getQiniu().getBucket(), null, EXPIRE_DATE, null);
    }

    public String token() {
        /*
         * 提取两百秒更新
         */
        if (token == null || System.currentTimeMillis() > (startTime.get() + (EXPIRE_DATE * 1000) - (200 * 1000))) {
            refreshToken();
        }
        return token;
    }


    public static void delete(List<String> strings) {
        //单次批量请求的文件数量不得超过1000
        try {
            final BucketManager.Batch batch = new BucketManager.Batch();
            batch.delete("cdn1",strings.toArray(new String[]{}));
            Response response = bucketManager.batch(batch);
            BatchStatus[] batchStatusList = response.jsonToObject(BatchStatus[].class);
            for (int i = 0; i < strings.size(); i++) {
                BatchStatus status = batchStatusList[i];
                String key = strings.get(i);
            }
        } catch (QiniuException e) {
            log.error(e.getMessage());
        }

    }



}
