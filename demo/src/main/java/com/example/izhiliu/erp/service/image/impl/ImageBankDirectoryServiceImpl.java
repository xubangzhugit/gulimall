package com.izhiliu.erp.service.image.impl;

import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.core.util.Constants;
import com.izhiliu.erp.domain.image.ImageBankDirectory;
import com.izhiliu.erp.repository.image.ImageBankDirectoryRepository;
import com.izhiliu.erp.service.image.ImageBankDirectoryService;
import com.izhiliu.erp.service.image.dto.ImageBankAddressDto;
import com.izhiliu.erp.service.image.dto.ImageBankDirectoryDto;
import com.izhiliu.erp.service.image.mapper.ImageBankDirectoryMapper;
import com.izhiliu.erp.service.image.result.ImageBankDirectoryTree;
import com.izhiliu.erp.util.SnowflakeGenerate;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ClientUserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 */
@Service
public class ImageBankDirectoryServiceImpl extends IBaseServiceImpl<ImageBankDirectory, ImageBankDirectoryDto, ImageBankDirectoryRepository, ImageBankDirectoryMapper> implements ImageBankDirectoryService {

    private final Logger log = LoggerFactory.getLogger(ImageBankDirectoryServiceImpl.class);

    public final static String IMAGE_BANK_INIT_KEY = "lux:image:bank:init:directory:";

    @Resource
    SnowflakeGenerate snowflakeGenerate;

    @Resource
    RedisLockHelper redisLockHelper;


    @Override
    public ImageBankDirectoryDto insert(ImageBankDirectoryDto aDto) {
        if(Objects.isNull(aDto.getParentId())){
            throw  new IllegalOperationException("illegal.operation.exception",true);
        }
        final Integer level =aDto.getParentId()==0?0: checke(aDto.getParentId()).getLevel();
        if (level == 3) {
            throw  new IllegalOperationException("image.bank.directory.level",true);
        }
        final String currentLogin = SecurityUtils.currentLogin();
        aDto.setLoginId(currentLogin);
        aDto.setId(snowflakeGenerate.nextId());
        aDto.upLevel(level);
        final ImageBankDirectoryDto save = save(aDto);
        return save;
    }

    @Override
    public List<ImageBankDirectoryTree> selectByLoginid() {
        final String currentLogin = SecurityUtils.currentLogin();
        final ImageBankDirectory imageBankDirectories = getRootImageBankDirectories(currentLogin);
        final List<ImageBankDirectoryDto> imageBankDirectoryDtos = mapper.toDto(repository.selectByLoginid(currentLogin,null));
        final List<ImageBankDirectoryTree> collect = imageBankDirectoryDtos.stream().map(ImageBankDirectoryTree::imageBankDirectoryTree).collect(Collectors.toList());

        final ImageBankDirectoryTree children = getChildren(ImageBankDirectoryTree.rootNode(), collect);
        return  children.getChildren().stream().peek(imageBankDirectoryTree -> {
            imageBankDirectoryTree.setRoot(Boolean.TRUE);
        }).collect(Collectors.toList());
    }

    @Override
    public ImageBankDirectory getRootImageBankDirectories(String currentLogin) {
        List<ImageBankDirectory> imageBankDirectories = repository.selectByLoginid(currentLogin, Long.valueOf(ImageBankDirectoryTree.rootNode().getId()));
        if(CollectionUtils.isEmpty(imageBankDirectories)){
            final boolean lock = redisLockHelper.lock(IMAGE_BANK_INIT_KEY + currentLogin,10, TimeUnit.SECONDS);
            if(lock){
                final ImageBankDirectoryDto imageBankDirectoryDto = new ImageBankDirectoryDto().setLoginId(currentLogin).setParentId(0L).setName("全部图片").setLevel(1);
                imageBankDirectoryDto.setId(snowflakeGenerate.nextId());
                return   mapper.toEntity(save(imageBankDirectoryDto));
            }else{
                throw  new IllegalOperationException("image.bank.directory.init",true);
            }
        }
        return   imageBankDirectories.iterator().next();
    }

    public ImageBankDirectoryTree getChildren(ImageBankDirectoryTree master,List<ImageBankDirectoryTree> imageBankDirectoryTrees){
        final Iterator<ImageBankDirectoryTree> iterator = imageBankDirectoryTrees.iterator();
        List<ImageBankDirectoryTree> childrens = null;
        while (iterator.hasNext()){
            final ImageBankDirectoryTree next = iterator.next();
            if(Objects.equals(next.getParentNodeId(),master.getId())){
                if(Objects.isNull(childrens)){
                    childrens = new ArrayList<>();
                }
                childrens.add(next);
                iterator.remove();
            }
        }
        if(Objects.isNull(childrens)){
            master.setChildren(Collections.emptyList());
        }else{
            childrens.forEach(o -> {
                getChildren(o,imageBankDirectoryTrees);
            });

            master.setChildren(childrens);
        }
        return master;
    }


    public  ImageBankDirectoryDto  checke(Long id){
        final Optional<ImageBankDirectoryDto> boostItemDTO1 = this.find(id);
        final ImageBankDirectoryDto boostItemDTO2 = boostItemDTO1.orElseThrow(() -> new IllegalOperationException("illegal.operation.exception", true));
        final String currentUserLogin = SecurityUtils.currentLogin();
        if (!boostItemDTO2.getLoginId().equals(currentUserLogin) && !currentUserLogin.equals(Constants.ANONYMOUS_USER)) {
            throw new IllegalOperationException("illegal.operation.exception",true);
        }
        return  boostItemDTO2;
    }

    @Override
    public List<ImageBankDirectoryDto> checke(List<Long> ids) {
        final List<ImageBankDirectoryDto> collect = ids.stream().map(this::checke).collect(Collectors.toList());
        return collect;
    }
}
