package com.izhiliu.erp.service.item;

import com.izhiliu.core.domain.common.BaseService;
import com.izhiliu.erp.domain.item.KyyCategory;
import com.izhiliu.erp.service.item.dto.KyyCategoryDTO;
import com.izhiliu.erp.web.rest.item.param.KyyCategoryQO;
import com.izhiliu.erp.web.rest.item.vm.KyyCategoryVO;

import java.util.List;

/**
 * @author Twilight
 * @date 2021/1/18 16:26
 */
public interface KyyCategoryService extends BaseService<KyyCategory, KyyCategoryDTO> {

    /**
     * 添加和修改客优云类目
     * @param qo
     * @return
     */
    KyyCategoryVO saveKyyCategory(KyyCategoryQO qo);

    /**
     * 查询客优云类目
     * @param parentId
     * @param login
     * @return
     */
    List<KyyCategoryVO> getKyyCategory(String parentId, String login);

    /**
     * 查询客优云类目层级
     * @param productId
     * @param login
     * @return
     */
    List<KyyCategoryVO> getKyyCategoryForebears(String productId, String login);

    /**
     * 删除客优云类目
     * @param qo
     * @return
     */
    Boolean deleteKyyCategory(KyyCategoryQO qo);


}
