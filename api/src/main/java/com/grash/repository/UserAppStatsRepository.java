package com.grash.repository;

import com.grash.model.UserAppStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserAppStatsRepository extends JpaRepository<UserAppStats, Long>, JpaSpecificationExecutor<UserAppStats> {
    Optional<UserAppStats> findByUser_Id(Long userId);
}
