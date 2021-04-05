package com.izhiliu.erp.repository.item;

import com.izhiliu.erp.domain.item.ItemCommentDetail;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author Twilight
 * @date 2021/2/8 9:49
 */
public interface ItemCommentDetailRepository extends MongoRepository<ItemCommentDetail, String> {
}
