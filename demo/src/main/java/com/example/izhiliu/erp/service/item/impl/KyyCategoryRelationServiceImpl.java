package com.izhiliu.erp.service.item.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.izhiliu.core.domain.common.BaseServiceImpl;
import com.izhiliu.erp.domain.item.KyyCategoryRelation;
import com.izhiliu.erp.repository.item.KyyCategoryRelationRepository;
import com.izhiliu.erp.service.item.KyyCategoryRelationService;
import com.izhiliu.erp.service.item.dto.KyyCategoryRelationDTO;
import com.izhiliu.erp.service.item.mapper.KyyCategoryRelationMapper;
import com.izhiliu.erp.web.rest.item.param.KyyCategoryRelationQO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author Twilight
 * @date 2021/1/18 17:51
 */
@Slf4j
@Service
public class KyyCategoryRelationServiceImpl extends BaseServiceImpl<KyyCategoryRelation, KyyCategoryRelationDTO,
        KyyCategoryRelationRepository, KyyCategoryRelationMapper> implements KyyCategoryRelationService {

    /**
     * 商品添加和修改客优云分类
     * @param qo
     * @return
     */
    @Override
    public Boolean itemAddKyyCategory(KyyCategoryRelationQO qo) {
        final String login = qo.getLogin();
        final List<KyyCategoryRelationQO.ProductBean> productBeanList = qo.getProductBeanList();
        productBeanList.stream().forEach(e -> {
            final String kyyCategoryId = e.getKyyCategoryId();
            final Long productId = e.getProductId();
            KyyCategoryRelationDTO dto = KyyCategoryRelationDTO
                    .builder()
                    .login(login)
                    .productId(productId)
                    .kyyCategoryId(kyyCategoryId)
                    .build();
            findOne(new QueryWrapper<KyyCategoryRelation>()
                    .eq("login", login)
                    .eq("product_id", productId))
                    .ifPresent(f-> dto.setId(f.getId()));
            saveOrUpdate(dto);
        });
        return true;
    }

    /**
     * 根据商品查询类目id
     * @param productId
     * @param login
     * @return
     */
    @Override
    public KyyCategoryRelationDTO selectCategoryByProductId(String productId, String login) {
        return findOne(new QueryWrapper<KyyCategoryRelation>()
                .eq("login", login)
                .eq("product_id", productId))
                .orElse(null);

    }

    /**
     * 根据类目获取productIds
     * @param kyyCategoryIdList
     * @return
     */
    @Override
    public Set<Long> selectProductIdByCategorys(List<String> kyyCategoryIdList, String login) {
        List<KyyCategoryRelationDTO> list = list(new QueryWrapper<KyyCategoryRelation>()
                .eq("login", login)
                .in("kyy_category_id", kyyCategoryIdList));
        return list.stream().map(KyyCategoryRelationDTO::getProductId).collect(Collectors.toSet());
    }
}
