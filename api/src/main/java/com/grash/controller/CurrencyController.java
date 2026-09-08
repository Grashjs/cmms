package com.grash.controller;

import com.grash.dto.CurrencyPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.model.Currency;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.CurrencyService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Collection;

@RestController
@RequestMapping("/currencies")
@Tag(name = "Currencies", description = "Operations on currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    public Collection<Currency> getAll() {
        return currencyService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public Currency getById(@PathVariable("id") Long id,
                            @Parameter(hidden = true) @CurrentUser User user) {
        return currencyService.getById(id);
    }


    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    Currency create(@Parameter(description = "Currency to create") @Valid @RequestBody Currency currency,
                    @Parameter(hidden = true) @CurrentUser User user) {
        return currencyService.create(currency);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public Currency patch(@Parameter(description = "Currency fields to update") @Valid @RequestBody CurrencyPatchDTO currencyPatchDTO,
                          @PathVariable("id") Long id,
                          @Parameter(hidden = true) @CurrentUser User user) {
        return currencyService.patch(id, currencyPatchDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        currencyService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }

}