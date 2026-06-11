package com.grash.service;

import com.grash.dto.shiftConfiguration.ShiftConfigurationPatchDTO;
import com.grash.dto.shiftConfiguration.ShiftConfigurationPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.ShiftConfigurationMapper;
import com.grash.model.ShiftConfiguration;
import com.grash.model.User;
import com.grash.repository.ShiftConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShiftConfigurationService {
    private final ShiftConfigurationRepository shiftConfigurationRepository;
    private final ShiftConfigurationMapper shiftConfigurationMapper;

    public Optional<ShiftConfiguration> findById(Long id) {
        return shiftConfigurationRepository.findById(id);
    }


    @Transactional
    public ShiftConfiguration create(ShiftConfigurationPostDTO dto, User user) {
        ShiftConfiguration shiftConfiguration = shiftConfigurationMapper.fromPostDto(dto);
        ShiftConfiguration saved = shiftConfigurationRepository.save(shiftConfiguration);
        return saved;
    }

    @Transactional
    public ShiftConfiguration update(User user, ShiftConfigurationPatchDTO dto) {
        ShiftConfiguration shiftConfiguration = Objects.requireNonNullElseGet(user.getShiftConfiguration(),
                () -> {
                    ShiftConfiguration newShift = shiftConfigurationRepository.save(new ShiftConfiguration());
                    user.setShiftConfiguration(newShift);
                    return newShift;
                });
        return shiftConfigurationRepository.save(
                shiftConfigurationMapper.updateShiftConfiguration(shiftConfiguration, dto));

    }

    @Transactional
    public void delete(Long id) {
        shiftConfigurationRepository.deleteById(id);
    }

    public ShiftConfiguration save(ShiftConfiguration shiftConfiguration) {
        return shiftConfigurationRepository.save(shiftConfiguration);
    }
}
