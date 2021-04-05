package com.izhiliu.erp.service.discount.mq;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.erp.service.item.ShopeeProductCommentService;
import com.izhiliu.erp.service.mq.consumer.BaseMQProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author Twilight
 * @date 2021/2/7 16:07
 */
@Component
@Slf4j
public class CommentSyncConsumer implements BaseMQProcessor {

    public static final String TAG = "COMMENT_SYNC_TAG";

    @Resource
    private EnvironmentHelper environmentHelper;
    @Resource
    private ShopeeProductCommentService shopeeProductCommentService;

    @Override
    public String getTag() {
        return TAG;
    }

    @PostConstruct
    void init(){
        mqProcessorMap.put(environmentHelper.handleTag(getTag()),this);
    }


    @Override
    public boolean process(Message message, ConsumeContext contest) {
        CommentSyncDTO dto = JSON.parseObject(new String(message.getBody()), CommentSyncDTO.class);
        log.info("同步商品评论,参数对象:" + dto);
        return shopeeProductCommentService.syncComment(dto);
    }
}
