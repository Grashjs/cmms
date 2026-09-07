package com.grash.controller;


import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.FilePatchDTO;
import com.grash.dto.FileShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.mapper.FileMapper;
import com.grash.model.File;
import com.grash.model.User;
import com.grash.model.enums.FileType;
import com.grash.security.ClientIpResolver;
import com.grash.security.CurrentUser;
import com.grash.service.FileService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@Tag(name = "Files", description = "Operations on files")
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    private final FileMapper fileMapper;
    private final ClientIpResolver clientIpResolver;

    @PostMapping(value = "/upload", produces = "application/json")
    public List<FileShowDTO> handleFileUpload(@Parameter(description = "Files to upload") @RequestParam("files") MultipartFile[] filesReq,
                                              @Parameter(description = "Whether files should be hidden (true/false)") @RequestParam("hidden") String hidden,
                                              @Parameter(description = "Type of file") @RequestParam("type") FileType fileType,
                                              @Parameter(hidden = true) @RequestParam(value = "bypass", required =
                                                      false) Boolean bypass,
                                              @Parameter(description = "Optional task ID to associate files with") @RequestParam(value = "taskId", required = false) Integer taskId,
                                              @Parameter(hidden = true) @CurrentUser User user) {
        Collection<File> result = fileService.uploadFiles(filesReq, hidden, fileType, bypass, taskId, user);
        return result.stream().map(fileMapper::toShowDto).collect(Collectors.toList());
    }

    @PostMapping(value = "/upload/request-portal/{uuid}", produces = "application/json")
    public ResponseEntity<List<FileShowDTO>> uploadToRequestPortal(@Parameter(description = "Request portal UUID") @PathVariable("uuid") String uuid,
                                                                  @Parameter(description = "Files to upload") @RequestParam("files") MultipartFile[] filesReq,
                                                                  @Parameter(description = "Type of file") @RequestParam("type") FileType fileType,
                                                                  HttpServletRequest req) {
        String clientIp = clientIpResolver.resolve(req);
        Collection<File> result = fileService.uploadToRequestPortal(uuid, filesReq, fileType, clientIp);
        List<FileShowDTO> response = result.stream().map(fileMapper::toShowDto).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<FileShowDTO>> search(@Parameter(description = "Search criteria for filtering files") @RequestBody SearchCriteria searchCriteria,
                                                    @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(fileService.findBySearchCriteria(fileService.getSearchCriteria(user, searchCriteria))
                .map(fileMapper::toShowDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public FileShowDTO getById(@PathVariable("id") Long id,
                               @Parameter(hidden = true) @CurrentUser User user) {
        return fileMapper.toShowDto(fileService.getById(id, user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public FileShowDTO patch(@Parameter(description = "File fields to update") @Valid @RequestBody FilePatchDTO file,
                             @PathVariable("id") Long id,
                             @Parameter(hidden = true) @CurrentUser User user) {
        return fileMapper.toShowDto(fileService.patchFile(id, file, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        fileService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }
}
