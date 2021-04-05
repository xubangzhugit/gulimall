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
 * @date 2021/2/7 17:22
 */

@Component
@Slf4j
public class CommentDetailConsumer implements BaseMQProcessor {

    public static final String TAG = "COMMENT_DETAIL_TAG";

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
        CommentDetailDTO dto = JSON.parseObject(new String(message.getBody()), CommentDetailDTO.class);
        log.info("同步折扣,参数对象:" + dto);
        return shopeeProductCommentService.handleCommentDetail(dto);
    }
}
