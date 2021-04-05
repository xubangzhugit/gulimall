package com.izhiliu.erp.web.rest.image;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.util.Constants;
import com.izhiliu.erp.domain.image.ImageBankAddress;
import com.izhiliu.erp.service.image.ImageBankAddressService;
import com.izhiliu.erp.service.image.dto.ImageBankAddressDto;
import com.izhiliu.erp.service.image.dto.ImageBankCapacityCacheObejct;
import com.izhiliu.erp.web.rest.AbstractController;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import com.izhiliu.erp.web.rest.image.param.ImageBankAddressCondition;
import com.izhiliu.erp.web.rest.image.param.MovingPictureCondition;
import com.izhiliu.erp.web.rest.image.validation.MovingImageValidation;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Validated
@RequestMapping("/api/image-bank-address")
@RestController
public  class ImageBankAddressResource extends AbstractController<ImageBankAddress, ImageBankAddressDto, ImageBankAddressService> {


    @GetMapping("")
    public ResponseEntity<List<ImageBankAddressDto>> listImageBankDirectoryTree(@Validated  ImageBankAddressCondition imageBankAddressCondition){

        final IPage<ImageBankAddressDto> imageBankAddressDtos = iBaseService.selectByLoginid(imageBankAddressCondition);
        return new ResponseEntity(imageBankAddressDtos.getRecords()
                , PaginationUtil.generatePaginationHttpHeaders(imageBankAddressDtos, "/api/image-bank-address")
                , HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Boolean> delete(@RequestBody @NotEmpty ArrayList<Long> deleteId) {
        iBaseService.remove(deleteId);
        return  ResponseEntity.ok(Boolean.TRUE);
    }

    @GetMapping("/image/size")
    public ResponseEntity<ImageBankCapacityCacheObejct> selectImageSizeByUserId(){
        return  ResponseEntity.ok(iBaseService.selectImageSizeByUserId());
    }


    @PatchMapping("")
    public ResponseEntity<Boolean> movingAndManagementPicture(@Validated @RequestBody MovingPictureCondition movingPicture){
        iBaseService.movingAndManagementPicture(movingPicture);
      return  ResponseEntity.ok(Boolean.TRUE);
    }



    @Override
    public ResponseEntity<Object> save(@RequestBody @Validated ImageBankAddressDto aDto) {
        if (log.isDebugEnabled()) {
            log.debug("REST request to save {} : {}", info(), aDto);
        }
        if (aDto.getId() != null) {
            throw new BadRequestAlertException("A new objcte cannot already have an ID", info(), "bad.request.alert.exception.create.shopee.product.sku.idexists");
        }
        return ResponseEntity.ok(iBaseService.insert(aDto));
    }



    @Override
    public String info() {
        return getClass().getName();
    }

    public  ImageBankAddressDto  checke(Long id){
        final Optional<ImageBankAddressDto> boostItemDTO1 = iBaseService.find(id);
        final ImageBankAddressDto boostItemDTO2 = boostItemDTO1.orElseThrow(() -> new IllegalOperationException("illegal.operation.exception", true));
        final String currentUserLogin = SecurityUtils.currentLogin();
        if (!boostItemDTO2.getLoginId().equals(currentUserLogin) && !currentUserLogin.equals(Constants.ANONYMOUS_USER)) {
            throw new IllegalOperationException("illegal.operation.exception",true);
        }
        return  boostItemDTO2;
    }
}
