package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.CustomerMiniDTO;
import com.grash.dto.CustomerPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.CustomerMapper;
import com.grash.model.Customer;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.CustomerService;
import com.grash.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customers")
@Api(tags = "customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final UserService userService;
    private final CustomerMapper customerMapper;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<Customer>> search(@RequestBody SearchCriteria searchCriteria, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.VENDORS_AND_CUSTOMERS)) {
                searchCriteria.filterCompany(user);
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(customerService.findBySearchCriteria(searchCriteria));
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "AssetCategory not found")})
    public Collection<CustomerMiniDTO> getMini(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        return customerService.findByCompany(user.getCompany().getId()).stream().map(customerMapper::toMiniDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Customer not found")})
    public Customer getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Customer> optionalCustomer = customerService.findById(id);
        if (optionalCustomer.isPresent()) {
            Customer savedCustomer = optionalCustomer.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.VENDORS_AND_CUSTOMERS)) {
                return savedCustomer;
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public Customer create(@ApiParam("Customer") @Valid @RequestBody Customer customerReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.VENDORS_AND_CUSTOMERS)) {
            return customerService.create(customerReq);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Customer not found")})
    public Customer patch(@ApiParam("Customer") @Valid @RequestBody CustomerPatchDTO customer,
                          @ApiParam("id") @PathVariable("id") Long id,
                          HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Customer> optionalCustomer = customerService.findById(id);

        if (optionalCustomer.isPresent()) {
            Customer savedCustomer = optionalCustomer.get();
            if (user.getRole().getEditOtherPermissions().contains(PermissionEntity.VENDORS_AND_CUSTOMERS) || savedCustomer.getCreatedBy().equals(user.getId())) {
                return customerService.update(id, customer);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Customer not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Customer not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Customer> optionalCustomer = customerService.findById(id);
        if (optionalCustomer.isPresent()) {
            Customer savedCustomer = optionalCustomer.get();
            if (user.getId().equals(savedCustomer.getCreatedBy()) ||
                    user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.VENDORS_AND_CUSTOMERS)) {
                customerService.delete(id);
                return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Customer not found", HttpStatus.NOT_FOUND);
    }

}
