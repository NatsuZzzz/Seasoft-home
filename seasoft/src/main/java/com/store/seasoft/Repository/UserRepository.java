package com.store.seasoft.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.store.seasoft.Model.User;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u from User u where u.role.code in :codes and u.status = com.store.seasoft.Model.UserStatus.ACTIVE order by u.fullName")
    List<User> findActiveByRoleCodes(@Param("codes") Collection<String> codes);

    long countByRoleCode(String code);
}
