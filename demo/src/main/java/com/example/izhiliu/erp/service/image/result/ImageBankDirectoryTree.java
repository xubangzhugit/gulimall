package com.izhiliu.erp.service.image.result;

import com.izhiliu.erp.service.image.dto.ImageBankDirectoryDto;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.criteria.Root;
import java.util.List;

/**
 * 树形结构
 *
 * @author Seriel
 * @create 2019-08-27 14:08
 **/
@Data
@Accessors( chain = true)
public class ImageBankDirectoryTree  {

    private  final  static ImageBankDirectoryTree  imageBankDirectoryTree =new ImageBankDirectoryTree().setId("0");

    public  final  static ImageBankDirectoryTree rootNode(){
           return imageBankDirectoryTree;
    }


    private String id;
    private String name;

    private String parentNodeId;

    /**
     *    是否是根目录
     */
    Boolean root;

    List<ImageBankDirectoryTree>   children;


    public final static ImageBankDirectoryTree imageBankDirectoryTree(ImageBankDirectoryDto imageBankDirectoryDto){
        final ImageBankDirectoryTree imageBankDirectoryTree = new ImageBankDirectoryTree();
        imageBankDirectoryTree.setId(imageBankDirectoryDto.getId().toString());
        imageBankDirectoryTree.setName(imageBankDirectoryDto.getName());
        imageBankDirectoryTree.setParentNodeId(imageBankDirectoryDto.getParentId().toString());
        return  imageBankDirectoryTree;
    }




}
