package com.grash.controller;

import com.grash.dto.PartQuantityCompletePatchDTO;
import com.grash.dto.PartQuantityPatchDTO;
import com.grash.dto.PartQuantityShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.PartQuantityMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.service.*;
import com.grash.utils.Helper;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/part-quantities")
@Api(tags = "partQuantity")
@RequiredArgsConstructor
public class PartQuantityController {

    private final PartQuantityService partQuantityService;
    private final PartQuantityMapper partQuantityMapper;
    private final UserService userService;
    private final WorkOrderService workOrderService;
    private final PartService partService;
    private final PurchaseOrderService purchaseOrderService;

    @GetMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "PartQuantityCategory not found")})
    public Collection<PartQuantityShowDTO> getByWorkOrder(HttpServletRequest req,
                                                          @ApiParam("id") @PathVariable("id") Long id) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            return partQuantityService.findByWorkOrder(id).stream().map(partQuantityMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/purchase-order/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Purchase not found")})
    public Collection<PartQuantityShowDTO> getByPurchaseOrder(HttpServletRequest req, @ApiParam("id") @PathVariable(
            "id") Long id) {
        OwnUser user = userService.whoami(req);
        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderService.findById(id);
        if (optionalPurchaseOrder.isPresent()) {
            return partQuantityService.findByPurchaseOrder(id).stream().map(partQuantityMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/work-order/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "WorkOrder not found")})
    public Collection<PartQuantityShowDTO> patchWorkOrder(@ApiParam("PartQuantities") @Valid @RequestBody List<Long> parts, @ApiParam("id") @PathVariable("id") Long id,
                                                          HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);

        if (optionalWorkOrder.isPresent()) {
            WorkOrder savedWorkOrder = optionalWorkOrder.get();
            if (savedWorkOrder.getFirstTimeToReact() == null) {
                savedWorkOrder.setFirstTimeToReact(new Date());
                workOrderService.save(savedWorkOrder);
            }
            if (savedWorkOrder.canBeEditedBy(user)) {
                Collection<PartQuantity> partQuantities = partQuantityService.findByWorkOrder(id);
                Collection<Long> partQuantityMappedPartIds = partQuantities.stream().map
                        (partQuantity -> partQuantity.getPart().getId()).collect(Collectors.toList());
                parts.forEach(partId -> {
                    if (!partQuantityMappedPartIds.contains(partId)) {
                        Optional<Part> optionalPart = partService.findById(partId);
                        if (optionalPart.isPresent()) {
                            partService.consumePart(optionalPart.get().getId(), 1, savedWorkOrder,
                                    Helper.getLocale(user));
                            PartQuantity partQuantity = new PartQuantity(optionalPart.get(), savedWorkOrder, null, 1);
                            partQuantityService.create(partQuantity);
                        } else throw new CustomException("Part not found", HttpStatus.NOT_FOUND);
                    }
                });
                partQuantityMappedPartIds.forEach(partId -> {
                    if (!parts.contains(partId)) {
                        partQuantityService.delete(partQuantities.stream().filter(partQuantity ->
                                partQuantity.getPart().getId().equals(partId)).findFirst().get().getId());
                    }
                });
                return partQuantityService.findByWorkOrder(id).stream().map(partQuantityMapper::toShowDto).collect(Collectors.toList());
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("WorkOrder not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/purchase-order/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Purchase Order not found")})
    public Collection<PartQuantityShowDTO> patchPurchaseOrder(@ApiParam("PartQuantities") @Valid @RequestBody List<PartQuantityCompletePatchDTO> partQuantitiesReq, @ApiParam("id") @PathVariable("id") Long id,
                                                              HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderService.findById(id);

        if (optionalPurchaseOrder.isPresent()) {
            PurchaseOrder savedPurchaseOrder = optionalPurchaseOrder.get();
            Collection<PartQuantity> savedPartQuantities = partQuantityService.findByPurchaseOrder(id);

            Collection<Long> savedPartQuantitiesMappedPartIds = savedPartQuantities.stream().map
                    (partQuantity -> partQuantity.getPart().getId()).collect(Collectors.toList());

            Collection<Long> partQuantitiesReqMappedPartIds = partQuantitiesReq.stream().map
                    (partQuantity -> partQuantity.getPart().getId()).collect(Collectors.toList());
            partQuantitiesReq.forEach(partQuantityReq -> {
                if (savedPartQuantitiesMappedPartIds.contains(partQuantityReq.getPart().getId())) {
                    //existing parts
                    PartQuantity savedPartQuantity =
                            savedPartQuantities.stream().filter(partQuantity -> partQuantity.getPart().getId().equals(partQuantityReq.getPart().getId())).findFirst().get();
                    savedPartQuantity.setQuantity(partQuantityReq.getQuantity());
                    partQuantityService.save(savedPartQuantity);
                } else {
                    //new Parts
                    Optional<Part> optionalPart = partService.findById(partQuantityReq.getPart().getId());
                    if (optionalPart.isPresent()) {
                        PartQuantity partQuantity = new PartQuantity(optionalPart.get(), null, savedPurchaseOrder,
                                partQuantityReq.getQuantity());
                        partQuantityService.create(partQuantity);
                    } else throw new CustomException("Part not found", HttpStatus.NOT_FOUND);
                }
            });
            // removed Parts
            savedPartQuantitiesMappedPartIds.forEach(partId -> {
                if (!partQuantitiesReqMappedPartIds.contains(partId)) {
                    partQuantityService.delete(savedPartQuantities.stream().filter(partQuantity ->
                            partQuantity.getPart().getId().equals(partId)).findFirst().get().getId());
                }
            });
            return partQuantityService.findByPurchaseOrder(id).stream().map(partQuantityMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("WorkOrder not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "PartQuantity not found")})
    public PartQuantityShowDTO getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<PartQuantity> optionalPartQuantity = partQuantityService.findById(id);
        if (optionalPartQuantity.isPresent()) {
            PartQuantity savedPartQuantity = optionalPartQuantity.get();
            return partQuantityMapper.toShowDto(savedPartQuantity);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public PartQuantityShowDTO create(@ApiParam("PartQuantity") @Valid @RequestBody PartQuantity partQuantityReq,
                                      HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        PartQuantity savedPartQuantity = partQuantityService.create(partQuantityReq);
        return partQuantityMapper.toShowDto(savedPartQuantity);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "PartQuantity not found")})
    public PartQuantityShowDTO patch(@ApiParam("PartQuantity") @Valid @RequestBody PartQuantityPatchDTO partQuantity,
                                     @ApiParam("id") @PathVariable("id") Long id,
                                     HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<PartQuantity> optionalPartQuantity = partQuantityService.findById(id);
        if (optionalPartQuantity.isPresent()) {
            PartQuantity savedPartQuantity = optionalPartQuantity.get();
            if (savedPartQuantity.getPart().getUnit() == null) {
                partQuantity.setQuantity((int) partQuantity.getQuantity());
            }
            if (savedPartQuantity.getWorkOrder() != null) {
                partService.consumePart(savedPartQuantity.getPart().getId(),
                        partQuantity.getQuantity() - savedPartQuantity.getQuantity(),
                        savedPartQuantity.getWorkOrder(), Helper.getLocale(user));
            }
            PartQuantity patchedPartQuantity = partQuantityService.update(id, partQuantity);
            return partQuantityMapper.toShowDto(patchedPartQuantity);
        } else throw new CustomException("PartQuantity not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "PartQuantity not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<PartQuantity> optionalPartQuantity = partQuantityService.findById(id);
        if (optionalPartQuantity.isPresent()) {
            PartQuantity savedPartQuantity = optionalPartQuantity.get();
            if
            (user.getId().equals(savedPartQuantity.getCreatedBy())
                    || user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)) {
                partQuantityService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("PartQuantity not found", HttpStatus.NOT_FOUND);
    }

}
