package com.store.seasoft.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff_profiles")
@Getter
@Setter
public class StaffProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "employee_code", unique = true)
    private String employeeCode;

    private String department; // Design, Dev, Sales, CSKH...
    private String position;

    @Column(name = "hire_date")
    private LocalDate hireDate;
}