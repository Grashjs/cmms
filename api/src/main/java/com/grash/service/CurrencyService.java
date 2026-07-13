package com.grash.service;

import com.grash.dto.CurrencyPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CurrencyMapper;
import com.grash.model.Currency;
import com.grash.model.OwnUser;
import com.grash.model.enums.RoleType;
import com.grash.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrencyService {
    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;

    public Currency create(Currency Currency) {
        return currencyRepository.save(Currency);
    }

    public Currency update(Long id, CurrencyPatchDTO currencyPatchDTO) {
        if (currencyRepository.existsById(id)) {
            Currency currency = currencyRepository.findById(id).get();
            return currencyRepository.save(currencyMapper.updateCurrency(currency, currencyPatchDTO));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Currency> getAll() {
        return currencyRepository.findAll();
    }

    public void delete(Long id) {
        currencyRepository.deleteById(id);
    }

    public Optional<Currency> findById(Long id) {
        return currencyRepository.findById(id);
    }

    public Optional<Currency> findByCode(String code) {
        return currencyRepository.findByCode(code);
    }
}
