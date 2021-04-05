package com.izhiliu.erp.web.rest.pricing;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.domain.pricing.PricingStrategies;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;
import com.izhiliu.erp.service.discount.PricingStrategiesService;
import com.izhiliu.erp.service.discount.dto.PricingStrategiesDto;
import com.izhiliu.erp.web.rest.AbstractController;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RequestMapping("/api/pricing-strategies")
@RestController()
public class PricingStrategiesResource extends AbstractController<PricingStrategies, PricingStrategiesDto, PricingStrategiesService> {

    /**
     *    获取 当前 用户当前站点 的 集合
     * @return
     */
    @GetMapping("/list")
    public ResponseEntity<IPage<PricingStrategiesDto>> listByplatform(@RequestParam("platformId") Integer platformId, @RequestParam("platformNodeId") Integer platformNodeId){
        return  ResponseEntity.ok(iBaseService.selectListByplatform(platformId,platformNodeId));
    }


    @Override
    public String info() {
           return getClass().getName();
    }


    @Override
    public PricingStrategiesDto checke(Long id) {
       return iBaseService.checke(id);
    }


    @PostMapping("")
    @Override
    public ResponseEntity save(@RequestBody @Validated PricingStrategiesDto aDto){
        if (log.isDebugEnabled()) {
            log.debug("REST request to save {} : {}", info(), aDto);
        }
        if (aDto.getId() != null) {
            throw new BadRequestAlertException("A new objcte cannot already have an ID", info(), "bad.request.alert.exception.create.shopee.product.sku.idexists");
        }
        final String login = SecurityUtils.currentLogin();
        return   this.iBaseService.save(aDto,login);
    }

    @GetMapping("/platformNodes")
    public ResponseEntity<List<PlatformNodeDTO>> platformNodes(){
        return   ResponseEntity.ok(this.iBaseService.platformNodesAsCurrency());
    }
}
