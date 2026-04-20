package com.grash.service;

import com.grash.dto.{name}[-c].{name}[-C]Criteria;
import com.grash.dto.{name}[-c].{name}[-C]PatchDTO;
import com.grash.dto.{name}[-c].{name}[-C]PostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.{name}[-C]Mapper;
import com.grash.model.{name}[-C];
import com.grash.model.{name}[-C]_;
import com.grash.model.User;
import com.grash.repository.{name}[-C]Repository;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class {name}[-C]Service {
    private final {name}[-C]Repository {name}[-c]Repository;
    private final {name}[-C]Mapper {name}[-c]Mapper;

    public {name}[-C] create(@Valid {name}[-C]PostDTO {name}[-c]Req, User user) {
        {name}[-C] {name}[-c] =
                {name}[-c]Mapper.fromPostDto({name}[-c]Req);
        return {name}[-c]Repository.save({name}[-c]);
    }


    public List<{name}[-C]> getAll() {
        return {name}[-c]Repository.findAll();
    }

    public void delete(Long id) {
        {name}[-c]Repository.deleteById(id);
    }

    public Optional<{name}[-C]> findById(Long id) {
        return {name}[-c]Repository.findById(id);
    }

    public {name}[-C] update(Long id, {name}[-C]PatchDTO {name}[-c]PatchDTO, User user) {
        {name}[-C] saved{name}[-C] =
                {name}[-c]Repository.findById(id).orElseThrow(() -> new CustomException("Not found",
                        HttpStatus.NOT_FOUND));
        return {name}[-c]Repository.save({name}[-c]Mapper.update{name}[-C](saved{name}[-C], {name}[-c]PatchDTO));
    }

    public Page<{name}[-C]> findByCriteria({name}[-C]Criteria criteria, Pageable pageable, User user) {
        Specification<{name}[-C]> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get({name}[-C]_.company).get("id"), user.getCompany().getId()));

//            if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
//                predicates.add(cb.like(cb.lower(root.get({name}[-C]_.recipientName)),
//                        "%" + criteria.getQuery().toLowerCase().trim() + "%"));
//            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return {name}[-c]Repository.findAll(specification, pageable);
    }
}
