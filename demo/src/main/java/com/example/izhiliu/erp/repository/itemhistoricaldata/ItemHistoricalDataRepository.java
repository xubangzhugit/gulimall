package com.izhiliu.erp.repository.itemhistoricaldata;

import com.izhiliu.erp.domain.itemhistoricaldata.ItemHistoricalDataRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ItemHistoricalDataRepository extends MongoRepository<ItemHistoricalDataRecord, String> {
    @Query("{'data.updateTime':{'$gt':'?0'}}")
    List<ItemHistoricalDataRecord> findByUpdate(Instant updateTime);
}
