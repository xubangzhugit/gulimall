package com.izhiliu.erp.service.item.cache;


import java.util.Optional;

public interface RedisService<T,C> {

    default  Optional<T> pull(Optional<C> supplier) throws IllegalAccessException {
       return  Optional.ofNullable(doPull(supplier));
    }
    T doPull(Optional<C> supplier) throws IllegalAccessException;



    default   Optional<T> push(Optional<C> supplier){
        return  Optional.ofNullable(doPush(supplier));
    }
    T doPush(Optional<C> supplier);



    default Optional<T> run(Optional<C> supplier) throws IllegalAccessException {
        Optional<T> pull = pull(supplier);
        if(!pull.isPresent()){
            lock();
            pull  = push(supplier);
            unlock();
        }
        return  pull;
    }


    default  void  lock() throws IllegalAccessException {
        lock("default");
    }

    default  void  lock(String key) throws IllegalAccessException {
    }

    default  void  unlock(){
        unlock("default");
    }

    default  void  unlock(String key){
    }
}
