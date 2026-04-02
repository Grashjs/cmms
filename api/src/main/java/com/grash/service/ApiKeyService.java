package com.grash.service;

import com.grash.dto.apiKey.ApiKeyCriteria;
import com.grash.dto.apiKey.ApiKeyPatchDTO;
import com.grash.dto.apiKey.ApiKeyPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.ApiKeyMapper;
import com.grash.model.ApiKey;
import com.grash.model.ApiKey_;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.repository.ApiKeyRepository;
import com.grash.utils.Helper;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ApiKeyService {
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;

    public ApiKey create(@Valid ApiKeyPostDTO apiKeyReq, OwnUser user) {
        if (!user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        ApiKey apiKey =
                apiKeyMapper.fromPostDto(apiKeyReq);
        apiKey.setUser(user);

        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(key);
        try {
            apiKey.setCode(Helper.hashKey(code));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return apiKeyRepository.save(apiKey);
    }


    public List<ApiKey> getAll() {
        return apiKeyRepository.findAll();
    }

    public void delete(Long id) {
        apiKeyRepository.deleteById(id);
    }

    public Optional<ApiKey> findById(Long id) {
        return apiKeyRepository.findById(id);
    }

    public ApiKey update(Long id, ApiKeyPatchDTO apiKeyPatchDTO, OwnUser user) {
        ApiKey savedApiKey =
                apiKeyRepository.findById(id).orElseThrow(() -> new CustomException("Not found",
                        HttpStatus.NOT_FOUND));
        return apiKeyRepository.save(apiKeyMapper.updateApiKey(savedApiKey, apiKeyPatchDTO));
    }

    public Page<ApiKey> findByCriteria(ApiKeyCriteria criteria, Pageable pageable, OwnUser user) {
        Specification<ApiKey> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get(ApiKey_.company).get("id"), user.getCompany().getId()));

//            if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
//                predicates.add(cb.like(cb.lower(root.get(ApiKey_.recipientName)),
//                        "%" + criteria.getQuery().toLowerCase().trim() + "%"));
//            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return apiKeyRepository.findAll(specification, pageable);
    }
}
