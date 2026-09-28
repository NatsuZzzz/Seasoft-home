package com.store.seasoft.Repository;

import com.store.seasoft.Model.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, UUID> {

    boolean existsByEmployeeCodeAndUserIdNot(String employeeCode, UUID userId);

    boolean existsByEmployeeCode(String employeeCode);
}
