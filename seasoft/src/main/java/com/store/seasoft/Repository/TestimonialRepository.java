package com.store.seasoft.Repository;

import com.store.seasoft.Model.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestimonialRepository extends JpaRepository<Testimonial, UUID> {

    List<Testimonial> findByPublishedTrueOrderBySortOrderAscCreatedAtDesc();

    List<Testimonial> findAllByOrderBySortOrderAscCreatedAtDesc();
}
