package com.store.seasoft.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(nullable = false, unique = true, length = 30)
    private String code; // CUSTOMER, STAFF, MANAGER, ADMIN

    @Column(nullable = false, length = 100)
    private String name;

    private String description;
}
