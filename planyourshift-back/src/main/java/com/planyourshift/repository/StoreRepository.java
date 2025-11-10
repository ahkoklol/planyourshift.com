package com.planyourshift.repository;

import com.planyourshift.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store,String> {
    Optional<Store> findByNameAndOwnerId(String name, String ownerId);

    List<Store> findAllByOwnerId(String ownerId);
}
