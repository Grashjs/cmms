package com.grash.controller;

import com.grash.dto.RelationPatchDTO;
import com.grash.dto.RelationPostDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Relation;
import com.grash.model.WorkOrder;
import com.grash.service.RelationService;
import com.grash.service.UserService;
import com.grash.service.WorkOrderService;
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
@RequestMapping("/relations")
@Api(tags = "relation")
@RequiredArgsConstructor
public class RelationController {

    private final RelationService relationService;
    private final UserService userService;
    private final WorkOrderService workOrderService;


    @GetMapping("")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Relation not found")})
    public Collection<Relation> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Long companyId = user.getCompany().getId();
        return relationService.findByCompany(companyId);
    }

    @GetMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Relation not found")})
    public Collection<Relation> getByWorkOrder(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            return relationService.findByWorkOrder(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Relation not found")})
    public Relation getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Relation> optionalRelation = relationService.findById(id);
        if (optionalRelation.isPresent()) {
            return relationService.findById(id).get();
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public Relation create(@ApiParam("Relation") @Valid @RequestBody RelationPostDTO relationReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Long parentId = relationReq.getParent().getId();
        Long childId = relationReq.getChild().getId();
        if (relationService.findByParentAndChild(parentId, childId).isEmpty() && relationService.findByParentAndChild(childId, parentId).isEmpty()) {
            return relationService.createPost(relationReq);
        } else
            throw new CustomException("There already is a relation between these 2 Work Orders", HttpStatus.NOT_ACCEPTABLE);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Relation not found")})
    public Relation patch(@ApiParam("Relation") @Valid @RequestBody RelationPatchDTO relation,
                          @ApiParam("id") @PathVariable("id") Long id,
                          HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Relation> optionalRelation = relationService.findById(id);

        if (optionalRelation.isPresent()) {
            Relation savedRelation = optionalRelation.get();
            return relationService.update(id, relation);
        } else {
            return null;
        }


    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Relation not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Relation> optionalRelation = relationService.findById(id);
        if (optionalRelation.isPresent()) {
            relationService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("Relation not found", HttpStatus.NOT_FOUND);
    }

}
