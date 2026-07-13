package com.grash.repository;

import com.grash.model.CustomField;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomFieldRepository extends JpaRepository<CustomField, Long> {
}
