package com.izhiliu.erp.service.image.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.izhiliu.erp.service.item.dto.BaseDto;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * @author Seriel
 * @create 2019-08-27 9:59
 **/
@Data
@Accessors(chain = true)
public class ImageBankDirectoryDto  implements BaseDto {


    @JsonSerialize(using = ToStringSerializer.class)
    @JsonAlias("superiorId")
    @NotNull(groups = {Add.class})
    @NotBlank(groups = {Add.class})
    private  Long parentId;

    @NotNull
    @NotBlank
    @Size(min = 2,max = 80,message = "shopee.image.name.length")
    private  String  name;

    private  Integer  level;

    private  String LoginId;

    /**
     *   客优云 的 id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private   Long  kkyId;

    private  Integer status;




    private Instant gmtCreate;

    private Instant gmtModified;

    private String feature;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }



    interface  Add{

    }
    interface  Update{

    }

    public  void upLevel(int parentLevel){
        level = parentLevel + 1;
    }
}
