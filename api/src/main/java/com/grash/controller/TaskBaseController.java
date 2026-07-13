package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.TaskBasePatchDTO;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.TaskBase;
import com.grash.service.TaskBaseService;
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
import java.util.Optional;

@RestController
@RequestMapping("/task-bases")
@Api(tags = "taskBase")
@RequiredArgsConstructor
public class TaskBaseController {

    private final TaskBaseService taskBaseService;
    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "TaskBase not found")})
    public TaskBase getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<TaskBase> optionalTaskBase = taskBaseService.findById(id);
        if (optionalTaskBase.isPresent()) {
            TaskBase savedTaskBase = optionalTaskBase.get();
            return savedTaskBase;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public TaskBase create(@ApiParam("TaskBase") @Valid @RequestBody TaskBase taskBaseReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        return taskBaseService.create(taskBaseReq);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "TaskBase not found")})
    public TaskBase patch(@ApiParam("TaskBase") @Valid @RequestBody TaskBasePatchDTO taskBase, @ApiParam("id") @PathVariable("id") Long id,
                          HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<TaskBase> optionalTaskBase = taskBaseService.findById(id);

        if (optionalTaskBase.isPresent()) {
            TaskBase savedTaskBase = optionalTaskBase.get();
            return taskBaseService.update(id, taskBase);
        } else throw new CustomException("TaskBase not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "TaskBase not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<TaskBase> optionalTaskBase = taskBaseService.findById(id);
        if (optionalTaskBase.isPresent()) {
            TaskBase savedTaskBase = optionalTaskBase.get();
            taskBaseService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("TaskBase not found", HttpStatus.NOT_FOUND);
    }

}
