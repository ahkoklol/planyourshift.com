package com.planyourshift.repository;

import com.planyourshift.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee,String> {
    List<Employee> findAllByOwnerId(String storeId);

    Optional<Employee> findByEmail(String email);
}
