package com.izhiliu.erp.domain.enums.enumsclasses;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;


@Getter
@Setter
@Accessors(chain = true)
public class  LocalProductStatusClass{

    public Integer statusCode;

    public String statusName;

}
