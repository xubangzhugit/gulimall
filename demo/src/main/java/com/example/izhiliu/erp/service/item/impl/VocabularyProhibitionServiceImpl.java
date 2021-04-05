package com.izhiliu.erp.service.item.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.domain.common.BaseServiceImpl;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.config.internation.HandleMessageSource;
import com.izhiliu.erp.domain.item.VocabularyProhibition;
import com.izhiliu.erp.domain.item.VocabularyProhibitionDetail;
import com.izhiliu.erp.repository.item.VocabularyProhibitionRepository;
import com.izhiliu.erp.service.item.VocabularyProhibitionDetailService;
import com.izhiliu.erp.service.item.VocabularyProhibitionService;
import com.izhiliu.erp.service.item.dto.VocabularyDTO;
import com.izhiliu.erp.service.item.dto.VocabularyProhibitionDTO;
import com.izhiliu.erp.service.item.dto.VocabularyProhibitionDetailDTO;
import com.izhiliu.erp.service.item.mapper.VocabularyProhibitionMapper;
import com.izhiliu.erp.web.rest.discount.qo.VocabularyQO;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.vm.VocabularyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 14:53
 */
@Slf4j
@Service
public class VocabularyProhibitionServiceImpl extends BaseServiceImpl<VocabularyProhibition, VocabularyProhibitionDTO, VocabularyProhibitionRepository, VocabularyProhibitionMapper> implements VocabularyProhibitionService {

    @Resource
    private VocabularyProhibitionDetailService vocabularyProhibitionDetailService;
    @Resource
    private HandleMessageSource handleMessageSource;

    @Override
    public IPage<VocabularyProhibitionDTO> getVocabularyAll(VocabularyQO qo) {
        QueryWrapper<VocabularyProhibition> queryWrapper = new QueryWrapper<VocabularyProhibition>().eq("login", qo.getLogin());
        if (CommonUtils.isNotBlank(qo.getVocabularyTypeName())) {
            queryWrapper.eq("vocabulary_type", qo.getVocabularyTypeName());
        }
        return page(new Page<>(qo.getPage() + 1, qo.getSize()), queryWrapper);
    }

    @Override
    public List<VocabularyVO> conventTOVO(List<VocabularyProhibitionDTO> records) {
        final String login = SecurityUtils.getCurrentLogin();
        List<Long> vocabularyTypeId = records.stream().map(VocabularyProhibitionDTO::getId).collect(Collectors.toList());
        // 根据词汇类型获取有的禁用词汇
        VocabularyDTO build = VocabularyDTO.builder().login(login).vocabularyTypeIds(vocabularyTypeId).build();
        List<VocabularyProhibitionDetailDTO> vocabularyProhibitionDetails = vocabularyProhibitionDetailService.vocabularyDetailAll(build);
        List<VocabularyVO> vocabularyList = records.stream().map(m -> {
            VocabularyVO vocabularyVO = new VocabularyVO();
            vocabularyVO.setVocabularyTypeId(m.getId());
            vocabularyVO.setVocabularyTypeName(m.getVocabularyType());
            List<VocabularyVO.VocabularyInfo> infos = vocabularyProhibitionDetails.stream().filter(f -> Objects.equals(f.getVocabularyId(), m.getId())).map(e -> {
                VocabularyVO.VocabularyInfo info = VocabularyVO.VocabularyInfo.builder().vocabularyId(e.getId()).vocabularyName(e.getVocabularyName()).build();
                return info;
            }).collect(Collectors.toList());
            vocabularyVO.setInfo(infos);
            return vocabularyVO;
        }).collect(Collectors.toList());
        return vocabularyList;
    }

    @Override
    public boolean updateVocabularyProhibition(VocabularyDTO dto) {
        VocabularyProhibitionDTO build = VocabularyProhibitionDTO.builder().login(dto.getLogin()).vocabularyType(dto.getVocabularyTypeName()).id(dto.getVocabularyTypeId()).build();
        List<VocabularyProhibitionDTO> list = list(new QueryWrapper<VocabularyProhibition>().eq("login", dto.getLogin()));

        Optional<VocabularyProhibitionDTO> first = list.stream().filter(x -> Objects.equals(dto.getVocabularyIds(),x.getId())).filter(f -> Objects.equals(f.getVocabularyType(), dto.getVocabularyTypeName())).findFirst();
        if (first.isPresent()) {
            throw new LuxServerErrorException(handleMessageSource.getMessage("vocabulary.type.error", new String[]{first.get().getVocabularyType()}));
        }

        boolean vocabulary = saveOrUpdate(build);

        Optional<VocabularyProhibitionDTO> one = findOne(new QueryWrapper<VocabularyProhibition>().eq("vocabulary_type", dto.getVocabularyTypeName()));
        if (one.isPresent()) {
            dto.setVocabularyTypeId(one.get().getId());
        }
        boolean vocabularyProhibition = vocabularyProhibitionDetailService.updateVocabularyProhibitionDetail(dto);
        if (vocabulary && vocabularyProhibition) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean deleteVocabularyType(VocabularyDTO dto) {
        boolean delete = delete(new QueryWrapper<VocabularyProhibition>().eq("login", dto.getLogin()).in("id", dto.getVocabularyTypeIds()));
        boolean result = vocabularyProhibitionDetailService.deleteVocabularyProhibition(dto);
        if (delete && result) {
            return true;
        }
        return false;
    }
}
