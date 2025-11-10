package com.planyourshift.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "StoreOwner")
public class StoreOwner {

    @Id
    @Column(name = "store_owner_id")
    private String storeOwnerID;

    private String name;
    private String email;
    private String password;
}
