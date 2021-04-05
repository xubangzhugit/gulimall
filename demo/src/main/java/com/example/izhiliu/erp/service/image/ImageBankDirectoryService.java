package com.izhiliu.erp.service.image;

import com.izhiliu.erp.domain.image.ImageBankDirectory;
import com.izhiliu.erp.service.image.dto.ImageBankDirectoryDto;
import com.izhiliu.erp.service.image.result.ImageBankDirectoryTree;

import java.util.List;

public interface ImageBankDirectoryService extends CustomizeBaseService<ImageBankDirectory, ImageBankDirectoryDto> {
    ImageBankDirectoryDto insert(ImageBankDirectoryDto aDto);

    List<ImageBankDirectoryTree> selectByLoginid();

    ImageBankDirectory getRootImageBankDirectories(String currentLogin);

}
