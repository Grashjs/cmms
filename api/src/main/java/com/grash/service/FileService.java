package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.model.File;
import com.grash.model.OwnUser;
import com.grash.model.enums.RoleType;
import com.grash.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private AssetService assetService;
    private PartService partService;
    private RequestService requestService;
    private WorkOrderService workOrderService;
    private LocationService locationService;

    @Autowired
    public void setDeps(@Lazy AssetService assetService, @Lazy PartService partService,
                        @Lazy RequestService requestService, @Lazy LocationService locationService,
                        @Lazy WorkOrderService workOrderService
    ) {
        this.assetService = assetService;
        this.partService = partService;
        this.requestService = requestService;
        this.locationService = locationService;
        this.workOrderService = workOrderService;
    }

    public File create(File File) {
        return fileRepository.save(File);
    }

    public File update(File File) {
        return fileRepository.save(File);
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
}
