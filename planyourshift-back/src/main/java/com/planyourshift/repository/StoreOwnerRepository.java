package com.planyourshift.repository;

import com.planyourshift.entity.StoreOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreOwnerRepository extends JpaRepository<StoreOwner,String> {
    Optional<StoreOwner> findStoreOwnerByEmail(String email);
}
