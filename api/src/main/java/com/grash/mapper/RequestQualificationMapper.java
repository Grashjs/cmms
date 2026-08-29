package com.grash.mapper;

import com.grash.dto.triage.QualificationCandidateShowDTO;
import com.grash.dto.triage.RequestQualificationShowDTO;
import com.grash.model.RequestQualification;
import com.grash.model.RequestQualificationCandidate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.List;

/**
 * Entity to DTO for triage results. Read-only in one direction: a qualification is written by the
 * matcher and decided on through {@code RequestQualificationService}, never posted by a client, so
 * there is nothing to map inbound.
 */
@Mapper(componentModel = "spring", uses = {AssetMapper.class, UserMapper.class})
public interface RequestQualificationMapper {

    @Mapping(target = "requestId", source = "request.id")
    RequestQualificationShowDTO toShowDto(RequestQualification model);

    QualificationCandidateShowDTO toCandidateDto(RequestQualificationCandidate model);

    /**
     * Splits the stored terms back into a list. The entity keeps them as one string because
     * nothing queries them and a child table for three words would be ceremony; the DTO hands
     * them over as a list because the frontend renders one chip per word and should not have to
     * know which separator the backend picked.
     */
    default List<String> matchedTermsToList(String matchedTerms) {
        if (matchedTerms == null || matchedTerms.isBlank()) return List.of();
        return Arrays.stream(matchedTerms.split(","))
                .map(String::trim)
                .filter(term -> !term.isEmpty())
                .toList();
    }
}
