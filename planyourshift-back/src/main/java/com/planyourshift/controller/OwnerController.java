package com.planyourshift.controller;

import com.planyourshift.entity.Owner;
import com.planyourshift.service.OwnerService;
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
public class OwnerController {

    private static final Logger log = LoggerFactory.getLogger(OwnerController.class);

    @Autowired
    private OwnerService ownerService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody Owner storeOwner){
        ownerService.register(storeOwner);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody Owner storeOwner){
        boolean result  = ownerService.login(storeOwner);
        if(!result){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
