package com.grash.service;

import com.grash.dto.MultiPartsPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.MultiPartsMapper;
import com.grash.model.MultiParts;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.repository.MultiPartsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MultiPartsService {
    private final MultiPartsRepository multiPartsRepository;
    private final MultiPartsMapper multiPartsMapper;
    private final EntityManager em;

    @Transactional
    public MultiParts create(MultiParts multiPartsReq, User user) {
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)) {
            MultiParts savedMultiParts = multiPartsRepository.saveAndFlush(multiPartsReq);
            em.refresh(savedMultiParts);
            return savedMultiParts;
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }


    public Collection<MultiParts> getAll(User user) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)) {
                return findByCompany(user.getCompany().getId()).stream()
                        .filter(multiPart -> multiPart.canBeViewedBy(user))
                        .collect(Collectors.toList());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else return multiPartsRepository.findAll();
    }

    public MultiParts getById(Long id, User user) {
        Optional<MultiParts> optionalMultiParts = multiPartsRepository.findById(id);
        if (optionalMultiParts.isPresent()) {
            MultiParts savedMultiParts = optionalMultiParts.get();
            if (savedMultiParts.canBeViewedBy(user)) {
                return savedMultiParts;
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public MultiParts patch(Long id, MultiPartsPatchDTO multiParts, User user) {
        Optional<MultiParts> optionalMultiParts = multiPartsRepository.findById(id);
        if (optionalMultiParts.isPresent()) {
            MultiParts savedMultiParts = optionalMultiParts.get();
            if (savedMultiParts.canBeEditedBy(user)) {
                MultiParts updatedMultiParts = multiPartsRepository.saveAndFlush(
                        multiPartsMapper.updateMultiParts(savedMultiParts, multiParts));
                em.refresh(updatedMultiParts);
                return updatedMultiParts;
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("MultiParts not found", HttpStatus.NOT_FOUND);
    }

    public Collection<MultiParts> findByCompany(Long id) {
        return multiPartsRepository.findByCompany_Id(id);
    }

    @Transactional
    public void deleteByIdAndUser(Long id, User user) {
        Optional<MultiParts> optionalMultiParts = multiPartsRepository.findById(id);
        if (optionalMultiParts.isPresent()) {
            MultiParts savedMultiParts = optionalMultiParts.get();
            if (savedMultiParts.canBeDeletedBy(user)) {
                multiPartsRepository.deleteById(id);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("MultiParts not found", HttpStatus.NOT_FOUND);
    }
}