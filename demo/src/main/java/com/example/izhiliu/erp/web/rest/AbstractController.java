package com.izhiliu.erp.web.rest;

import com.izhiliu.core.domain.common.BaseEntity;
import com.izhiliu.erp.service.image.CustomizeBaseService;
import com.izhiliu.erp.service.item.dto.BaseDto;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public abstract class AbstractController<T extends BaseEntity,A extends BaseDto,C extends CustomizeBaseService<T,A>> {

    protected Logger log= LoggerFactory.getLogger(getClass());

    @Autowired
    protected   C iBaseService;


//    @GetMapping("/list")
    public ResponseEntity<Collection<A>> list(){
        final Collection<A> list = iBaseService.list();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/query/{id}")
    public ResponseEntity<A> query(@PathVariable("id") @Validated  @NotNull Long id){
        final A aDto =  checke(id);
        return ResponseEntity.ok(aDto);
    }

    @PostMapping("")
    public ResponseEntity<Object> save(@RequestBody @Validated A aDto){
        if (log.isDebugEnabled()) {
            log.debug("REST request to save {} : {}", info(), aDto);
        }
        if (aDto.getId() != null) {
            throw new BadRequestAlertException("A new objcte cannot already have an ID", info(), "bad.request.alert.exception.create.shopee.product.sku.idexists");
        }
        final A save = iBaseService.save(aDto);
        return ResponseEntity.ok(save);
    }


    @DeleteMapping(value = "")
    public ResponseEntity<Boolean> delete(@RequestBody  @NotEmpty ArrayList<Long> deleteId){
        for (Long aLong : deleteId) {
            checke(aLong);
        }
        final boolean delete = iBaseService.delete(deleteId);
        return ResponseEntity.ok(delete);
    }

    @PutMapping("")
    public ResponseEntity<Boolean>  update(@RequestBody @Validated  A aDto){
        if (log.isDebugEnabled()) {
            log.debug("REST request to update  {} : {}", info(), aDto);
        }
        if (aDto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", info(), "bad.request.alert.exception.idnull");
        }
        checke(aDto.getId());
        final boolean update = iBaseService.update(aDto);
        return ResponseEntity.ok(update);
    }


    public abstract String info();

    public abstract A  checke(Long id);
}
