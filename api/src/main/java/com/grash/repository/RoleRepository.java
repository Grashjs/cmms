package com.grash.repository;

import com.grash.model.Role;
import com.grash.model.enums.RoleCode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    @Query("SELECT r from Role r where r.companySettings.company.id = :x ")
    Collection<Role> findByCompany_Id(@Param("x") Long id);

    @Query("SELECT r FROM Role r WHERE r.code !=com.grash.model.enums.RoleCode.USER_CREATED and r.companySettings is " +
            "null")
    List<Role> findDefaultRoles();
}
