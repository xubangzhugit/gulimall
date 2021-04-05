package com.izhiliu.erp.service.item.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.erp.common.TaskExecutorUtils;
import com.izhiliu.erp.config.ApplicationProperties;
import com.izhiliu.erp.domain.item.UserImage;
import com.izhiliu.erp.repository.item.UserImageRepository;
import com.izhiliu.erp.service.item.UserImageService;
import com.izhiliu.erp.service.item.dto.UserImageDTO;
import com.izhiliu.erp.service.item.mapper.UserImageMapper;
import com.izhiliu.erp.web.rest.item.param.BatchDownloadImageQO;
import com.izhiliu.open.shopee.open.sdk.util.CommonUtils;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service Implementation for managing ShopeeProductImage.
 */
@Service
public class UserImageServiceImpl extends IBaseServiceImpl<UserImage, UserImageDTO, UserImageRepository, UserImageMapper> implements UserImageService {

    private final Logger log = LoggerFactory.getLogger(UserImageServiceImpl.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ApplicationProperties applicationProperties;
    @Resource
    private TaskExecutorUtils taskExecutorUtils;

    private static final int CORE_POOL_SIZE = 10;
    private static final int MAX_POOL_SIZE = 20;
    private static final int QUEUE_CAPACITY = 50;
    private static final Long KEEP_ALIVE_TIME = 1L;

    ThreadPoolExecutor executor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactoryBuilder()
                    .setNameFormat("userImage").build());

    @Override
    public Boolean batchDownloadImage(BatchDownloadImageQO qo) {
        final String taskId = qo.getTaskId();
        taskExecutorUtils.initSyncTask(taskId, (long)qo.getUrl().size(), null);
        executor.execute(()-> downloadImage(qo));
        return true;
    }

    private boolean downloadImage(BatchDownloadImageQO qo) {
        final String fileName = generateId();
        final List<String> files = qo.getUrl();
        final String taskId = qo.getTaskId();

        File zipFile = new File(fileName);
        // 判断压缩后的文件存在不，不存在则创建
        if (!zipFile.exists()) {
            try {
                zipFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 创建 FileOutputStream 对象
        FileOutputStream fileOutputStream = null;
        // 创建 ZipOutputStream
        ZipOutputStream zipOutputStream = null;
        // 创建 FileInputStream 对象
        InputStream inputStream = null;
        try {
            // 实例化 FileOutputStream 对象
            fileOutputStream = new FileOutputStream(zipFile);
            // 实例化 ZipOutputStream 对象
            zipOutputStream = new ZipOutputStream(fileOutputStream);
            // 创建 ZipEntry 对象
            ZipEntry zipEntry = null;
            // 遍历源文件数组
            for (int i = 0; i < files.size(); i++) {
                URL url = new URL(files.get(i));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty( "Content-Type", "application/json");
                conn.setRequestProperty( "Content-Encoding", "utf-8");
                conn.addRequestProperty("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36");
                conn.setRequestMethod("GET");
                conn.connect();
                // 将源文件数组中的当前文件读入 InputStream 流中
                try {
                    inputStream = conn.getInputStream();
                }catch (IOException e){
                    taskExecutorUtils.incrementSyncHash(taskId, "fail", 1);
                    continue;
                }
                // 实例化 ZipEntry 对象，源文件数组中的当前文件
                zipEntry = new ZipEntry(i+".jpg");
                zipOutputStream.putNextEntry(zipEntry);
                // 该变量记录每次真正读的字节个数
                int len;
                // 定义每次读取的字节数组
                byte[] buffer = new byte[1024];
                while ((len = inputStream.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, len);
                }
                taskExecutorUtils.incrementSyncHash(taskId, "success", 1);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                zipOutputStream.closeEntry();
                zipOutputStream.close();
                inputStream.close();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        //上传七牛云
        //构造一个带指定Region对象的配置类
        Configuration cfg = new Configuration(Zone.zone0());
        UploadManager uploadManager = new UploadManager(cfg);
        //...生成上传凭证，然后准备上传
        ApplicationProperties.Qiniu qiniu = applicationProperties.getQiniu();
        String accessKey = qiniu.getAccess();
        String secretKey = qiniu.getSecret();
        String bucket = qiniu.getBucket();


        //默认不指定key的情况下，以文件内容的hash值作为文件名
        String key = "archive/" + LocalDate.now().getYear() + "/" + generateId() + ".zip";
        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);
        try {
            Response response = uploadManager.put(fileName, key, upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            stringRedisTemplate.boundHashOps(taskId).put("key", putRet.key);
            log.info("上传压缩包到七牛云成功,key:{},hash:{}", putRet.key, putRet.hash);
        } catch (QiniuException ex) {
            stringRedisTemplate.boundHashOps(taskId).put("key", "0");
            Response r = ex.response;
            log.error("上传压缩包到七牛云失败,response:{}", r.toString());
            try {
                log.error("上传压缩包到七牛云失败,body:{}", r.bodyString());
            } catch (QiniuException ex2) {
                //ignore
            }
        }
        zipFile.delete();
        return true;
    }

    /**
     * 生成32位随机字符串
     * @return
     */
    public static String generateId() {
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString();
        id = id.replaceAll("-", "");
        return id;
    }
}
