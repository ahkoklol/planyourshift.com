package com.planyourshift.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "Store")
public class Store {

    @Id
    @Column(name = "store_id")
    private String storeId;

    @Column(name = "owner_id")
    private String ownerId;

    private String name;
}
