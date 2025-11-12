package com.planyourshift.service;

import com.planyourshift.entity.Shift;
import com.planyourshift.repository.ShiftRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ShiftService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    @Autowired
    private ShiftRepository shiftRepository;

    /**
     * Create a Shift
     * @param shift a Shift object
     */
    public void createShift(Shift shift) {
        shiftRepository.save(shift);
        log.info("Shift created");
    }

    public void saveAllShifts(List<Shift> shifts) {
        shiftRepository.saveAll(shifts);
        log.info("Shifts saved");
    }

    /**
     * Fetch a Shift by Employee, Store and Day
     * @param employeeId the id of the Employee
     * @param storeId the id of the Store
     * @param day the day
     * @return a Shift if found, null otherwise
     */
    public Optional<Shift> getShiftByEmployeeStoreAndDay(String employeeId, String storeId, DayOfWeek day) {
        return shiftRepository.findByEmployeeIdAndStoreIdAndDay(employeeId, storeId, day);
    }
}
