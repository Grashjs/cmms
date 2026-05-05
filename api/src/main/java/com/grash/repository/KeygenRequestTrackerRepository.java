package com.grash.repository;

import com.grash.model.KeygenRequestTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

public interface KeygenRequestTrackerRepository extends JpaRepository<KeygenRequestTracker, Long> {

    Optional<KeygenRequestTracker> findByRequestDate(LocalDate date);

    @Modifying
    @Transactional
    @Query("UPDATE KeygenRequestTracker k SET k.requestCount = :count WHERE k.requestDate = :date")
    void updateRequestCount(@Param("date") LocalDate date, @Param("count") Integer count);
}
