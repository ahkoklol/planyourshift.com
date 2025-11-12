package com.planyourshift;

import com.planyourshift.entity.*;
import com.planyourshift.llm.Mistral;
import com.planyourshift.service.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static java.time.DayOfWeek.*;

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

    private Mistral mistral;

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

    private StoreDaySchedule createStoreDaySchedule(String storeId, DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closedTime ) {
        StoreDaySchedule storeDaySchedule = new StoreDaySchedule();
        storeDaySchedule.setStoreId(storeId);
        storeDaySchedule.setDayOfWeek(dayOfWeek);
        storeDaySchedule.setOpenTime(openTime);
        storeDaySchedule.setCloseTime(closedTime);
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

    @BeforeEach
    void setup() {
        mistral = Mockito.mock(Mistral.class);
    }

    @Test
    void testFlow() {
        // create owner
        // owner 2 creates store
        // owner creates store day schedule for each day of week
        // owner adds employees to store
        // owner generates schedules

        Owner owner = ownerService.register(createOwner("testname", "testemail", "Testpassword1!"));
        employeeService.createEmployee(createEmployee(owner.getOwnerId(), "testfirstname1", "testlastname1", "testemail1", "", "sunday off", 45), owner.getOwnerId());
        employeeService.createEmployee(createEmployee(owner.getOwnerId(), "testfirstname2", "testlastname2", "testemail2", "", "", 45), owner.getOwnerId());
        employeeService.createEmployee(createEmployee(owner.getOwnerId(), "testfirstname3", "testlastname3", "testemail3", "", "wednesday off", 45), owner.getOwnerId());

        Store store1 = storeService.createStore(createStore(owner.getOwnerId(), "teststore1"), owner.getOwnerId());
        StoreDaySchedule monday1 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store1.getStoreId(), MONDAY, LocalTime.of(9, 0), LocalTime.of(20, 30)), store1.getStoreId());
        StoreDaySchedule tuesday1 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store1.getStoreId(), TUESDAY, LocalTime.of(9, 0), LocalTime.of(20, 30)), store1.getStoreId());
        StoreDaySchedule wednesday1 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store1.getStoreId(), WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(20, 30)), store1.getStoreId());
        StoreDaySchedule thursday1 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store1.getStoreId(), THURSDAY, LocalTime.of(9, 0), LocalTime.of(20, 30)), store1.getStoreId());
        StoreDaySchedule friday1 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store1.getStoreId(), FRIDAY, LocalTime.of(9, 0), LocalTime.of(22, 0)), store1.getStoreId());
        StoreDaySchedule saturday1 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store1.getStoreId(), SATURDAY, LocalTime.of(9, 0), LocalTime.of(22, 0)), store1.getStoreId());
        StoreDaySchedule sunday1 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store1.getStoreId(), SUNDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)), store1.getStoreId());

        Store store2 = storeService.createStore(createStore(owner.getOwnerId(), "teststore2"), owner.getOwnerId());
        StoreDaySchedule monday2 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store2.getStoreId(), MONDAY, LocalTime.of(9, 30), LocalTime.of(17, 30)), store2.getStoreId());
        StoreDaySchedule tuesday2 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store2.getStoreId(), TUESDAY, LocalTime.of(9, 30), LocalTime.of(17, 30)), store2.getStoreId());
        StoreDaySchedule wednesday2 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store2.getStoreId(), WEDNESDAY, LocalTime.of(9, 30), LocalTime.of(17, 30)), store2.getStoreId());
        StoreDaySchedule thursday2 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store2.getStoreId(), THURSDAY, LocalTime.of(9, 30), LocalTime.of(17, 30)), store2.getStoreId());
        StoreDaySchedule friday2 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store2.getStoreId(), FRIDAY, LocalTime.of(9, 30), LocalTime.of(17, 30)), store2.getStoreId());
        StoreDaySchedule saturday2 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store2.getStoreId(), SATURDAY, LocalTime.of(9, 30), LocalTime.of(17, 30)), store2.getStoreId());
        StoreDaySchedule sunday2 = storeDayScheduleService.createDaySchedule(createStoreDaySchedule(store2.getStoreId(), SUNDAY, null, null), store2.getStoreId());

        List<Shift> roster = schedulingService.solveRoaster(owner.getOwnerId());
        System.out.println("roster: " + roster);
    }
}
