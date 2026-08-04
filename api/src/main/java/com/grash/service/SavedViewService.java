package com.grash.service;

import com.grash.dto.savedView.SavedViewPatchDTO;
import com.grash.dto.savedView.SavedViewPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.SavedViewMapper;
import com.grash.model.SavedView;
import com.grash.model.User;
import com.grash.model.enums.SavedViewEntityType;
import com.grash.repository.SavedViewRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class SavedViewService {

    private final SavedViewRepository savedViewRepository;
    private final SavedViewMapper savedViewMapper;

    public List<SavedView> findVisible(User user, SavedViewEntityType entityType) {
        return savedViewRepository.findVisible(user.getCompany().getId(), entityType, user.getId());
    }

    public Optional<SavedView> findById(Long id) {
        return savedViewRepository.findById(id);
    }

    public SavedView create(@Valid SavedViewPostDTO dto, User user) {
        SavedView savedView = savedViewMapper.fromPostDto(dto);
        savedView.setOwner(user);
        // company is set by CompanyAudit.beforePersist from the authenticated user
        return savedViewRepository.save(savedView);
    }

    public SavedView update(Long id, SavedViewPatchDTO dto, User user) {
        SavedView savedView = findById(id)
                .orElseThrow(() -> new CustomException("Saved view not found", HttpStatus.NOT_FOUND));
        assertCanEdit(savedView, user);
        return savedViewRepository.save(savedViewMapper.updateSavedView(savedView, dto));
    }

    public void delete(Long id, User user) {
        SavedView savedView = findById(id)
                .orElseThrow(() -> new CustomException("Saved view not found", HttpStatus.NOT_FOUND));
        assertCanEdit(savedView, user);
        savedViewRepository.deleteById(id);
    }

    /**
     * Read access is separate from edit access: a shared view is readable by the whole company
     * but only its owner (or the company owner) may change it. {@code CompanyAudit.afterLoad}
     * has already rejected anything belonging to another company by the time we get here, so
     * this only has to separate private from shared.
     */
    public void assertCanRead(SavedView savedView, User user) {
        if (!savedView.isVisibleTo(user)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
    }

    private void assertCanEdit(SavedView savedView, User user) {
        if (!savedView.canBeEditedBy(user)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
    }
}
