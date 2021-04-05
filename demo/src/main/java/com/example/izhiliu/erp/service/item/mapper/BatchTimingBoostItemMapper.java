package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.BoostItem;
import com.izhiliu.erp.service.item.dto.BoostItemDTO;
import com.izhiliu.erp.service.item.dto.MyBoostItem;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;


@Mapper(componentModel = "spring")
public interface BatchTimingBoostItemMapper extends EntityMapper<BoostItemDTO, BoostItem> {

    default LocalDateTime mapDate(Instant gmtCreate) {
        if(Objects.isNull(gmtCreate)){
            return null;
        }
        return LocalDateTime.ofInstant(gmtCreate, ZoneOffset.UTC);
    }
    default Instant mapDate(LocalDateTime gmtCreate) {
        if(Objects.isNull(gmtCreate)){
            return null;
        }
        return gmtCreate.toInstant(ZoneOffset.UTC);
    }

    BoostItem toEntityV2(MyBoostItem dto);

    MyBoostItem toDtoV2(BoostItem entity);

    List<BoostItem> toEntityV2(List<MyBoostItem> dtoList);

    List <MyBoostItem> toDtoV2(List<BoostItem> entityList);

}
