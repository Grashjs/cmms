package com.grash.service;

import com.grash.dto.ReadingPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.ReadingMapper;
import com.grash.model.OwnUser;
import com.grash.model.Reading;
import com.grash.model.enums.RoleType;
import com.grash.repository.ReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReadingService {
    private final ReadingRepository readingRepository;
    private final ReadingMapper readingMapper;
    private MeterService meterService;

    @Autowired
    public void setDeps(@Lazy MeterService meterService
    ) {
        this.meterService = meterService;
    }

    public Reading create(Reading Reading) {
        return readingRepository.save(Reading);
    }

    public Reading update(Long id, ReadingPatchDTO reading) {
        if (readingRepository.existsById(id)) {
            Reading savedReading = readingRepository.findById(id).get();
            return readingRepository.save(readingMapper.updateReading(savedReading, reading));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Reading> getAll() {
        return readingRepository.findAll();
    }

    public void delete(Long id) {
        readingRepository.deleteById(id);
    }

    public Optional<Reading> findById(Long id) {
        return readingRepository.findById(id);
    }

    public Collection<Reading> findByCompany(Long id) {
        return readingRepository.findByCompany_Id(id);
    }

    public Collection<Reading> findByMeter(Long id) {
        return readingRepository.findByMeter_Id(id);
    }
}
