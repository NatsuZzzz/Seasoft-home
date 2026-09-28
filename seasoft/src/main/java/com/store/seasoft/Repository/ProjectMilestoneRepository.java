package com.store.seasoft.Repository;

import com.store.seasoft.Model.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID> {

    Optional<ProjectMilestone> findByIdAndProjectId(UUID id, UUID projectId);
}
