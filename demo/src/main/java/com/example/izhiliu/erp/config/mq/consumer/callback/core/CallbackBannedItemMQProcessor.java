package com.izhiliu.erp.config.mq.consumer.callback.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.izhiliu.config.BaseVariable;
import com.izhiliu.config.produer.CallbackMQProduerVariable;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.erp.config.mq.consumer.callback.base.CallbacEntity;
import com.izhiliu.erp.config.mq.consumer.callback.CallbackBaseMQProcessor;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.repository.item.ShopeeProductRepository;
import com.izhiliu.mq.consumer.ConsumerObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
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
public class CallbackBannedItemMQProcessor extends CallbackBaseMQProcessor implements CallbackMQProduerVariable.Tag.Banned {


    @Override
    public void setMyTopic(String topic) {
        super.setMyTopic(topic);
    }

    @Override
    public ConsumerObject getConsumerObject() {
        return new ConsumerObject().setTopic(getTopic()).setTag(getTag()).setCid(getCid());
    }

    protected String getCid() {
        return BaseVariable.CID.concat(getVariable());
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getTag() {
        return this.getTagVariable();
    }


    @Resource
    public ShopeeProductRepository shopeeProductRepository;


    @Override
    public void process(CallbacEntity callbacEntity) {
        final JSONObject data = callbacEntity.getData();
        final JSONObject itemInfo = data.getJSONObject("data");
        final Long itemId = itemInfo.getLong("item_id");

        ShopeeProduct shopeeProductDTO = shopeeProductRepository.selectByShopeeItemId(itemId);
        if (Objects.nonNull(shopeeProductDTO)) {
            final Long shopId = data.getLong("shopid");
            if (Objects.equals(shopId, shopeeProductDTO.getShopId())) {
                final JSONArray reasonList = itemInfo.getJSONArray("reason_list");
                StringBuilder stringBuilder = new StringBuilder();
                if(reasonList.isEmpty()){
                    stringBuilder.append("item banned\n");
                }else {
                    final JSONObject jsonObject = reasonList.getJSONObject(0);
                    stringBuilder.append("violation_type: 【");
                    stringBuilder.append(MapUtils.getString(jsonObject, "violation_type", "..."));

                    stringBuilder.append("】; violation_reason: 【");
                    stringBuilder.append(MapUtils.getString(jsonObject, "violation_reason", "..."));

                    stringBuilder.append("】;suggestion: 【");
                    stringBuilder.append(MapUtils.getString(jsonObject, "suggestion", "..."));

                    stringBuilder.append("】; days_to_fix: 【");
                    stringBuilder.append(MapUtils.getString(jsonObject, "days_to_fix", "..."));
                    stringBuilder.append("】  \n");
                }
                final String message = stringBuilder.toString();

                final String feature = shopeeProductDTO.getFeature();
                final Map<String, String> map;
                if (Objects.nonNull(feature)) {
                    map = JSONObject.parseObject(feature, Map.class);
                } else {
                    map = new HashMap<>();
                }
                map.put("error", message);
                final ShopeeProduct shopeeProduct = new ShopeeProduct().setShopeeItemStatus(ShopeeItemStatus.BANNED);
                shopeeProduct.setFeature(JSONObject.toJSONString(map));
                shopeeProduct.setId(shopeeProductDTO.getId());
                shopeeProductRepository.updateById(shopeeProduct);

            }
        }

    }


    public static Map<String, Object> invokeMetHod(Object args) {
        return Arrays.stream(BeanUtils.getPropertyDescriptors(args.getClass()))
                .filter(pd -> !"class".equals(pd.getName()))
                .collect(HashMap::new,
                        (map, pd) -> map.put(pd.getName(), ReflectionUtils.invokeMethod(pd.getReadMethod(), args)),
                        HashMap::putAll);
    }


}


