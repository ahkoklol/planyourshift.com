package com.planyourshift.controller;

import com.planyourshift.entity.StoreOwner;
import com.planyourshift.repository.StoreOwnerRepository;
import com.planyourshift.service.StoreOwnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/owner")
public class StoreOwnerController {

    private static final Logger log = LoggerFactory.getLogger(StoreOwnerController.class);

    @Autowired
    private StoreOwnerService storeOwnerService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody StoreOwner storeOwner){
        storeOwnerService.register(storeOwner);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody StoreOwner storeOwner){
        boolean result  = storeOwnerService.login(storeOwner);
        if(!result){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
