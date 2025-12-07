package com.grash.repository;

import com.grash.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByDemoTrue();

    void deleteAllByDemoTrue();

    Optional<Company> findBySubscription_Id(Long id);
}
