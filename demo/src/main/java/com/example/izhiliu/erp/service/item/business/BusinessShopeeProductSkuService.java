package com.izhiliu.erp.service.item.business;


import com.alibaba.fastjson.JSONObject;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.log.LogConstant;
import com.izhiliu.erp.log.LoggerOp;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.impl.CoverShopeeProductSkuService;
import com.izhiliu.erp.service.item.impl.ShopeeProductSkuServiceImpl;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.vm.VariationVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Service Implementation for managing ShopeeProductSku.
 */
@Service
public class BusinessShopeeProductSkuService {

    public final static String currentVersion = "v2";


    private final Logger log = LoggerFactory.getLogger(BusinessShopeeProductSkuService.class);


    @Resource
    ShopeeProductSkuServiceImpl shopeeProductSkuService;

    @Resource
    CoverShopeeProductSkuService coverShopeeProductSkuService;

    LoggerOp getLoggerOpObejct() {
        return new LoggerOp().start().setKind(LogConstant.SHOPEE_PRODUCT).setType(LogConstant.PUT).setCode(LogConstant.SKU);
    }

    /**
     * 删除已删除的SKU
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void coverTheProduct(VariationVM param, String login) {
        final LoggerOp loggerOpObejct = getLoggerOpObejct();
        loggerOpObejct.setLoginId(login);
        try {
            log.info(loggerOpObejct.setMessage("coverTheProduct").toString());
            coverCheck(param);
            if (isCurrentVersion(param)) {
//                coverShopeeProductSkuService.coverTheProduct(param);
                //仅更新表数据，不推送到shopee
                coverShopeeProductSkuService.coverTheProductForDB(param);
            }else{
                shopeeProductSkuService.coverTheProduct(param);
            }
        } catch (Throwable e) {
            log.error(loggerOpObejct.error().setMessage(" put coverTheProduct error  param "+ JSONObject.toJSONString(param)).toString(),e );
            throw  e;
        }
        log.info(loggerOpObejct.ok().setMessage("coverTheProduct").toString());
    }
    private void coverCheck(VariationVM param) {
        /**
         * 规格图要与规格对应上
         */
        if (CommonUtils.isNotBlank(param.getVariations())
                && !param.getVariations().stream()
                .filter(e -> CommonUtils.isNotBlank(e.getImageUrls()) && CommonUtils.isNotBlank(e.getOptions()))
                .allMatch(e -> e.getImageUrls().size() == e.getOptions().size())) {
            throw new LuxServerErrorException("规格图不能为空");
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void coverTheProductForBatch(VariationVM param, String login) {
        final LoggerOp loggerOpObejct = getLoggerOpObejct();
        loggerOpObejct.setLoginId(login);
        try {
            log.info(loggerOpObejct.setMessage("coverTheProduct").toString());
            if (isCurrentVersion(param)) {
                coverShopeeProductSkuService.coverTheProductForBatch(param);
            }else{
                shopeeProductSkuService.coverTheProduct(param);
            }
        } catch (Throwable e) {
            log.error(loggerOpObejct.error().setMessage(" put coverTheProduct error  param "+ JSONObject.toJSONString(param)).toString(),e );
            throw  e;
        }
        log.info(loggerOpObejct.ok().setMessage("coverTheProduct").toString());
    }

    private boolean isCurrentVersion(VariationVM param) {
        return Objects.nonNull(param.getVersion())&&param.getVersion();
    }


    @Transactional(rollbackFor = Exception.class)
    public void coverByProductSource(VariationVM param, ShopeeProductDTO product) {
        final List<VariationVM.Variation> variations = param.getVariations();
       if(isCurrentVersion(param)){
           if (ShopeeProduct.VariationTier.ZERO.val.equals(param.getVariationTier())) {
               shopeeProductSkuService.oneOrNoSkuAttribute(param, variations, product.getCurrency());
           } else {
               coverShopeeProductSkuService.towSkuAttribute(param, variations,product.getCurrency());
           }
       }else{
           if (ShopeeProduct.VariationTier.TWO.val.equals(param.getVariationTier())) {
               shopeeProductSkuService.towSkuAttribute(param, variations);
           } else {
               shopeeProductSkuService.oneOrNoSkuAttribute(param, variations, product.getCurrency());
           }
       }
        shopeeProductSkuService.insertVariationTier(param, product);
    }


    /**
     * 参数校验
     */
    public ShopeeProductDTO checkParam(VariationVM param,  Optional<ShopeeProductDTO> productSupplier) {
        if (isCurrentVersion(param)) {
            return coverShopeeProductSkuService.checkParam(param, productSupplier);
        } else {
            return shopeeProductSkuService.checkParam(param, productSupplier);
        }
    }

    public VariationVM variationByProduct(long productId, Boolean isActualStock){
            return coverShopeeProductSkuService.variationByProduct(productId,isActualStock);
    }

}
