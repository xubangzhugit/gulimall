package com.izhiliu.erp.web.rest.image;

import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.util.Constants;
import com.izhiliu.erp.domain.image.ImageBankDirectory;
import com.izhiliu.erp.domain.item.BoostItem;
import com.izhiliu.erp.service.image.ImageBankDirectoryService;
import com.izhiliu.erp.service.image.dto.ImageBankDirectoryDto;
import com.izhiliu.erp.service.image.result.ImageBankDirectoryTree;
import com.izhiliu.erp.service.item.BatchBoostItemService;
import com.izhiliu.erp.service.item.dto.BoostItemDTO;
import com.izhiliu.erp.web.rest.AbstractController;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Validated
@RequestMapping("/api/image-bank-directory")
@RestController
public  class ImageBankDirectoryResource extends AbstractController<ImageBankDirectory, ImageBankDirectoryDto, ImageBankDirectoryService> {


    @Override
    public ResponseEntity<Object> save(@RequestBody @Validated ImageBankDirectoryDto aDto) {
        if (log.isDebugEnabled()) {
            log.debug("REST request to save {} : {}", info(), aDto);
        }
        if (aDto.getId() != null) {
            throw new BadRequestAlertException("A new objcte cannot already have an ID", info(), "bad.request.alert.exception.create.shopee.product.sku.idexists");
        }
        return ResponseEntity.ok(iBaseService.insert(aDto));
    }

    @Override
    public ResponseEntity<Boolean> update(@RequestBody @Validated  ImageBankDirectoryDto aDto) {
        if (log.isDebugEnabled()) {
            log.debug("REST request to update  {} : {}", info(), aDto);
        }
        if (aDto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", info(), "bad.request.alert.exception.idnull");
        }
        final Long parentId = checke(aDto.getId()).getParentId();
        if(parentId==0){
            throw new IllegalOperationException("illegal.operation.exception",true);
        }
        final boolean update = iBaseService.update(aDto);
        return ResponseEntity.ok(update);
    }

    @Override
    public ResponseEntity<Boolean> delete(@RequestBody @NotEmpty ArrayList<Long> deleteId) {
        for (Long aLong : deleteId) {
            final Long parentId =  checke(aLong).getParentId();
            if(parentId==0){
                throw new IllegalOperationException("illegal.operation.exception",true);
            }
        }
        final boolean delete = iBaseService.delete(deleteId);
        return ResponseEntity.ok(delete);
    }

    @GetMapping("")
    public ResponseEntity<List<ImageBankDirectoryTree>> listImageBankDirectoryTree(){

        return ResponseEntity.ok(iBaseService.selectByLoginid());
    }


    @Override
    public String info() {
        return getClass().getName();
    }

    public  ImageBankDirectoryDto  checke(Long id){
        final Optional<ImageBankDirectoryDto> boostItemDTO1 = iBaseService.find(id);
        final ImageBankDirectoryDto boostItemDTO2 = boostItemDTO1.orElseThrow(() -> new IllegalOperationException("illegal.operation.exception", true));
        final String currentUserLogin = SecurityUtils.currentLogin();
        if (!boostItemDTO2.getLoginId().equals(currentUserLogin) && !currentUserLogin.equals(Constants.ANONYMOUS_USER)) {
            throw new IllegalOperationException("illegal.operation.exception",true);
        }
        return  boostItemDTO2;
    }
}
