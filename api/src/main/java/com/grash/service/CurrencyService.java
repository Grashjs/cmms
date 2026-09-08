package com.grash.service;

import com.grash.dto.CurrencyPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CurrencyMapper;
import com.grash.model.Currency;
import com.grash.model.User;
import com.grash.model.enums.RoleType;
import com.grash.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrencyService {
    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;

    @Transactional
    public Currency create(Currency currency) {
        return currencyRepository.save(currency);
    }

    public Collection<Currency> getAll() {
        return currencyRepository.findAll();
    }

    public Currency getById(Long id) {
        Optional<Currency> optionalCurrency = currencyRepository.findById(id);
        if (optionalCurrency.isPresent()) {
            return optionalCurrency.get();
        } else throw new CustomException("Currency not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public Currency patch(Long id, CurrencyPatchDTO currencyPatchDTO) {
        Optional<Currency> optionalCurrency = currencyRepository.findById(id);
        if (optionalCurrency.isPresent()) {
            Currency currency = optionalCurrency.get();
            return currencyRepository.save(currencyMapper.updateCurrency(currency, currencyPatchDTO));
        } else throw new CustomException("Currency not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public void deleteByIdAndUser(Long id, User user) {
        Optional<Currency> optionalCurrency = currencyRepository.findById(id);
        if (optionalCurrency.isPresent()) {
            if (user.getRole().getRoleType().equals(RoleType.ROLE_SUPER_ADMIN)) {
                currencyRepository.deleteById(id);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Currency not found", HttpStatus.NOT_FOUND);
    }

    public Optional<Currency> findById(Long id) {
        return currencyRepository.findById(id);
    }

    public Optional<Currency> findByCode(String code) {
        return currencyRepository.findByCode(code);
    }
}