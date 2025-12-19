package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PartMiniDTO;
import com.grash.dto.PartPatchDTO;
import com.grash.dto.PartShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.PartMapper;
import com.grash.model.Asset;
import com.grash.model.OwnUser;
import com.grash.model.Part;
import com.grash.model.Workflow;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.model.enums.workflow.WFMainCondition;
import com.grash.service.PartService;
import com.grash.service.UserService;
import com.grash.service.WorkflowService;
import com.grash.utils.Helper;
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

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/parts")
@Api(tags = "part")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;
    private final PartMapper partMapper;
    private final UserService userService;
    private final WorkflowService workflowService;
    private final EntityManager em;


    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<PartShowDTO>> search(@RequestBody SearchCriteria searchCriteria,
                                                    HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)) {
                searchCriteria.filterCompany(user);
                boolean canViewOthers =
                        user.getRole().getViewOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS);
                if (!canViewOthers) {
                    searchCriteria.filterCreatedBy(user);
                }
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(partService.findBySearchCriteria(searchCriteria));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Part not found")})
    public PartShowDTO getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Part> optionalPart = partService.findById(id);
        if (optionalPart.isPresent()) {
            Part savedPart = optionalPart.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS) &&
                    (user.getRole().getViewOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS) || savedPart.getCreatedBy().equals(user.getId()))) {
                return partMapper.toShowDto(savedPart);
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public PartShowDTO create(@ApiParam("Part") @Valid @RequestBody Part partReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)) {
            if (partReq.getBarcode() != null) {
                Optional<Part> optionalPartWithSameBarCode = partService.findByBarcodeAndCompany(partReq.getBarcode()
                        , user.getCompany().getId());
                if (optionalPartWithSameBarCode.isPresent()) {
                    throw new CustomException("Part with same barcode exists", HttpStatus.NOT_ACCEPTABLE);
                }
            }
            Part savedPart = partService.create(partReq);
            partService.notify(savedPart, Helper.getLocale(user));
            return partMapper.toShowDto(savedPart);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Part not found")})
    public PartShowDTO patch(@ApiParam("Part") @Valid @RequestBody PartPatchDTO part, @ApiParam("id") @PathVariable(
                                     "id") Long id,
                             HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Part> optionalPart = partService.findById(id);

        if (optionalPart.isPresent()) {
            Part savedPart = optionalPart.get();
            em.detach(savedPart);
            if (user.getRole().getEditOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS) || savedPart.getCreatedBy().equals(user.getId())) {
                if (part.getBarcode() != null) {
                    Optional<Part> optionalPartWithSameBarCode =
                            partService.findByBarcodeAndCompany(part.getBarcode(), user.getCompany().getId());
                    if (optionalPartWithSameBarCode.isPresent() && !optionalPartWithSameBarCode.get().getId().equals(id)) {
                        throw new CustomException("Part with same barcode exists", HttpStatus.NOT_ACCEPTABLE);
                    }
                }
                Part patchedPart = partService.update(id, part);
                Collection<Workflow> workflows =
                        workflowService.findByMainConditionAndCompany(WFMainCondition.PART_UPDATED,
                        user.getCompany().getId());
                workflows.forEach(workflow -> workflowService.runPart(workflow, patchedPart));
                partService.patchNotify(savedPart, patchedPart, Helper.getLocale(user));
                return partMapper.toShowDto(patchedPart);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Part not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "AssetCategory not found")})
    public Collection<PartMiniDTO> getMini(HttpServletRequest req) {
        OwnUser part = userService.whoami(req);
        return partService.findByCompany(part.getCompany().getId()).stream().map(partMapper::toMiniDto).collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Part not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Part> optionalPart = partService.findById(id);
        if (optionalPart.isPresent()) {
            Part savedPart = optionalPart.get();
            if (savedPart.getId().equals(user.getId()) || user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)) {
                partService.delete(id);
                return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Part not found", HttpStatus.NOT_FOUND);
    }

}
