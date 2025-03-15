package com.grash.repository;

import com.grash.model.Asset;
import com.grash.model.UiConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface UiConfigurationRepository extends JpaRepository<UiConfiguration, Long> {

    Optional<UiConfiguration> findByCompanySettings_Id(Long id);
}

