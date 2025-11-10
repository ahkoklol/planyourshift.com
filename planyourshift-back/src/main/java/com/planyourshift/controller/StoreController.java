package com.planyourshift.controller;

import com.planyourshift.entity.Store;
import com.planyourshift.service.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores")
public class StoreController {

    private static final Logger log = LoggerFactory.getLogger(StoreController.class);

    @Autowired
    private StoreService storeService;

    @PostMapping("/{ownerId}")
    public ResponseEntity<Store> createStore(@RequestBody Store store, @PathVariable String ownerId) {
        Store result = storeService.createStore(store, ownerId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<List<Store>> getStores(@PathVariable String ownerId) {
        List<Store> result = storeService.getStores(ownerId);
        log.info("Found {} stores for owner id {}", result.size(), ownerId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(@PathVariable String storeId) {
        storeService.deleteStore(storeId);
        return ResponseEntity.ok().build();
    }

}
