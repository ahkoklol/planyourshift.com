package com.planyourshift.repository;

import com.planyourshift.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner,String> {
    Optional<Owner> findOwnerByEmail(String email);
}
