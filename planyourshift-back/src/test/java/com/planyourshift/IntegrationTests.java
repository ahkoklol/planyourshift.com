package com.planyourshift;

import com.planyourshift.entity.*;
import com.planyourshift.service.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalTime;
import java.util.List;

@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class IntegrationTests extends PostgresTestcontainer {

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private StoreDayScheduleService storeDayScheduleService;

    @Autowired
    private SchedulingService schedulingService;

    private Owner createOwner(String name, String email, String password) {
        Owner owner = new Owner();
        owner.setName(name);
        owner.setEmail(email);
        owner.setPassword(password);
        return owner;
    }

    private Store createStore(String ownerId, String name) {
        Store store = new Store();
        store.setName(name);
        store.setOwnerId(ownerId);
        return store;
    }

    private StoreDaySchedule createStoreDaySchedule(String storeId, String dayOfWeek, LocalTime openTime, LocalTime closedTime ) {
        StoreDaySchedule storeDaySchedule = new StoreDaySchedule();
        storeDaySchedule.setStoreId(storeId);
        storeDaySchedule.setDayOfWeek(dayOfWeek);
        storeDaySchedule.setOpenTime(openTime);
        storeDaySchedule.setOpenTime(closedTime);
        return storeDaySchedule;
    }

    private Employee createEmployee(String ownerId, String firstName, String lastName, String email, String constraints, String preferences, double requiredHours) {
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setOwnerId(ownerId);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setConstraints(constraints);
        employee.setPreferences(preferences);
        employee.setRequiredHours(requiredHours);
        return employee;
    }

    @Test
    void testFlow() {
        // create owner
        // owner creates store
        // owner creates store day schedule for each day of week
        // owner adds employees to store
        // owner generates schedules

        Owner owner = ownerService.register(createOwner("testname", "testemail", "testpassword"));
        Store store = storeService.createStore(createStore(owner.getOwnerId(), "teststore"), owner.getOwnerId());
        StoreDaySchedule monday = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store.getStoreId(), "MONDAY", LocalTime.of(9, 0), LocalTime.of(20, 30)), store.getStoreId());
        StoreDaySchedule tuesday = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store.getStoreId(), "MONDAY", LocalTime.of(9, 0), LocalTime.of(20, 30)), store.getStoreId());
        StoreDaySchedule wednesday = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store.getStoreId(), "MONDAY", LocalTime.of(9, 0), LocalTime.of(20, 30)), store.getStoreId());
        StoreDaySchedule thursday = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store.getStoreId(), "MONDAY", LocalTime.of(9, 0), LocalTime.of(20, 30)), store.getStoreId());
        StoreDaySchedule friday = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store.getStoreId(), "MONDAY", LocalTime.of(9, 0), LocalTime.of(22, 0)), store.getStoreId());
        StoreDaySchedule saturday = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store.getStoreId(), "MONDAY", LocalTime.of(9, 0), LocalTime.of(22, 0)), store.getStoreId());
        StoreDaySchedule sunday = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store.getStoreId(), "MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0)), store.getStoreId());
        employeeService.createEmployee(createEmployee(owner.getOwnerId(), "testfirstname", "testlastname", "testemail", "testconstraints", "testpreferences", 45), store.getOwnerId());
        //List<Shift> schedule = schedulingService.generateWeeklySchedule(store.getStoreId());
        //System.out.println("Schedule: " + schedule);
    }
}
