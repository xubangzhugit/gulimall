package com.izhiliu.erp.service.item;

import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.domain.item.UserImage;
import com.izhiliu.erp.service.item.dto.UserImageDTO;
import com.izhiliu.erp.web.rest.item.param.BatchDownloadImageQO;

/**
 * Service Interface for managing ShopeeProductImage.
 */
public interface UserImageService extends IBaseService<UserImage, UserImageDTO> {

    String CACHE_ONE = "user-image-one";
    String CACHE_LIST = "user-image-list";
    String CACHE_PAGE = "user-image-page";
    String CACHE_PAGE$ = "user-image-page$";

    Boolean batchDownloadImage(BatchDownloadImageQO qo);

}
