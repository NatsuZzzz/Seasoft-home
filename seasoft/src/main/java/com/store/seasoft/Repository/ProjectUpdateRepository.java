package com.store.seasoft.Repository;

import com.store.seasoft.Model.ProjectUpdate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectUpdateRepository extends JpaRepository<ProjectUpdate, UUID> {

    @EntityGraph(attributePaths = {"author", "author.role"})
    List<ProjectUpdate> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    @EntityGraph(attributePaths = {"author", "author.role"})
    List<ProjectUpdate> findByProjectIdAndVisibleToCustomerTrueOrderByCreatedAtDesc(UUID projectId);
}
