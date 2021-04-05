package com.izhiliu.erp.web.rest.image.param;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.izhiliu.erp.service.image.ImageBankAddressService;
import com.izhiliu.erp.service.image.ImageBankDirectoryService;
import com.izhiliu.erp.web.rest.image.validation.MovingImageValidation;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Seriel
 * @create 2019-08-28 10:27
 **/
@Data
@Accessors( chain = true)
public class MovingPictureCondition {

    @JsonAlias("imageIds")
    @NotNull
    @NotEmpty
    @MovingImageValidation(check = ImageBankAddressService.class)
    private List<Long> imageIds;

    @JsonAlias("folderId")
    @NotNull
    @MovingImageValidation(check = ImageBankDirectoryService.class)
    private  Long   directoryId;
}
