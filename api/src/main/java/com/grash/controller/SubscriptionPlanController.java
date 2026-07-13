package com.grash.controller;

import com.grash.dto.SubscriptionPlanPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.SubscriptionPlan;
import com.grash.service.SubscriptionPlanService;
import com.grash.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping("/subscription-plans")
@Api(tags = "subscriptionPlan")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final UserService userService;

    @GetMapping("")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "SubscriptionPlanCategory not found")})
    public Collection<SubscriptionPlan> getAll(HttpServletRequest req) {
        return subscriptionPlanService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "SubscriptionPlan not found")})
    public SubscriptionPlan getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<SubscriptionPlan> optionalSubscriptionPlan = subscriptionPlanService.findById(id);
        if (optionalSubscriptionPlan.isPresent()) {
            SubscriptionPlan savedSubscriptionPlan = optionalSubscriptionPlan.get();
            return savedSubscriptionPlan;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public SubscriptionPlan create(@ApiParam("SubscriptionPlan") @Valid @RequestBody SubscriptionPlan subscriptionPlanReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        return subscriptionPlanService.create(subscriptionPlanReq);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "SubscriptionPlan not found")})
    public SubscriptionPlan patch(@ApiParam("SubscriptionPlan") @Valid @RequestBody SubscriptionPlanPatchDTO subscriptionPlan, @ApiParam("id") @PathVariable("id") Long id,
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "SubscriptionPlan not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
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
