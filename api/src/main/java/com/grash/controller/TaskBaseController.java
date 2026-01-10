package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.TaskBasePatchDTO;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.TaskBase;
import com.grash.service.TaskBaseService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Optional;

@RestController
@RequestMapping("/task-bases")
@Tag(name = "taskBase")
@RequiredArgsConstructor
public class TaskBaseController {

    private final TaskBaseService taskBaseService;
    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public TaskBase getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<TaskBase> optionalTaskBase = taskBaseService.findById(id);
        if (optionalTaskBase.isPresent()) {
            TaskBase savedTaskBase = optionalTaskBase.get();
            return savedTaskBase;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public TaskBase patch(@Valid @RequestBody TaskBasePatchDTO taskBase,
                          @PathVariable("id") Long id,
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
    public ResponseEntity delete(@PathVariable("id") Long id, HttpServletRequest req) {
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


