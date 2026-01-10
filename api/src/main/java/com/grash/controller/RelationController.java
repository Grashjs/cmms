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
@RequestMapping("/relations")
@Tag(name = "relation")
@RequiredArgsConstructor
public class RelationController {

    private final RelationService relationService;
    private final UserService userService;
    private final WorkOrderService workOrderService;


    @GetMapping("")
    @PreAuthorize("permitAll()")

    public Collection<Relation> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Long companyId = user.getCompany().getId();
        return relationService.findByCompany(companyId);
    }

    @GetMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")

    public Collection<Relation> getByWorkOrder(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            return relationService.findByWorkOrder(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")

    public Relation getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Relation> optionalRelation = relationService.findById(id);
        if (optionalRelation.isPresent()) {
            return relationService.findById(id).get();
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    Relation create(@Valid @RequestBody RelationPostDTO relationReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Long parentId = relationReq.getParent().getId();
        Long childId = relationReq.getChild().getId();
        if (relationService.findByParentAndChild(parentId, childId).isEmpty() && relationService.findByParentAndChild(childId, parentId).isEmpty()) {
            return relationService.createPost(relationReq);
        } else
            throw new CustomException("There already is a relation between these 2 Work Orders",
                    HttpStatus.NOT_ACCEPTABLE);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public Relation patch(@Valid @RequestBody RelationPatchDTO relation,
                          @PathVariable("id") Long id,
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

    public ResponseEntity delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Relation> optionalRelation = relationService.findById(id);
        if (optionalRelation.isPresent()) {
            relationService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("Relation not found", HttpStatus.NOT_FOUND);
    }

}


