package com.izhiliu.erp.service.discount.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.pricing.PricingStrategies;
import com.izhiliu.erp.service.discount.dto.PricingStrategiesDto;
import org.mapstruct.Mapper;

import java.util.Objects;


@Mapper(componentModel = "spring", uses = {})
public interface PricingStrategiesMapper extends EntityMapper<PricingStrategiesDto, PricingStrategies> {

    default Integer map(Float aFloat) {
        if (Objects.isNull(aFloat)) {
            return null;
        }
        return (int) (aFloat * 100);
    }

    default Float map(Integer aFloat) {
        if (Objects.isNull(aFloat)) {
            return null;
        }
        final float v = aFloat.floatValue() / 100f;
        return v;
    }

}
