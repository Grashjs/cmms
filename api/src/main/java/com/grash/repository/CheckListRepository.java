package com.grash.repository;

import com.grash.model.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface CheckListRepository extends JpaRepository<Checklist, Long> {
    Collection<Checklist> findByCompanySettings_Id(Long id);
}
