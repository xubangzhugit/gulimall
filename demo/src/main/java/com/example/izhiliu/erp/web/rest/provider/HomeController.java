package com.izhiliu.erp.web.rest.provider;


import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.web.rest.provider.dto.HomeTodoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HomeController {

    @Autowired
    ShopeeProductService shopeeProductService;

    @GetMapping("/home/todo/item")
    public ResponseEntity<HomeTodoVO> todo() {
        return ResponseEntity.ok().body(shopeeProductService.todo());
    }


}
