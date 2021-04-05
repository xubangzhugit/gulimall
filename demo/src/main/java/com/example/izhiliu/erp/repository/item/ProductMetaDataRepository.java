package com.izhiliu.erp.repository.item;

import com.izhiliu.erp.domain.item.ProductMetaData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;


/**
 * Spring Data  repository for the ProductMetaData entity.
 */
public interface ProductMetaDataRepository extends MongoRepository<ProductMetaData, String> {


    Page<ProductMetaData> findAllByUrl(String url, Pageable pageable);

    Page<ProductMetaData> findAllByLoginId(String loginId, Pageable pageable);

    Page<ProductMetaData> findAllByLoginIdAndNameLike(String loginId, String name, Pageable pageable);

    Page<ProductMetaData> findAllByCollectTimeBetween(Instant start, Instant end, Pageable pageable);
}
