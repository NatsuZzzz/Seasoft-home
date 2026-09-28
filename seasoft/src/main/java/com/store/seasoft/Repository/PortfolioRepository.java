package com.store.seasoft.Repository;

import com.store.seasoft.Model.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<PortfolioItem, UUID> {

    List<PortfolioItem> findByPublishedTrueOrderBySortOrderAscCreatedAtDesc();

    List<PortfolioItem> findAllByOrderBySortOrderAscCreatedAtDesc();

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);
}
