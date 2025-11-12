package com.planyourshift.repository;

import com.planyourshift.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift,String> {
    Optional<Shift> findByEmployeeIdAndStoreIdAndDay(String employeeId, String storeId, DayOfWeek day);
}
