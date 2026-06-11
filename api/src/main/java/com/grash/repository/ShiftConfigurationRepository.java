package com.grash.repository;

import com.grash.model.ShiftConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ShiftConfigurationRepository extends JpaRepository<ShiftConfiguration, Long>,
        JpaSpecificationExecutor<ShiftConfiguration> {
}
