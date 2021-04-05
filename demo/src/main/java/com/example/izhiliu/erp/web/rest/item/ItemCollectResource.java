package com.izhiliu.erp.web.rest.item;

import cn.hutool.core.util.ReUtil;
import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;
import com.aliyun.openservices.shade.com.alibaba.fastjson.JSONObject;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.service.mq.producer.MQProducerService;
import com.izhiliu.erp.web.rest.errors.InternalServerErrorException;
import com.izhiliu.erp.web.rest.item.vm.CollectResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.regex.Pattern;


/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/24 19:02
 */
@Controller
@RequestMapping("/api")
public class ItemCollectResource {

    private static final Logger log = LoggerFactory.getLogger(ItemCollectResource.class);

    private static final Pattern RE_OFFER_ID = Pattern.compile("https://detail.1688.com/offer/(\\d+?).html");

    @Resource
    MQProducerService mqProducerService;

    @PostMapping("/collect/forward/erp")
    @ResponseBody
    public CollectResponse erpDataFrowardAjax(@RequestBody String data) {
        return dataHandler(data);
    }

    private CollectResponse dataHandler(String data) {
        JSONObject jsonObject;
        try {
            jsonObject = JSON.parseObject(data);
        } catch (Exception e) {
            return CollectResponse.error(null, "[JSON解析报错] : [" + e.getMessage() + "]");
        }

        final Integer action = jsonObject.getInteger("action");
        String requestId = jsonObject.getString("requestId");

        String loginId = SecurityUtils.getCurrentUserLogin().orElseThrow(()->new InternalServerErrorException("please login KeyouyunERP", ""));
        jsonObject.put("loginId", loginId);

        if (ErpActionEnum.COLLECT_SHOPEE.code.equals(action)) {
            mqProducerService.sendMQ(ErpActionEnum.COLLECT_SHOPEE.tag,requestId, jsonObject);
            log.info("[采集 - Shopee] : {}:{}", loginId, requestId);

            return CollectResponse.ok(requestId);
        } else if (ErpActionEnum.COLLECT_ALIBABA.code.equals(action)) {
            if (StringUtils.isBlank(requestId)) {
                String offerId = ReUtil.getGroup1(RE_OFFER_ID, jsonObject.getString("collectUrl"));
                if (StringUtils.isNotBlank(offerId)) {
                    requestId = offerId;
                }
            }
            log.info("[采集 - 1688] : {}:{}", loginId, requestId);
            mqProducerService.sendMQ(ErpActionEnum.COLLECT_ALIBABA.tag,requestId,jsonObject);

            return CollectResponse.ok(requestId);
        } else if (ErpActionEnum.COLLECT_LAZADA.code.equals(action)) {
            mqProducerService.sendMQ(ErpActionEnum.COLLECT_LAZADA.tag,requestId, jsonObject);
            log.info("[采集 - 来赞达] : {}:{}", loginId, requestId);

            return CollectResponse.ok(requestId);
        } else if (ErpActionEnum.COLLECT_EXPRESS.code.equals(action)) {
            mqProducerService.sendMQ(ErpActionEnum.COLLECT_EXPRESS.tag, requestId, jsonObject);
            log.info("[采集 - 速卖通] : {}:{}", loginId, requestId);

            return CollectResponse.ok(requestId);
        } else if (ErpActionEnum.COLLECT_TB.code.equals(action)) {
            mqProducerService.sendMQ(ErpActionEnum.COLLECT_TB.tag,requestId, jsonObject);
            log.info("[采集 - 淘宝] : {}:{}", loginId, requestId);

            return CollectResponse.ok(requestId);
        } else if (ErpActionEnum.COLLECT_TM.code.equals(action)) {
            mqProducerService.sendMQ(ErpActionEnum.COLLECT_TM.tag, requestId, jsonObject);
            log.info("[采集 - 天猫] : {}:{}", loginId, requestId);

            return CollectResponse.ok(requestId);
        }
        else if (ErpActionEnum.COLLECT_PDD.code.equals(action)) {
            mqProducerService.sendMQ(ErpActionEnum.COLLECT_PDD.tag,requestId, jsonObject);
            log.info("[采集 - 拼多多] : {}:{}", loginId, requestId);

            return CollectResponse.ok(requestId);
        }else if(ErpActionEnum.COLLECT_E7.code.equals(action)) {
            mqProducerService.sendMQ(ErpActionEnum.COLLECT_E7.tag, requestId, jsonObject);
            log.info("[采集 - 17网] : {}:{}", loginId, requestId);
            return CollectResponse.ok(requestId);
        }

        return CollectResponse.error(requestId, "[Action 无法识别] : " + action);
    }
}
