package com.izhiliu.erp.service.image.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
@Setter
@Accessors( chain = true)
public class ImageBankCapacityCacheObejct {


    public   final  static ImageBankCapacityCacheObejct IMAGE_BANK_CAPACITY_CACHE_OBEJCT =new ImageBankCapacityCacheObejct().setTotal(Integer.MAX_VALUE).setUsed(Integer.MAX_VALUE);

    /**
     * redis  总量
     */
    private long total;
    /**
     *  已使用
     */
    private long used;

    /**
     * key
     */
    private String loginId;
    /**
     * size
     */
    private Long  userId;


    public long remainingMemory(){
         long remaining = total - used;
        if(remaining<0){
            remaining = 0;
        }
        return remaining;
    }

    public boolean isOk(long addMemory, Consumer<Long> aftarConsume){
        final long newRemainingMemory  = remainingMemory() - addMemory;
        if(newRemainingMemory<0){
            return false;
        }
        aftarConsume.accept(addMemory);
        return  true;
    }

}
