package com.izhiliu.erp.service.image.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.core.util.Constants;
import com.izhiliu.erp.config.module.upload.QiNiuUploadUtils;
import com.izhiliu.erp.domain.image.ImageBankAddress;
import com.izhiliu.erp.domain.image.ImageBankDirectory;
import com.izhiliu.erp.repository.image.ImageBankAddressRepository;
import com.izhiliu.erp.service.image.ImageBankAddressService;
import com.izhiliu.erp.service.image.ImageBankDirectoryService;
import com.izhiliu.erp.service.image.cache.impl.ImageBankCacheServiceImpl;
import com.izhiliu.erp.service.image.dto.ImageBankAddressDto;
import com.izhiliu.erp.service.image.dto.ImageBankCapacityCacheObejct;
import com.izhiliu.erp.service.image.mapper.ImageBankAddressMapper;
import com.izhiliu.erp.service.item.UserImageService;
import com.izhiliu.erp.service.item.dto.UserImageDTO;
import com.izhiliu.erp.util.SnowflakeGenerate;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import com.izhiliu.erp.web.rest.image.param.ImageBankAddressCondition;
import com.izhiliu.erp.web.rest.image.param.MovingPictureCondition;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ClientUserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 */
@Service
public class ImageBankAddressServiceImpl extends IBaseServiceImpl<ImageBankAddress, ImageBankAddressDto, ImageBankAddressRepository, ImageBankAddressMapper> implements ImageBankAddressService {

    private final Logger log = LoggerFactory.getLogger(ImageBankAddressServiceImpl.class);

    @Resource
    ImageBankDirectoryService imageBankDirectoryService;
    @Resource
    UaaService uaaService;
    @Resource
    SnowflakeGenerate snowflakeGenerate;

    @Resource
    ImageBankCacheServiceImpl imageBankCacheService;




    @Override
    public ImageBankAddressDto insert(ImageBankAddressDto aDto) {
        final String currentLogin = SecurityUtils.currentLogin();
        if(Objects.isNull(aDto.getDirectoryId())){
            final ImageBankDirectory rootImageBankDirectories = imageBankDirectoryService.getRootImageBankDirectories(currentLogin);
            aDto.setDirectoryId(rootImageBankDirectories.getId());
        }
        aDto.setLoginId(currentLogin);
        log.info("currentLogin {} ImageBankAddressDto {}",currentLogin,aDto);
        if(Objects.isNull(aDto.getKkyId())){
            final String substring = aDto.getUrl().substring(0, aDto.getUrl().indexOf("/"));
            aDto.setKkyId(Long.parseLong(substring));
        }
        final boolean ok = imageBankCacheService.doPull(Optional.of(currentLogin)).isOk(aDto.getImageSize(), newRemainingMemory -> {
            imageBankCacheService.minus(newRemainingMemory, currentLogin);
        });
        if(!ok){
            throw   new IllegalOperationException("您的 图片存储 已满");
        }
        aDto.setId(snowflakeGenerate.nextId());
        return  save(aDto);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public IPage<ImageBankAddressDto> selectByLoginid(ImageBankAddressCondition condition) {
        final String currentLogin = SecurityUtils.currentLogin();
        log.info("currentLogin {} ImageBankAddressDto {}",currentLogin,condition);
        final IPage<ImageBankAddressDto> selectByLoginid = map(repository.selectByLoginid(new Page<>(condition.getPage()+1, condition.getSize()), condition,currentLogin),this.getMapper());
        return selectByLoginid;
    }

    @Override
    public void movingAndManagementPicture(MovingPictureCondition movingPicture) {
        final String currentLogin = SecurityUtils.currentLogin();
        log.info("currentLogin {} ImageBankAddressDto {}",currentLogin,movingPicture);

        repository.movingImage(movingPicture,currentLogin);
    }

    /**
     *  查询当前用户的 图片银行信息
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImageBankCapacityCacheObejct selectImageSizeByUserId() {
        final String currentLogin = SecurityUtils.currentLogin();
         final Optional<ImageBankCapacityCacheObejct> run = imageBankCacheService.pull(Optional.of(currentLogin));
         if(!run.isPresent()){
             log.error("该用户暂未开通功能 或者已过期  currentLogin {}",currentLogin);
             throw  new IllegalOperationException("该用户暂未开通功能 或者已过期");
         }
         return run.get();
    }

    @Override
    public Long selectImageSizeByUserId(String currentLogin) {
        return repository.selectImageSizeByUserId(currentLogin);
    }

    @Override
    public void  remove(List<Long> deleteId) {
        final String currentLogin = SecurityUtils.currentLogin();
        long size = 0;
        List<String> urls=new ArrayList<>();
        for (Long aLong : deleteId) {
            final ImageBankAddressDto checke = checke(aLong);
            urls.add(checke.getUrl());
           size = size - checke.getImageSize();
        }
        final boolean delete = this.delete(deleteId);
        log.info("currentLogin {} deleteId {} size {}",currentLogin,deleteId,size);
        deleteRemoteImageInfos(currentLogin,urls);
        if(delete){
            imageBankCacheService.minus(size, currentLogin);
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImageBankAddressDto checke(Long id) {
        final Optional<ImageBankAddressDto> boostItemDTO1 = this.find(id);
        final ImageBankAddressDto boostItemDTO2 = boostItemDTO1.orElseThrow(() -> new IllegalOperationException("illegal.operation.exception.image.not.found.exception", true));
        final String currentUserLogin = SecurityUtils.currentLogin();
        if (!boostItemDTO2.getLoginId().equals(currentUserLogin) && !currentUserLogin.equals(Constants.ANONYMOUS_USER)) {
            throw new IllegalOperationException("illegal.operation.exception",true);
        }
        return  boostItemDTO2;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<ImageBankAddressDto> checke(List<Long> ids) {
        final List<ImageBankAddressDto> collect = ids.stream().map(this::checke).collect(Collectors.toList());
        return collect;
    }


    public void deleteRemoteImageInfos(String loginId){
        long size;
        do {
            final IPage<ImageBankAddress> imageBankAddressIPage = repository.selectByLoginid(new Page<>(1, 10), new ImageBankAddressCondition(), loginId);
            size = imageBankAddressIPage.getSize();
            if(size > 0) {
                QiNiuUploadUtils.delete(imageBankAddressIPage.getRecords().stream().map(ImageBankAddress::getUrl).collect(Collectors.toList()));
                repository.deleteBatchIds(imageBankAddressIPage.getRecords().stream().map(ImageBankAddress::getId).collect(Collectors.toList()));
            }
        }while (size != 0);
    }

    public void deleteRemoteImageInfos(String loginId,List<String> imagsUrls){
        try {
            QiNiuUploadUtils.delete(imagsUrls);
        } catch (Exception e) {
           log.error("{}  loginId: {}  imagsUrls :{}",e,loginId,imagsUrls);
        }
    }
}


