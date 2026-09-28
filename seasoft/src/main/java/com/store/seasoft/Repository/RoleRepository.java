package com.store.seasoft.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.store.seasoft.Model.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByCode(String code);

}
