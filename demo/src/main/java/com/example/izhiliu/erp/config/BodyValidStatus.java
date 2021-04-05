package com.izhiliu.erp.config;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.http.ResponseEntity;

@Data
@Builder
@Accessors(chain = true)
public class BodyValidStatus {

    private String code;

    private String title;

    private String field;

    private String type;


    public static BodyValidStatus myPackage(String message){
        return myPackage("500",message);
    }

    public static BodyValidStatus myPackage(String code,String message){
        return BodyValidStatus.builder()
                .code(code)
                .title(message)
                .field("parameter_check_exception")
                .type("Validated").build();
    }


    public static ResponseEntity myPackage(Integer code, String message){
        return  ResponseEntity.status(code).body(BodyValidStatus.builder()
                .code(String.valueOf(code))
                .title(message)
                .field("parameter_check_exception")
                .type("Validated").build());
    }
}
