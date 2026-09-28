package com.store.seasoft.Repository;

import com.store.seasoft.Model.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    @EntityGraph(attributePaths = {"milestones", "manager"})
    List<Project> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    // Khach chi lay duoc du an cua chinh minh
    @EntityGraph(attributePaths = {"milestones", "manager"})
    Optional<Project> findByIdAndCustomerId(UUID id, UUID customerId);

    @EntityGraph(attributePaths = {"milestones", "manager", "customer"})
    Optional<Project> findWithDetailsById(UUID id);

    boolean existsByConsultationId(UUID consultationId);

    Optional<Project> findByConsultationId(UUID consultationId);

    @Query(value = "select nextval('project_code_seq')", nativeQuery = true)
    long nextCodeNumber();
}
