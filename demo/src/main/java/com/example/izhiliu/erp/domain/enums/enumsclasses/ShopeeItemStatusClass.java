package com.izhiliu.erp.domain.enums.enumsclasses;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class ShopeeItemStatusClass {

    public Integer shopeeItemStatusCode;
    public String shopeeItemStatusName;
}
