package com.grash.repository;

import com.grash.model.UserFeaturePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFeaturePermissionRepository extends JpaRepository<UserFeaturePermission, Long> {
    
    List<UserFeaturePermission> findByUserId(Long userId);
    
    Optional<UserFeaturePermission> findByUserIdAndFeatureCode(Long userId, String featureCode);
    
    @Query("SELECT ufp FROM UserFeaturePermission ufp WHERE ufp.user.id = :userId AND ufp.isEnabled = true")
    List<UserFeaturePermission> findEnabledByUserId(@Param("userId") Long userId);
    
    void deleteByUserIdAndFeatureCode(Long userId, String featureCode);
    
    boolean existsByUserIdAndFeatureCode(Long userId, String featureCode);
}
