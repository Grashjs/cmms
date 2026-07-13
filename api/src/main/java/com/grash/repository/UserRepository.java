package com.grash.repository;

import com.grash.model.OwnUser;
import com.grash.model.enums.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<OwnUser, Long>, JpaSpecificationExecutor<OwnUser> {

    boolean existsByUsername(String username);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Optional<OwnUser> findByEmailIgnoreCase(String email);

    @Transactional
    void deleteByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    Collection<OwnUser> findByCompany_Id(Long id);

    Collection<OwnUser> findByLocation_Id(Long id);

    Optional<OwnUser> findByEmailIgnoreCaseAndCompany_Id(String email, Long companyId);

    Optional<OwnUser> findByIdAndCompany_Id(Long id, Long companyId);

    @Query("select u from OwnUser u where u.company.id=:id and u.role.code not in :roleCodes")
    Collection<OwnUser> findWorkersByCompany(@Param("id") Long id, @Param("roleCodes") List<RoleCode> roleCodes);

    @Query("select u from OwnUser u where u.createdViaSso=true and lower(u.email) like concat('%@',lower" +
            "(:emailDomain))")
    List<OwnUser> findBySSOCompany(@Param("emailDomain") String emailDomain);
}
