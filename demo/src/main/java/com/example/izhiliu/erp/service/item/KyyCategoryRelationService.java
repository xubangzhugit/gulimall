package com.izhiliu.erp.service.item;

import com.izhiliu.core.domain.common.BaseService;
import com.izhiliu.erp.domain.item.KyyCategoryRelation;
import com.izhiliu.erp.service.item.dto.KyyCategoryRelationDTO;
import com.izhiliu.erp.web.rest.item.param.KyyCategoryRelationQO;

import java.util.List;
import java.util.Set;

/**
 * @author Twilight
 * @date 2021/1/18 17:51
 */
public interface KyyCategoryRelationService extends BaseService<KyyCategoryRelation, KyyCategoryRelationDTO> {

    /**
     * 商品添加和修改客优云分类
     * @param qo
     * @return
     */
    Boolean itemAddKyyCategory(KyyCategoryRelationQO qo);

    /**
     * 根据商品查询类目id
     * @param productId
     * @param login
     * @return
     */
    KyyCategoryRelationDTO selectCategoryByProductId(String productId, String login);

    /**
     * 根据类目获取productIds
     * @param kyyCategoryIdList
     * @return
     */
    Set<Long> selectProductIdByCategorys(List<String> kyyCategoryIdList, String login);
}
