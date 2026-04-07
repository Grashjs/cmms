package com.grash.repository;

import com.grash.model.GeneralPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

public interface GeneralPreferencesRepository extends JpaRepository<GeneralPreferences, Long> {

    Collection<GeneralPreferences> findByCompanySettings_Id(Long id);

    @Modifying
    @Transactional
    @Query(
            value = "UPDATE GeneralPreferences SET timeZone =:tz WHERE timeZone = 'MY_RANDOM_ZONE'")
    void updateTemporaryTimeZones(@Param("tz") String timezone);
}
