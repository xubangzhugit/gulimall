package com.izhiliu.erp.web.rest;

import cn.hutool.core.util.EnumUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.codahale.metrics.annotation.Timed;
import com.google.gson.JsonObject;
import com.izhiliu.core.common.constant.CurrencyEnum;
import com.izhiliu.core.common.constant.PlatformNodeEnum;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.config.module.currency.CurrencyRateApiImpl;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateApi;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateResult;
import com.izhiliu.erp.config.module.upload.QiNiuUploadResource;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/14 20:33
 */
@Validated
@RestController
@RequestMapping("/api")
public class HelperAPI {

    @Resource
    private QiNiuUploadResource qiNiuUploadResource;

    @Resource
    private CurrencyRateApiImpl  currencyRateApil;

    @Autowired
    @Qualifier(value = "handleProductExceptionInfo")
    private HandleProductExceptionInfo messageSource;

    @GetMapping("/currency/codes")
    @Timed
    public ResponseEntity<List<Object>> currentCodes() {
        return ResponseEntity.ok(EnumUtil.getFieldValues(CurrencyRateApi.Currency.class, "code"));
    }

    @GetMapping("/shopee/nodes")
    public ResponseEntity<String> shopeeNodes() {
        final SerializeConfig config = new SerializeConfig();
        config.configEnumAsJavaBean(PlatformNodeEnum.class);
        return ResponseEntity.ok(JSON.toJSONString(PlatformNodeEnum.NODES, config));
    }
    @GetMapping("/shopee/currency")
    public ResponseEntity<String> currencyEnum() {
        final SerializeConfig config = new SerializeConfig();
        config.configEnumAsJavaBean(CurrencyEnum.class);
        final JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(CurrencyEnum.values(), config));
        final String body = JSON.toJSONString(jsonArray.toJavaList(JSONObject.class).stream().map(jsonObject -> {
            final Map<String, Object> innerMap = jsonObject.getInnerMap();
            final String code = (String) innerMap.get("code");
            innerMap.put("msg", messageSource.doMessage(code));
            return innerMap;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/shopee/getNodeId")
    public ResponseEntity<Long> getShopeeNodeId(@NotEmpty String code) {
        return ResponseEntity.ok(ShopeeUtil.nodeId(code));
    }

    @GetMapping("/shopee/getCode")
    public ResponseEntity<String> getShopeeCode(@NotNull Long id) {
        return ResponseEntity.ok(ShopeeUtil.code(id));
    }

    @PostMapping("/upload/token")
    @Timed
    public ResponseEntity<String> upload() {
        return ResponseEntity.ok(qiNiuUploadResource.token());
    }

    @GetMapping(value = {
            "/money/currencyConvert/{from}/{to}",
            "/service/money/currencyConvert/{from}/{to}"
    })
    @Timed
    public ResponseEntity<CurrencyRateResult> currencyConvert(@PathVariable(value = "from") String from, @PathVariable(value = "to") String to) {

        return ResponseEntity.ok(currencyRateApil.currencyConvert(
                Objects.requireNonNull(CurrencyRateApi.Currency.selectCurrency(from)),
                Objects.requireNonNull(CurrencyRateApi.Currency.selectCurrency(to))
                ));
    }
}
