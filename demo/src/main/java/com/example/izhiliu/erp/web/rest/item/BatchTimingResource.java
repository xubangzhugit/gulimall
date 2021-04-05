package com.izhiliu.erp.web.rest.item;


import com.izhiliu.erp.service.item.BatchBoostItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Validated
@RequestMapping("/api/timing-boost")
@RestController
public  class BatchTimingResource  {

    @Resource
    BatchBoostItemService batchBoostItemService;

    /**
     *
      * @param ids    这个是productId 哦~
     * @return
     */
    @PostMapping("/batch/product")
    public ResponseEntity<Boolean> timingTopping(@RequestBody @NotEmpty List<Long> ids){
       batchBoostItemService.timingTopping(ids);
        return ResponseEntity.ok(true);
    }

    /**
     *
     * @param deleteId   这个是productId 哦~
     * @return
     */
    @DeleteMapping("/batch/product")
    public ResponseEntity<Boolean>  deletetimingTopping(@RequestBody @NotEmpty List<Long> deleteId){
        batchBoostItemService.deletetimingTopping(deleteId);
        return ResponseEntity.ok(true);
    }




}
