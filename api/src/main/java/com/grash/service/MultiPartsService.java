package com.grash.service;

import com.grash.dto.MultiPartsPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.MultiPartsMapper;
import com.grash.model.Company;
import com.grash.model.MultiParts;
import com.grash.model.Part;
import com.grash.repository.MultiPartsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MultiPartsService {
    private final MultiPartsRepository multiPartsRepository;
    private final CompanyService companyService;
    private final MultiPartsMapper multiPartsMapper;
    private final EntityManager em;
    private final PartService partService;

    /**
     * @param company the caller's company. It cannot be read off the entity here: CompanyAudit
     *                fills that in {@code @PrePersist}, which runs inside the save below — long
     *                after the parts have to be resolved.
     */
    @Transactional
    public MultiParts create(MultiParts multiParts, Company company) {
        // Both entry points take parts as bare ids; see PartService.resolveRequestedParts for
        // why handing those to Hibernate as-is fails.
        setParts(multiParts, multiParts.getParts(), company.getId());
        MultiParts savedMultiParts = multiPartsRepository.saveAndFlush(multiParts);
        em.refresh(savedMultiParts);
        return savedMultiParts;
    }

    @Transactional
    public MultiParts update(Long id, MultiPartsPatchDTO multiPartsPatchDTO) {
        if (multiPartsRepository.existsById(id)) {
            MultiParts savedMultiParts = multiPartsRepository.findById(id).get();
            MultiParts patchedMultiParts = multiPartsMapper.updateMultiParts(savedMultiParts,
                    multiPartsPatchDTO);
            setParts(patchedMultiParts, multiPartsPatchDTO.getParts(),
                    patchedMultiParts.getCompany().getId());
            MultiParts updatedMultiParts = multiPartsRepository.saveAndFlush(patchedMultiParts);
            em.refresh(updatedMultiParts);
            return updatedMultiParts;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    private void setParts(MultiParts multiParts, Collection<Part> requested, Long companyId) {
        List<Part> parts = partService.resolveRequestedParts(requested, companyId);
        if (parts != null) {
            multiParts.getParts().clear();
            multiParts.getParts().addAll(parts);
        }
    }

    public Collection<MultiParts> getAll() {
        return multiPartsRepository.findAll();
    }

    public void delete(Long id) {
        multiPartsRepository.deleteById(id);
    }

    public Optional<MultiParts> findById(Long id) {
        return multiPartsRepository.findById(id);
    }

    public Collection<MultiParts> findByCompany(Long id) {
        return multiPartsRepository.findByCompany_Id(id);
    }

}

