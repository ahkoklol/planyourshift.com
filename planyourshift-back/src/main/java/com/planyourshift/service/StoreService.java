package com.planyourshift.service;

import com.planyourshift.entity.Employee;
import com.planyourshift.entity.Owner;
import com.planyourshift.entity.Store;
import com.planyourshift.repository.OwnerRepository;
import com.planyourshift.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private OwnerService ownerService;

    /**
     * Creates a Store
     * @param store the Store to create
     * @param ownerId the id of the Owner
     * @return a Store object
     */
    public Store createStore(Store store, String ownerId) {
        if (!Objects.equals(ownerId, store.getOwnerId())) {
            log.error("Owner id and store owner id do not match");
            throw new IllegalArgumentException("Owner id and store owner id do not match");
        }
        Optional<Store> existingStore = getStoreByOwnerAndName(store.getName(), store.getOwnerId());
        if (existingStore.isPresent()) {
            log.error("Store with name {} already exists", store.getName());
            throw new IllegalArgumentException("Store with name " + store.getName() + " already exists");
        }
        Optional<Owner> existingOwner = ownerService.getOwner(store.getOwnerId());
        if (existingOwner.isEmpty()) {
            log.error("Owner with id {} does not exist", store.getOwnerId());
            throw new IllegalArgumentException("Owner with name " + store.getOwnerId() + " does not exist");
        }
        store.setStoreId(UUID.randomUUID().toString());
        return storeRepository.save(store);
    }

    /**
     * Helper to fetch a Store by owner id and store name
     * @param ownerId the id of the owner
     * @param storeName the store name
     * @return a Store if found, null otherwise
     */
    private Optional<Store> getStoreByOwnerAndName(String ownerId, String storeName) {
        return storeRepository.findByNameAndOwnerId(storeName, ownerId);
    }

    /**
     * Fetches all Stores for an Owner
     * @param ownerId the id of the owner
     * @return a list of Stores
     */
    public List<Store> getStores(String ownerId) {
        Optional<Owner> existingOwner = ownerService.getOwner(ownerId);
        if (existingOwner.isEmpty()) {
            log.error("Owner with id {} does not exist", ownerId);
            throw new IllegalArgumentException("Owner with name " + ownerId + " does not exist");
        }
        return storeRepository.findAllByOwnerId(ownerId);
    }

    /**
     * Fetch a Store
     * @param storeId the id of the Store
     * @return a Store object if found, null otherwise
     */
    public Optional<Store> getStore(String storeId) {
        return storeRepository.findById(storeId);
    }

    /**
     * Deletes a Store
     * @param storeId the id of the Store
     */
    public void deleteStore(String storeId) {
        Optional<Store> existingStore = getStore(storeId);
        if (existingStore.isEmpty()) {
            log.info("Store with id {} does not exist", storeId);
            throw new IllegalArgumentException("Store with id " + storeId + " does not exist");
        }
        storeRepository.deleteById(storeId);
        log.info("Store with id {} has been deleted", storeId);
    }
}
