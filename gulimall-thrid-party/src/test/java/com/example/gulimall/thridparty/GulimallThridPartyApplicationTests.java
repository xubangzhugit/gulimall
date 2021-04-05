package com.example.gulimall.thridparty;

import com.aliyun.oss.OSS;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class GulimallThridPartyApplicationTests {

    @Test
    void contextLoads() {
    }
    /**
     * 文件上传测试
     * @throws FileNotFoundException
     */
//    @Test
//    public  void uploadFile() throws FileNotFoundException {
//// yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-hangzhou.aliyuncs.com。
//        String endpoint = "oss-cn-shenzhen.aliyuncs.com";
//// 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
//        String accessKeyId = "LTAI4G9Vnxr9gR8eUwt2EyUk";
//        String accessKeySecret = "Aejv6j7rqiKLCiO8i1DuwozR2yuJxN";
//
//// 创建OSSClient实例。
//        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//
//// 填写本地文件的完整路径。如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件流。
//        InputStream inputStream = new FileInputStream("D:\\软件下载\\360\\360zip\\config\\zcomment\\skin\\skin4.jpg");
//// 填写Bucket名称和Object完整路径。Object完整路径中不能包含Bucket名称。
//        ossClient.putObject("gulimall-xubangzhu", "Skin4.jpg", inputStream);
//
//// 关闭OSSClient。
//        ossClient.shutdown();
//    }
}
