package com.grash.repository;

import com.grash.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface FileRepository extends JpaRepository<File, Long>, JpaSpecificationExecutor<File> {
    Collection<File> findByCompany_Id(Long id);

    List<File> findByIdIn(List<Long> ids);

    List<File> findByIdInAndCompany_Id(Collection<Long> ids, Long companyId);
}
