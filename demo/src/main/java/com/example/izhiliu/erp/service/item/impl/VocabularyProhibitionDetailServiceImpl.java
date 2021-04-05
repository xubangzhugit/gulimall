package com.izhiliu.erp.service.item.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.config.internation.InternationUtils;
import com.izhiliu.core.domain.common.BaseServiceImpl;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.config.internation.HandleMessageSource;
import com.izhiliu.erp.domain.item.VocabularyProhibitionDetail;
import com.izhiliu.erp.repository.item.VocabularyProhibitionDetailRepository;
import com.izhiliu.erp.service.item.VocabularyProhibitionDetailService;
import com.izhiliu.erp.service.item.dto.VocabularyDTO;
import com.izhiliu.erp.service.item.dto.VocabularyProhibitionDetailDTO;
import com.izhiliu.erp.service.item.mapper.VocabularyProhibitionDetailMapper;
import com.izhiliu.erp.web.rest.discount.qo.VocabularyQO;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.vm.VocabularyVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 14:58
 */
@Service
public class VocabularyProhibitionDetailServiceImpl extends BaseServiceImpl<VocabularyProhibitionDetail, VocabularyProhibitionDetailDTO, VocabularyProhibitionDetailRepository, VocabularyProhibitionDetailMapper> implements VocabularyProhibitionDetailService {

    @Resource
    private HandleMessageSource handleMessageSource;

    @Override
    public List<VocabularyProhibitionDetailDTO> vocabularyDetailAll(VocabularyDTO dto) {
        List<VocabularyProhibitionDetailDTO> list = list(new QueryWrapper<VocabularyProhibitionDetail>().eq("login", dto.getLogin()).in("vocabulary_id", dto.getVocabularyTypeIds()));
        return list;
    }

    @Override
    public IPage<VocabularyProhibitionDetailDTO> getVocabularyProhibitionDetail(VocabularyQO qo) {
        QueryWrapper<VocabularyProhibitionDetail> queryWrapper = new QueryWrapper<VocabularyProhibitionDetail>().eq("login", qo.getLogin());
        if (CommonUtils.isNotBlank(qo.getVocabularyTypeId())) {
            queryWrapper.eq("vocabulary_id", qo.getVocabularyTypeId());
        }
        if (CommonUtils.isNotBlank(qo.getVocabularyTypeName())) {
            queryWrapper.eq("vocabulary_name", qo.getVocabularyTypeName());
        }
        return page(new Page<>(qo.getPage() + 1, qo.getSize()), queryWrapper);
    }

    @Override
    public List<VocabularyVO.VocabularyInfo> conventTOVO(List<VocabularyProhibitionDetailDTO> vocabularyList) {
        List<VocabularyVO.VocabularyInfo> builders = vocabularyList.stream().map(m -> {
            VocabularyVO.VocabularyInfo build = VocabularyVO.VocabularyInfo.builder().vocabularyName(m.getVocabularyName()).vocabularyId(m.getId()).build();
            return build;
        }).collect(Collectors.toList());
        return builders;
    }

    @Override
    public boolean updateVocabularyProhibitionDetail(VocabularyDTO dto) {
        List<VocabularyProhibitionDetailDTO> collect = dto.getInfo().stream().map(m -> {
            VocabularyProhibitionDetailDTO build = VocabularyProhibitionDetailDTO.builder().login(dto.getLogin()).id(m.getVocabularyId()).vocabularyName(m.getVocabularyName())
                    .vocabularyId(dto.getVocabularyTypeId()).build();
            return build;
        }).collect(Collectors.toList());
        // 检查是否重复
        checkUp(dto);
        return batchSaveOrUpdate(collect);
    }

    private void checkUp(VocabularyDTO dto) {
        // 新增加的判断是否重复
        ArrayList<String> repeating = new ArrayList();
        List<Long> detailID = dto.getInfo().stream().map(VocabularyDTO.VocabularyInfo::getVocabularyId).collect(Collectors.toList());
        // 要更新的数据过滤掉本来的数据，进行匹配如果存在相同就是重复数据，需要排除
        List<VocabularyProhibitionDetailDTO> prohibitionDetail = list(new QueryWrapper<VocabularyProhibitionDetail>().eq("login", dto.getLogin()).eq("vocabulary_id", dto.getVocabularyTypeId()));
        // 获取UPDATE时的词汇名
        List<String> updateDetailName = dto.getInfo().stream().filter(f -> CommonUtils.isNotBlank(f.getVocabularyId())).map(VocabularyDTO.VocabularyInfo::getVocabularyName).collect(Collectors.toList());
        // 获取ADD时的词汇名
        List<String> addDetailName = dto.getInfo().stream().filter(f -> CommonUtils.isBlank(f.getVocabularyId())).map(VocabularyDTO.VocabularyInfo::getVocabularyName).collect(Collectors.toList());
        // 如果传来两个相同的也需要提示
        List<String> stringList = dto.getInfo().stream().map(VocabularyDTO.VocabularyInfo::getVocabularyName).filter(f -> {
            return dto.getInfo().stream().map(VocabularyDTO.VocabularyInfo::getVocabularyName).filter(f::equals).count() > 1;
        }).distinct().collect(Collectors.toList());
        repeating.addAll(stringList);
        // UPDATE时重复的词汇名
        List<String> repeatingDetailName = prohibitionDetail.stream().filter(s -> !detailID.contains(s.getId())).filter(f -> {
            return updateDetailName.contains(f.getVocabularyName());
        }).map(VocabularyProhibitionDetailDTO::getVocabularyName).collect(Collectors.toList());
        repeating.addAll(repeatingDetailName);
        // ADD时重复的词汇名
        List<String> repeatingName = prohibitionDetail.stream().filter(f -> {
            return addDetailName.contains(f.getVocabularyName());
        }).map(VocabularyProhibitionDetailDTO::getVocabularyName).collect(Collectors.toList());
        if (CommonUtils.isNotBlank(repeatingName)) {
            repeating.addAll(repeatingName);
        }
        if (CommonUtils.isNotBlank(repeating)) {
            throw new LuxServerErrorException(handleMessageSource.getMessage("vocabulary.error", new String[]{repeating.stream().distinct().collect(Collectors.joining("、"))}, InternationUtils.getLocale()));
        }
    }

    @Override
    public boolean deleteVocabularyProhibition(VocabularyDTO dto) {
        QueryWrapper<VocabularyProhibitionDetail> queryWrapper = new QueryWrapper<VocabularyProhibitionDetail>().eq("login", dto.getLogin());
        if (CommonUtils.isNotBlank(dto.getVocabularyTypeIds())) {
            queryWrapper.in("vocabulary_id", dto.getVocabularyTypeId());
        } else {
            queryWrapper.in("id", dto.getVocabularyIds());
        }
        return delete(queryWrapper);
    }
}
