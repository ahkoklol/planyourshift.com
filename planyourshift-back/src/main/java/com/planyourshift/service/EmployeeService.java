package com.planyourshift.service;

import com.planyourshift.entity.Employee;
import com.planyourshift.entity.Store;
import com.planyourshift.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private StoreService storeService;

    public void createEmployee(Employee employee, String ownerId) {
        if (!Objects.equals(ownerId, employee.getOwnerId())) {
            log.info("Owner Id does not match");
            throw new IllegalArgumentException("Owner Id does not match");
        }
        Optional<Employee> existingEmployee = getEmployeeByEmail(employee.getEmail());
        if (existingEmployee.isPresent()) {
            log.info("Employee already exists");
            throw new IllegalArgumentException("Employee already exists");
        }
        employee.setEmployeeId(UUID.randomUUID().toString());
        employeeRepository.save(employee);
        log.info("Employee created");
    }

    /**
     * Helper to fetch an Employee by email
     * @param email the email of the employee
     * @return an Employee object if found, null otherwise
     */
    private Optional<Employee> getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }

    /**
     * Fetch an Employee by id
     * @param employeeId the id of the employee
     * @return an Employee object if found, null otherwise
     */
    public Optional<Employee> getEmployee(String employeeId) {
        return employeeRepository.findById(employeeId);
    }

    /**
     * Updates an Employee
     * @param employee the information to update
     * @param employeeId the id of the Employee
     */
    public void updateEmployee(Employee employee, String employeeId) {
        Optional<Employee> existingEmployee = getEmployee(employeeId);
        if (existingEmployee.isEmpty()) {
            log.info("Employee with id {} not found ", employeeId);
            throw new IllegalArgumentException("Employee with id " + employeeId + " not found");
        }
        employeeRepository.save(employee);
        log.info("Employee with id {} updated", employeeId);
    }

    /**
     * Deletes an Employee
     * @param employeeId the id of the Employee
     */
    public void deleteEmployee(String employeeId) {
        Optional<Employee> existingEmployee = getEmployee(employeeId);
        if (existingEmployee.isEmpty()) {
            log.info("Employee with id {} not found ", employeeId);
            throw new IllegalArgumentException("Employee with id " + employeeId + " not found");
        }
        employeeRepository.delete(existingEmployee.get());
        log.info("Employee with id {} deleted", employeeId);
    }

    /**
     * Fetch all Employees for a Store
     * @param ownerId the id of the Store
     * @return a list of Employees
     */
    public List<Employee> getAllEmployeesByOwner(String ownerId) {
        Optional<Store> existingStore = storeService.getStore(ownerId);
        if (existingStore.isEmpty()) {
            log.info("Store with id {} not found ", ownerId);
            throw new IllegalArgumentException("Store with id " + ownerId + " not found");
        }
        List<Employee> list = employeeRepository.findAllByOwnerId(ownerId);
        log.info("Found {} employees for store {}", list.size(), ownerId);
        return  list;
    }
}
