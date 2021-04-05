package com.izhiliu.erp.service.image.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.izhiliu.erp.service.item.dto.BaseDto;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.validation.constraints.*;
import java.time.Instant;

/**
 * dto
 *
 * @author Seriel
 * @create 2019-08-27 9:58
 **/
@Data
@Accessors( chain = true)
public class ImageBankAddressDto implements BaseDto {

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonAlias("folderId")
    private  Long directoryId;

    @JsonAlias("filename")
    @Size(min = 2,max = 80,message = "shopee.image.name.length")
    private  String  name;

    @NotNull
    @NotEmpty
    @Size(min = 10,max = 80,message = "shopee.image.size.length")
    private  String  url;

    @NotNull
    @Min(value = 300L)
    @JsonAlias("size")
    private  Long imageSize;

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
}
