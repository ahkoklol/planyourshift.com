package com.planyourshift.repository;

import com.planyourshift.entity.StoreDaySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreDayScheduleRepository extends JpaRepository<StoreDaySchedule,String> {
    Optional<StoreDaySchedule> findByStoreId(String storeId);

    List<StoreDaySchedule> findAllByStoreId(String storeId);
}
