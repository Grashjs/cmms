package com.grash.service;

import com.grash.dto.shiftConfiguration.ShiftConfigurationPatchDTO;
import com.grash.dto.shiftConfiguration.ShiftConfigurationPostDTO;
import com.grash.dto.shiftConfiguration.ShiftConfigurationShowDTO;
import com.grash.dto.shiftConfiguration.UserShiftDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.ShiftConfigurationMapper;
import com.grash.model.ShiftConfiguration;
import com.grash.model.User;
import com.grash.repository.ShiftConfigurationRepository;
import com.grash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftConfigurationService {
    private final ShiftConfigurationRepository shiftConfigurationRepository;
    private final ShiftConfigurationMapper shiftConfigurationMapper;
    private final UserRepository userRepository;
    private final UserService userService;

    public List<UserShiftDTO> getUsersWithShiftConfig(Collection<Long> userIds, Long companyId) {
        List<User> users;
        if (userIds != null && !userIds.isEmpty()) {
            users = userRepository.findByIdInAndCompany_Id(userIds, companyId);
        } else {
            users = userService.findWorkersByCompany(companyId).stream()
                    .filter(User::isEnabled)
                    .toList();
        }
        return users.stream()
                .map(user -> {
                    UserShiftDTO dto = new UserShiftDTO();
                    dto.setUserId(user.getId());
                    dto.setFullName(user.getFullName());
                    dto.setShiftConfiguration(
                            user.getShiftConfiguration() != null
                                    ? shiftConfigurationMapper.toShowDto(user.getShiftConfiguration())
                                    : null);
                    return dto;
                })
                .collect(Collectors.toList());
    }

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
