package com.example.gulimall.search;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.ToString;
import net.bytebuddy.build.ToStringPlugin;
import org.apache.commons.collections.bag.SynchronizedSortedBag;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

@SpringBootTest
public class GulimallSearchApplicationTests {

    @Resource
    private RestHighLevelClient restHighLevelClient;
    private static final RequestOptions COMMON_OPTIONS;
    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
//        builder.addHeader("Authorization", "Bearer " + TOKEN);
//        builder.setHttpAsyncResponseConsumerFactory(
//                new HttpAsyncResponseConsumerFactory
//                        .HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }
    @Test
    void contextLoads() {
    }
    @ToString
    @Data
    static class brand
    {
        private int account_number;

        private int balance;

        private String firstname;

        private String lastname;

        private int age;

        private String gender;

        private String address;

        private String employer;

        private String email;

        private String city;

        private String state;

    }

    /**
     * 测试检索数据
     */
    @Test
    public void testSearchEs(){
        //创建检索
        SearchRequest searchRequest = new SearchRequest();
        //设置index
        searchRequest.indices("brand");
        //指定检索条件
        //添加聚合条件参照 https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-search.html
        SearchSourceBuilder size = new SearchSourceBuilder().from(0).size(3);
        size.query(QueryBuilders.matchQuery("address","Mill"));
        //添加聚合条件
        // size.aggregation(AggregationBuilders.terms("").field("").size())
        searchRequest.source(size);
        try {
            //执行检索
            SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //分析结果
            System.out.println("search"+search);
            //遍历聚合信息
            Aggregations aggregations =
                    search.getAggregations();
            Terms addaggs =  aggregations.get("addaggs");
            addaggs.getBuckets().stream().forEach((bucker)->{
                Number keyAsNumber = ((Terms.Bucket) bucker).getKeyAsNumber();
                long docCount = ((Terms.Bucket) bucker).getDocCount();
            });
            //遍历source信息
            SearchHit[] hits = search.getHits().getHits();
            Arrays.asList(hits).stream().forEach((hit)->{
               System.out.println( hit.getId());
                String sourceAsString = hit.getSourceAsString();
                brand brand = JSON.parseObject(sourceAsString, brand.class);
                System.out.println(brand);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 测试存储数据到es
     */
    @Test
    public void testIndexEs(){
        IndexRequest indexRequest = new IndexRequest("testES");//设置一个index
        indexRequest.id("1");  //设置数据的id
        //方式1 ：
        // indexRequest.source("username","xubangzhu","age","28","gender","FEIMAL");
        //方式2 ：
        user user = new user();
        String s = JSON.toJSONString(user);
        indexRequest.source(s, XContentType.JSON);

        try {
            IndexResponse index = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //异步操作
        restHighLevelClient.indexAsync(indexRequest, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
                //执行成功
               Consumer coms = System.out::println;
               coms.accept(indexResponse.getResult());
            }

            @Override
            public void onFailure(Exception e) {
                //执行失败
                Consumer coms = System.out::println;
                coms.accept(e.getMessage());
            }
        });
    }
    @Data
    class user{
        private String username;
        private Integer age;
        private String gender;
    }
}
