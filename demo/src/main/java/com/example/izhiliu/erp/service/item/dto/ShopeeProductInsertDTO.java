package com.izhiliu.erp.service.item.dto;


import com.izhiliu.erp.web.rest.item.vm.VariationVM;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;


/**
 * A DTO for the ShopeeProduct entity.
 */
@Data
@Accessors(chain = true)
public class ShopeeProductInsertDTO implements Serializable {

    private static final long serialVersionUID = -593020037192602931L;
    @Valid
    @NotNull
    ShopeeProductDTO shopeeProduct;

    @Valid
    @NotNull
    VariationVM param;
    public  interface Insert{

    }
    public  interface OneInsert{

    }
    public  interface NewInsert{

    }
    public  interface OneNewInsert{

    }

}
