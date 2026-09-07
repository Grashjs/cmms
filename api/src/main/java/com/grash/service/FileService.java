package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.FilePatchDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.factory.StorageServiceFactory;
import com.grash.model.File;
import com.grash.model.RequestPortal;
import com.grash.model.Task;
import com.grash.model.User;
import com.grash.model.enums.*;
import com.grash.repository.FileRepository;
import com.grash.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final StorageServiceFactory storageServiceFactory;
    private final TaskRepository taskRepository;
    private final LicenseService licenseService;
    private final RequestPortalService requestPortalService;
    private final RateLimiterService rateLimiterService;

    public File create(File file) {
        return fileRepository.save(file);
    }

    public File update(File file) {
        return fileRepository.save(file);
    }

    public Collection<File> getAll() {
        return fileRepository.findAll();
    }

    public void delete(Long id) {
        fileRepository.deleteById(id);
    }

    public Optional<File> findById(Long id) {
        return fileRepository.findById(id);
    }

    public Collection<File> findByCompany(Long id) {
        return fileRepository.findByCompany_Id(id);
    }

    public Page<File> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<File> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return fileRepository.findAll(builder.build(), page);
    }

    @Transactional
    public Collection<File> uploadFiles(MultipartFile[] filesReq, String hidden, FileType fileType,
                                        Boolean bypass, Integer taskId, User user) {
        if (!licenseService.hasEntitlement(LicenseEntitlement.FILE_ATTACHMENTS))
            throw new CustomException("You need a license to add a file", HttpStatus.FORBIDDEN);
        boolean isBypass = Boolean.TRUE.equals(bypass);
        if (!rateLimiterService.tryConsumeFileUpload(String.valueOf(user.getId()), isBypass)) {
            throw new CustomException("Rate limit exceeded. Try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }
        if (isBypass || (user.getRole().getCreatePermissions().contains(PermissionEntity.FILES) &&
                user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.FILE))) {
            Collection<File> result = new ArrayList<>();
            Arrays.asList(filesReq).forEach(fileReq -> {
                String filePath = storageServiceFactory.getStorageService().upload(fileReq,
                        "company " + user.getCompany().getId());
                Task task = null;
                if (taskId != null) {
                    Optional<Task> optionalTask = taskRepository.findById(taskId.longValue());
                    if (optionalTask.isPresent()) {
                        task = optionalTask.get();
                    }
                }
                result.add(create(new File(fileReq.getOriginalFilename(), filePath, fileType, task,
                        hidden.equals("true"))));
            });
            return result;
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @Transactional
    public Collection<File> uploadToRequestPortal(String uuid, MultipartFile[] filesReq,
                                                  FileType fileType, String clientIp) {
        if (!rateLimiterService.resolveFileUploadBucket(clientIp).tryConsume(1)) {
            throw new CustomException("Rate limit exceeded. Try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }

        RequestPortal requestPortal = requestPortalService.findByUuidByUser(uuid).orElseThrow(() -> new CustomException(
                "Request Portal not found", HttpStatus.NOT_FOUND));

        String folder = "company " + requestPortal.getCompany().getId() + "/request-portal/" + requestPortal.getUuid();

        Collection<File> result = new ArrayList<>();
        Arrays.asList(filesReq).forEach(fileReq -> {
            String filePath = storageServiceFactory.getStorageService().upload(fileReq, folder);
            File file = new File(fileReq.getOriginalFilename(), filePath, fileType, null, true);
            file.setCompany(requestPortal.getCompany());
            result.add(create(file));
        });
        return result;
    }

    public SearchCriteria getSearchCriteria(User user, SearchCriteria searchCriteria) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.FILES)) {
                searchCriteria.filterCompany(user);
                boolean canViewOthers = user.getRole().getViewOtherPermissions().contains(PermissionEntity.FILES);
                if (!canViewOthers) {
                    searchCriteria.filterCreatedBy(user);
                }
                searchCriteria.getFilterFields().add(FilterField.builder()
                        .field("hidden")
                        .value(false)
                        .operation("eq")
                        .values(new ArrayList<>())
                        .alternatives(new ArrayList<>()).build());
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return searchCriteria;
    }

    public File getById(Long id, User user) {
        Optional<File> optionalFile = fileRepository.findById(id);
        if (optionalFile.isPresent()) {
            File savedFile = optionalFile.get();
            if (savedFile.canBeViewedBy(user)) {
                return savedFile;
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public File patchFile(Long id, FilePatchDTO file, User user) {
        Optional<File> optionalFile = fileRepository.findById(id);
        if (optionalFile.isPresent()) {
            File savedFile = optionalFile.get();
            if (savedFile.canBeEditedBy(user)) {
                savedFile.setName(file.getName());
                return update(savedFile);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("File not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public void deleteByIdAndUser(Long id, User user) {
        Optional<File> optionalFile = fileRepository.findById(id);
        if (optionalFile.isPresent()) {
            File savedFile = optionalFile.get();
            if (savedFile.canBeDeletedBy(user)) {
                delete(id);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("File not found", HttpStatus.NOT_FOUND);
    }
}
