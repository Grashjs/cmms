package com.grash.controller;

import com.grash.dto.SubscriptionPlanPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.SubscriptionPlan;
import com.grash.service.SubscriptionPlanService;
import com.grash.service.UserService;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping("/subscription-plans")
@Tag(name = "subscriptionPlan")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final UserService userService;

    @GetMapping("")

    public Collection<SubscriptionPlan> getAll(HttpServletRequest req) {
        return subscriptionPlanService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")

    public SubscriptionPlan getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<SubscriptionPlan> optionalSubscriptionPlan = subscriptionPlanService.findById(id);
        if (optionalSubscriptionPlan.isPresent()) {
            SubscriptionPlan savedSubscriptionPlan = optionalSubscriptionPlan.get();
            return savedSubscriptionPlan;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    SubscriptionPlan create(@Valid @RequestBody SubscriptionPlan subscriptionPlanReq,
                            HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        return subscriptionPlanService.create(subscriptionPlanReq);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")

    public SubscriptionPlan patch(@Valid @RequestBody SubscriptionPlanPatchDTO subscriptionPlan,
                                  @PathVariable("id") Long id,
                                  HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<SubscriptionPlan> optionalSubscriptionPlan = subscriptionPlanService.findById(id);

        if (optionalSubscriptionPlan.isPresent()) {
            SubscriptionPlan savedSubscriptionPlan = optionalSubscriptionPlan.get();
            return subscriptionPlanService.update(id, subscriptionPlan);
        } else throw new CustomException("SubscriptionPlan not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")

    public ResponseEntity delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<SubscriptionPlan> optionalSubscriptionPlan = subscriptionPlanService.findById(id);
        if (optionalSubscriptionPlan.isPresent()) {
            SubscriptionPlan savedSubscriptionPlan = optionalSubscriptionPlan.get();
            subscriptionPlanService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("SubscriptionPlan not found", HttpStatus.NOT_FOUND);
    }

}


