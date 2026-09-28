package com.store.seasoft.Repository;

import com.store.seasoft.Model.ConsultationRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ConsultationRepository
        extends JpaRepository<ConsultationRequest, UUID>, JpaSpecificationExecutor<ConsultationRequest> {

    @EntityGraph(attributePaths = "assignedStaff")
    List<ConsultationRequest> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
