package com.izhiliu.erp.service.item.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.izhiliu.core.domain.common.BaseServiceImpl;
import com.izhiliu.erp.domain.item.KyyCategory;
import com.izhiliu.erp.repository.item.KyyCategoryRepository;
import com.izhiliu.erp.service.item.KyyCategoryRelationService;
import com.izhiliu.erp.service.item.KyyCategoryService;
import com.izhiliu.erp.service.item.dto.KyyCategoryDTO;
import com.izhiliu.erp.service.item.dto.KyyCategoryRelationDTO;
import com.izhiliu.erp.service.item.mapper.KyyCategoryMapper;
import com.izhiliu.erp.util.SnowflakeGenerate;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.param.KyyCategoryQO;
import com.izhiliu.erp.web.rest.item.vm.KyyCategoryVO;
import com.izhiliu.open.shopee.open.sdk.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Twilight
 * @date 2021/1/18 16:27
 */
@Slf4j
@Service
public class KyyCategoryServiceImpl extends BaseServiceImpl<KyyCategory, KyyCategoryDTO,
        KyyCategoryRepository, KyyCategoryMapper> implements KyyCategoryService {

    private static final String DEFAULT_PARENT = "0";

    @Resource
    private SnowflakeGenerate snowflakeGenerate;
    @Resource
    private KyyCategoryRelationService kyyCategoryRelationService;

    /**
     * 添加和修改客优云类目
     * @param qo
     * @return
     */
    @Override
    public KyyCategoryVO saveKyyCategory(KyyCategoryQO qo) {
        final String login = qo.getLogin();
        final String kyyCategoryId = qo.getKyyCategoryId();
        KyyCategoryDTO dto = KyyCategoryDTO
                .builder()
                .login(login)
                .categoryLevel(qo.getCategoryLevel())
                .categoryName(qo.getCategoryName())
                .kyyCategoryId(kyyCategoryId)
                .parentId(qo.getParentId())
                .build();
        return CommonUtils.isBlank(kyyCategoryId) ? addKyyCategory(dto) : updateKyyCategory(dto);
    }

    private KyyCategoryVO updateKyyCategory(KyyCategoryDTO dto) {
        final String login = dto.getLogin();
        final String kyyCategoryId = dto.getKyyCategoryId();
        update(dto,new QueryWrapper<KyyCategory>()
                .eq("login", login)
                .eq("kyy_category_id", kyyCategoryId));
        return convert(dto);
    }

    private KyyCategoryVO addKyyCategory(KyyCategoryDTO dto) {
        final String categoryId = String.valueOf(snowflakeGenerate.nextId());
        dto.setKyyCategoryId(categoryId);
        return convert(save(dto));
    }

    /**
     * 查询客优云类目
     * @param parentId
     * @param login
     * @return
     */
    @Override
    public List<KyyCategoryVO> getKyyCategory(String parentId, String login) {
        List<KyyCategoryDTO> list = list(new QueryWrapper<KyyCategory>()
                .eq("login", login)
                .eq("parent_id", parentId)
                .orderByAsc("gmt_create")
        );
        return list.stream().map(this::convert).collect(Collectors.toList());
    }

    /**
     * 查询客优云类目层级
     * 1.查询商品是否有类目
     * 2.循环查询父类目
     * @param productId
     * @param login
     * @return
     */
    @Override
    public List<KyyCategoryVO> getKyyCategoryForebears(String productId, String login) {
        //返回集合
        List<KyyCategoryVO> list = new ArrayList<>();
        //查询商品是否有类目
        KyyCategoryRelationDTO kyyCategoryRelationDTO = kyyCategoryRelationService.selectCategoryByProductId(productId, login);
        if (CommonUtils.isBlank(kyyCategoryRelationDTO)){
            return list;
        }
        Optional<KyyCategoryDTO> optional = findOne(new QueryWrapper<KyyCategory>()
                .eq("login", login)
                .eq("kyy_category_id", kyyCategoryRelationDTO.getKyyCategoryId())
        );
        if (!optional.isPresent()){
            return list;
        }
        KyyCategoryDTO kyyCategoryDTO = optional.get();
        while (true) {
            list.add(convert(kyyCategoryDTO));
            if (CommonUtils.isBlank(kyyCategoryDTO) || CommonUtils.isBlank(kyyCategoryDTO.getParentId()) || Objects.equals(kyyCategoryDTO.getParentId(),DEFAULT_PARENT)) {
                break;
            } else {
                kyyCategoryDTO = findOne(new QueryWrapper<KyyCategory>()
                        .eq("login", login)
                        .eq("kyy_category_id", kyyCategoryDTO.getParentId())
                ).orElse(null);
            }
        }
        return list;
    }

    /**
     * 删除客优云类目
     * 1.获取到需要删除的id
     * 2.根据id批量删除
     * @param qo
     * @return
     */
    @Override
    public Boolean deleteKyyCategory(KyyCategoryQO qo) {
        final String login = qo.getLogin();
        final List<String> kyyCategoryIdList = qo.getKyyCategoryIdList();
        List<KyyCategoryDTO> list = list(new QueryWrapper<KyyCategory>()
                .eq("login", login)
                .in("kyy_category_id", kyyCategoryIdList));
        if (CommonUtils.isBlank(list)){
            return false;
        }
        List<Long> ids = list.stream()
                .map(KyyCategoryDTO::getId)
                .collect(Collectors.toList());
        return delete(ids);
    }



    private KyyCategoryVO convert(KyyCategoryDTO kyyCategoryDTO){
        KyyCategoryVO kyyCategoryVO = new KyyCategoryVO();
        if (CommonUtils.isNotBlank(kyyCategoryDTO)){
            BeanUtils.copyProperties(kyyCategoryDTO,kyyCategoryVO);
        }
        return kyyCategoryVO;
    }

}
