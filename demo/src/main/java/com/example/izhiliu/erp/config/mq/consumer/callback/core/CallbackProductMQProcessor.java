package com.izhiliu.erp.config.mq.consumer.callback.core;

import com.izhiliu.config.BaseVariable;
import com.izhiliu.config.processor.CallbackMQProcessorVariable;
import com.izhiliu.erp.config.mq.consumer.callback.base.CallbacEntity;
import com.izhiliu.erp.config.mq.consumer.callback.CallbackBaseMQProcessor;
import com.izhiliu.erp.domain.item.ItemCategoryMap;
import com.izhiliu.erp.domain.item.ProductMetaData;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.repository.item.CategoryMapRepository;
import com.izhiliu.erp.repository.item.ProductMetaDataRepository;
import com.izhiliu.erp.repository.item.ShopeeProductRepository;
import com.izhiliu.erp.service.item.ShopeeBasicDataService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.mq.consumer.ConsumerObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;
import java.time.Instant;
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
public class CallbackProductMQProcessor extends CallbackBaseMQProcessor implements CallbackMQProcessorVariable.CallbackProductVariable {



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

    @Resource
    CategoryMapRepository categoryMapRepository;
    @Resource
    ProductMetaDataRepository metaDataRepository;
    @Resource
    ShopeeProductRepository shopeeProductRepository;

    @Resource
    ShopeeBasicDataService shopeeBasicDataService;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public void process(CallbacEntity callbacEntity) {
        //   save:productId:categoryId
        //   1:1231231:12312123
        final String key = callbacEntity.getKey();
        final String substringKey = key.substring(0, key.indexOf(":"));
        final ShopeeProductDTO shopeeProductDTO = callbacEntity.getData().toJavaObject(ShopeeProductDTO.class);
        //
        if (Objects.equals(substringKey, BaseVariable.CallbackProduct.RESET)) {
            reset(shopeeProductDTO, shopeeProductDTO.getShopeeCategoryId());
            return;
        }
        final ShopeeProduct shopeeProduct = shopeeProductRepository.selectById(shopeeProductDTO.getId());
        if (Objects.nonNull(shopeeProduct) && Objects.nonNull(shopeeProduct.getMetaDataId())) {
            final Optional<ProductMetaData> next = metaDataRepository.findById(shopeeProduct.getMetaDataId());

            if (next.isPresent()) {
                final ProductMetaData productMetaData = next.get();
                final Long categoryId = productMetaData.getCategoryId();
                if (Objects.nonNull(categoryId)) {
                    if (Objects.equals(substringKey, BaseVariable.CallbackProduct.SAVE_OR_UPADTE)) {
                        final ItemCategoryMap itemCategoryMap = categoryMapRepository.selectByObj(shopeeProductDTO.getPlatformId(), shopeeProductDTO.getPlatformNodeId(), categoryId, shopeeProductDTO.getShopeeCategoryId());

                        if (Objects.nonNull(itemCategoryMap)) {
                            categoryMapRepository.updateSuccessCountById(itemCategoryMap.getId());
                        } else {
                            save(shopeeProduct, productMetaData);
                        }
                    } else if (Objects.equals(substringKey, BaseVariable.CallbackProduct.DELETE)) {
                        deleteCategoryId(shopeeProductDTO,categoryId);
                    }
                }
            }

        }
    }

    void reset(ShopeeProductDTO shopeeProductDTO, Long categoryId) {
        shopeeBasicDataService.refreshAllAttributeByCategoryIdReCache(categoryId,shopeeProductDTO.getPlatformNodeId());
    }

    void deleteCategoryId(ShopeeProductDTO shopeeProductDTO, Long categoryId) {
        categoryMapRepository.deleteByObj(shopeeProductDTO.getPlatformId(),shopeeProductDTO.getPlatformNodeId(), categoryId, shopeeProductDTO.getShopeeCategoryId());
    }


    private void save(ShopeeProduct shopeeProduct, ProductMetaData next) {
        final Long categoryId = next.getCategoryId();
        final Long platformId = next.getPlatformId();
        final Long platformNodeId = next.getPlatformNodeId();
        final Long shopeeCategoryId = shopeeProduct.getShopeeCategoryId();
        final Long platformNodeId1 = shopeeProduct.getPlatformNodeId();
        final Long platformId1 = shopeeProduct.getPlatformId();
        ItemCategoryMap saveCategoryMap = new ItemCategoryMap()
                .setSrcCategroyId(categoryId)
                .setSrcPlatformId(platformId)
                .setSrcPlatfromNodeId(platformNodeId)

                .setDstCategroyId(shopeeCategoryId)
                .setDstPlatfromNodeId(platformNodeId1)
                .setDstPlatformId(platformId1)
                .setSuccessCount(1);
        saveCategoryMap.setGmtCreate(Instant.now());
        saveCategoryMap.setGmtModified(Instant.now());
        categoryMapRepository.insert(saveCategoryMap);

    }




    private void incr(String key) {
        stringRedisTemplate.opsForValue().increment(key, 1);
    }


    public static Map<String, Object> invokeMetHod(Object args) {
        return Arrays.stream(BeanUtils.getPropertyDescriptors(args.getClass()))
                .filter(pd -> !"class".equals(pd.getName()))
                .collect(HashMap::new,
                        (map, pd) -> map.put(pd.getName(), ReflectionUtils.invokeMethod(pd.getReadMethod(), args)),
                        HashMap::putAll);
    }

}


