package com.planyourshift.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "Owner")
public class Owner {

    @Id
    @Column(name = "owner_id")
    private String ownerId;

    private String name;
    private String email;
    private String password;
}
