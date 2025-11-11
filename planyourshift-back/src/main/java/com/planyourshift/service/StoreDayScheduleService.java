package com.planyourshift.service;

import com.planyourshift.entity.Store;
import com.planyourshift.entity.StoreDaySchedule;
import com.planyourshift.repository.StoreDayScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StoreDayScheduleService {

    private static final Logger log = LoggerFactory.getLogger(StoreDayScheduleService.class);

    @Autowired
    StoreDayScheduleRepository storeDayScheduleRepository;

    @Autowired
    StoreService storeService;

    /**
     * Creates a StoreDaySchedule for a Store
     * @param storeDaySchedule a StoreDaySchedule object
     * @param storeId the id of the store
     */
    public void CreateDaySchedule(StoreDaySchedule storeDaySchedule, String storeId) {
        Optional<Store> existingStore = storeService.getStore(storeId);
        if (existingStore.isEmpty()) {
            log.info("Store {} not found", storeId);
            throw new IllegalArgumentException("Store " + storeId + " not found");
        }
        storeDaySchedule.setStoreDayScheduleId(UUID.randomUUID().toString());
        storeDayScheduleRepository.save(storeDaySchedule);
        log.info("DaySchedule created for store {}", storeId);
    }

    /**
     * Fetch a StoreDaySchedule by store
     * @param storeId the id of the store
     * @return a StoreDaySchedule if found, null otherwise
     */
    public Optional<StoreDaySchedule> getStoreDaySchedule(String storeId) {
        return storeDayScheduleRepository.findByStoreId(storeId);
    }

    /**
     * Fetch all StoreDaySchedule by store
     * @param storeId the id of the store
     * @return a list of StoreDaySchedules
     */
    public List<StoreDaySchedule> getStoreDaySchedules(String storeId) {
        return storeDayScheduleRepository.findAllByStoreId(storeId);
    }
}
