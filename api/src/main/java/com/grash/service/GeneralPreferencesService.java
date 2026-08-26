package com.grash.service;

import com.grash.dto.GeneralPreferencesPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.GeneralPreferencesMapper;
import com.grash.model.GeneralPreferences;
import com.grash.repository.GeneralPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GeneralPreferencesService {
    private final GeneralPreferencesRepository generalPreferencesRepository;
    private final GeneralPreferencesMapper generalPreferencesMapper;
    private static final Pattern HEX_PATTERN =
            Pattern.compile("^#([A-Fa-f0-9]{3,4}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");

    private static final Pattern URL_PATTERN =
            Pattern.compile("(?i)\\b(https?://|www\\.)\\S+\\b");

    public static boolean isValidColor(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        boolean isHex = HEX_PATTERN.matcher(text).matches();
        boolean isNotUrl = !URL_PATTERN.matcher(text).find();
        return isHex && isNotUrl;
    }

    public GeneralPreferences create(GeneralPreferences GeneralPreferences) {
        return generalPreferencesRepository.save(GeneralPreferences);
    }

    public GeneralPreferences update(Long id, GeneralPreferencesPatchDTO generalPreferencesPatchDTO) {
        if (generalPreferencesRepository.existsById(id)) {
            if (generalPreferencesPatchDTO.getColor() != null && !generalPreferencesPatchDTO.getColor().isBlank())
                if (!isValidColor(generalPreferencesPatchDTO.getColor())) {
                    throw new CustomException("Invalid color format", HttpStatus.BAD_REQUEST);
                }
            GeneralPreferences savedGeneralPreferences = generalPreferencesRepository.findById(id).get();
            return generalPreferencesRepository.save(generalPreferencesMapper.updateGeneralPreferences(savedGeneralPreferences, generalPreferencesPatchDTO));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<GeneralPreferences> getAll() {
        return generalPreferencesRepository.findAll();
    }

    public void delete(Long id) {
        generalPreferencesRepository.deleteById(id);
    }

    public Optional<GeneralPreferences> findById(Long id) {
        return generalPreferencesRepository.findById(id);
    }

    public Collection<GeneralPreferences> findByCompanySettings(Long id) {
        return generalPreferencesRepository.findByCompanySettings_Id(id);
    }
}
