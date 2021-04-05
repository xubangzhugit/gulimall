package com.izhiliu.erp.config.mq.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.izhiliu.config.BaseVariable;
import com.izhiliu.core.config.EnvironmentHelper;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.erp.service.item.dto.BoostMQItems;
import com.izhiliu.erp.service.item.impl.BatchBoostItemServiceImpl;
import com.izhiliu.erp.service.mq.consumer.MQProcessor;
import com.izhiliu.log.LockCloseable;
import com.izhiliu.log.LogConstant;
import com.izhiliu.log.LoggerOp;
import com.izhiliu.mq.RocketMQProcessor;
import com.izhiliu.mq.consumer.ConsumerObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static com.izhiliu.erp.service.item.impl.BatchBoostItemServiceImpl.BOOST_ITEM_MAX_SIZE;

/**
 * describe:
 * <p>
 *
 * @author Seriel
 * @date 2019/3/12 15:10
 */
@Service
@Slf4j
public class CycleBoostMQProcessor implements MQProcessor {

    public static final String DELIMITER = ":";

    @Resource
    EnvironmentHelper environmentHelper;

    public String getTopic() {
        return BaseVariable.CallbackProduct.TOPIC_TO_PRODUCT_CALLBACK;
    }

    @PostConstruct
    void init(){
        mqProcessorMap.put(environmentHelper.handleTag(getTag()),this);
    }

    @Override
    public ConsumerObject getConsumerObject() {
        return new ConsumerObject().setTopic(getTopic()).setTag(getTag()).setCid(getCid());
    }

    @Resource
    private BatchBoostItemServiceImpl cycleBoostService;

    @Resource
    protected RedisLockHelper redisLockHelper;

    protected String getCid() {
        return  BaseVariable.Boost.CID;
    }


    @Override
    public String getTag() {
        return BaseVariable.Boost.CYCLE_BOOST;
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public void doProcess(Message message, ConsumeContext contest) {
        final LoggerOp loggerOp = new LoggerOp().start().setKind(LogConstant.SHOPEE_PRODUCT).setType("create").setCode(LogConstant.BOOST);

        final BoostMQItems action = JSON.parseObject(new String(message.getBody()), BoostMQItems.class);
        String shopId = action.getShopId().toString();
        String boostMQItems = JSONObject.toJSONString(action);
        loggerOp.setLoginId(action.getLoginId());
        try {
            log.info(loggerOp.setMessage("[处理商品置顶任务任务] [{}:{}:{}]").toString(), shopId, boostMQItems);
            cycleBoostService.boostItem(action, BOOST_ITEM_MAX_SIZE);
            log.info(loggerOp.ok().setMessage("[处理商品置顶任务任务] [{}:{}:{}]").toString(), shopId, boostMQItems);
        } catch (Throwable e) {
            String errorMessage = e.getMessage();
            if(e instanceof NullPointerException){
                errorMessage = "System is busy Try again later or Contact customer service";
                loggerOp.setThrowableType(errorMessage);
            }
//            boostAction.fail(action.getProductId(), errorMessage, LocalProductStatus.PUSH_FAILURE);
            loggerOp.setMessage("[处理商品置顶任务任务 - 异常] [{"+shopId+"}:{"+boostMQItems+"}] ");
            log.error(loggerOp.error().toString(), e);
        }
    }


    @Override
    public LockCloseable getCloseable() {
        return null;
    }

    @Override
    public boolean isCloseable() {
        return false;
    }
}
