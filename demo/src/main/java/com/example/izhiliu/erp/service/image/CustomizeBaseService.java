package com.izhiliu.erp.service.image;

import com.izhiliu.core.domain.common.IBaseService;

import java.util.List;

/**
 * 公共接口
 *
 * @author Seriel
 * @create 2019-08-28 17:49
 **/
public interface CustomizeBaseService<T,C> extends IBaseService<T,C> {

     C checke(Long id);

     List<C> checke(List<Long> ids);

     default  Long getId(){
          return  null;
     };
}
