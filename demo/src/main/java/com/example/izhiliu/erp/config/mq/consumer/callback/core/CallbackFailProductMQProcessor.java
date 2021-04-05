package com.izhiliu.erp.config.mq.consumer.callback.core;

import com.alibaba.fastjson.JSON;
import com.izhiliu.config.processor.CallbackMQProcessorVariable;

import com.izhiliu.erp.config.aop.subscribe.SubLimitService;
import com.izhiliu.erp.config.mq.consumer.callback.base.CallbacEntity;
import com.izhiliu.erp.config.mq.consumer.callback.CallbackBaseMQProcessor;
import com.izhiliu.mq.consumer.ConsumerObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;
import java.util.*;


/**
 * describe: 前端采集shopee数据处理
 * <p>
 *
 * @author cheng
 * @date 2019/1/24 14:56
 */
@Service
@Slf4j
public class CallbackFailProductMQProcessor extends CallbackBaseMQProcessor implements CallbackMQProcessorVariable.CallbackFailProductVariable {


    @Resource
    SubLimitService subLimitService;

    @Override
    public ConsumerObject getConsumerObject() {
        return new ConsumerObject().setTopic(getTopic()).setTag(getTag()).setCid(getCid());
    }

    protected String getCid() {
        return this.getCidVariable();
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getTag() {
        return this.getTagVariable();
    }




    @Override
    public void process(CallbacEntity callbacEntity) {
        log.info(JSON.toJSONString(callbacEntity));
        final Integer size = callbacEntity.getData().getInteger("incr");
        final String key = callbacEntity.getKey();
        subLimitService.incr(key,size);
    }



    public static Map<String, Object> invokeMetHod(Object args) {
        return Arrays.stream(BeanUtils.getPropertyDescriptors(args.getClass()))
                .filter(pd -> !"class".equals(pd.getName()))
                .collect(HashMap::new,
                        (map, pd) -> map.put(pd.getName(), ReflectionUtils.invokeMethod(pd.getReadMethod(), args)),
                        HashMap::putAll);
    }

}


