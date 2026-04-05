package com.grash.repository;

import com.grash.model.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {
    
    Optional<Feature> findByCode(String code);
    
    List<Feature> findByIsActiveTrue();
    
    List<Feature> findByCategory(String category);
    
    boolean existsByCode(String code);
}
