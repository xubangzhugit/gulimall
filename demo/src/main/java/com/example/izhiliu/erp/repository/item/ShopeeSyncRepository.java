package com.izhiliu.erp.repository.item;

import com.izhiliu.erp.service.item.dto.ShopeeSyncDTO;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShopeeSyncRepository extends MongoRepository<ShopeeSyncDTO, String> {

}
