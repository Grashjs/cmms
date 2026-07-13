package com.grash.repository;

import com.grash.model.GeneralPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface GeneralPreferencesRepository extends JpaRepository<GeneralPreferences, Long> {

    Collection<GeneralPreferences> findByCompanySettings_Id(Long id);
}
