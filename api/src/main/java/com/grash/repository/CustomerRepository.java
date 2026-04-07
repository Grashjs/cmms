package com.grash.repository;

import com.grash.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    Collection<Customer> findByCompany_Id(Long id);

    List<Customer> findByNameIgnoreCaseAndCompany_Id(String name, Long companyId);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);
}
