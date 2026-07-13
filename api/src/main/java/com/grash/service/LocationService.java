package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.LocationPatchDTO;
import com.grash.dto.LocationShowDTO;
import com.grash.dto.imports.LocationImportDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.LocationMapper;
import com.grash.model.*;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.RoleType;
import com.grash.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {
    private final LocationRepository locationRepository;
    private final UserService userService;
    private final CompanyService companyService;
    private final CustomerService customerService;
    private final MessageSource messageSource;
    private final VendorService vendorService;
    private final LocationMapper locationMapper;
    private final NotificationService notificationService;
    private final TeamService teamService;
    private final EntityManager em;
    private final FileService fileService;
    private final CustomSequenceService customSequenceService;

    @Transactional
    public Location create(Location location, Company company) {
        Long nextSequence = customSequenceService.getNextLocationSequence(company);
        location.setCustomId("L" + String.format("%06d", nextSequence));

        Location savedLocation = locationRepository.saveAndFlush(location);
        em.refresh(savedLocation);
        return savedLocation;
    }

    @Transactional
    public Location update(Long id, LocationPatchDTO location) {
        if (locationRepository.existsById(id)) {
            Location savedLocation = locationRepository.findById(id).get();
            Location patchedLocation = locationRepository.saveAndFlush(locationMapper.updateLocation(savedLocation,
                    location));
            em.refresh(patchedLocation);
            return patchedLocation;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Location> getAll() {
        return locationRepository.findAll();
    }

    public void delete(Long id) {
        locationRepository.deleteById(id);
    }

    public Optional<Location> findById(Long id) {
        return locationRepository.findById(id);
    }

    public Collection<Location> findByCompany(Long id) {
        return locationRepository.findByCompany_Id(id);
    }

    public List<Location> findByCompany(Long id, Sort sort) {
        return locationRepository.findByCompany_Id(id, sort);
    }


    public void notify(Location location, Locale locale) {
        String title = messageSource.getMessage("new_assignment", null, locale);
        String message = messageSource.getMessage("notification_location_assigned", new Object[]{location.getName()},
                locale);
        notificationService.createMultiple(location.getUsers().stream().map(user -> new Notification(message, user,
                NotificationType.LOCATION, location.getId())).collect(Collectors.toList()), true, title);
    }

    public void patchNotify(Location oldLocation, Location newLocation, Locale locale) {
        String title = messageSource.getMessage("new_assignment", null, locale);
        String message = messageSource.getMessage("notification_location_assigned",
                new Object[]{newLocation.getName()}, locale);
        notificationService.createMultiple(oldLocation.getNewUsersToNotify(newLocation.getUsers()).stream().map(user ->
                new Notification(message, user, NotificationType.LOCATION, newLocation.getId())).collect(Collectors.toList()), true, title);
    }

    public List<Location> findLocationChildren(Long id, Sort sort) {
        return locationRepository.findByParentLocation_Id(id, sort);
    }

    public void save(Location location) {
        locationRepository.save(location);
    }

    public boolean isLocationInCompany(Location location, long companyId, boolean optional) {
        if (optional) {
            Optional<Location> optionalLocation = location == null ? Optional.empty() : findById(location.getId());
            return location == null || (optionalLocation.isPresent() && optionalLocation.get().getCompany().getId().equals(companyId));
        } else {
            Optional<Location> optionalLocation = findById(location.getId());
            return optionalLocation.isPresent() && optionalLocation.get().getCompany().getId().equals(companyId);
        }
    }

    public Optional<Location> findByNameIgnoreCaseAndCompany(String locationName, Long companyId) {
        return locationRepository.findByNameIgnoreCaseAndCompany_Id(locationName, companyId);
    }

    public void importLocation(Location location, LocationImportDTO dto, Company company) {
        Long companyId = company.getId();
        location.setName(dto.getName());
        location.setAddress(dto.getAddress());
        location.setLongitude(dto.getLongitude());
        location.setLatitude(dto.getLatitude());
        Optional<Location> optionalLocation = findByNameIgnoreCaseAndCompany(dto.getParentLocationName(), companyId);
        optionalLocation.ifPresent(location::setParentLocation);
        List<OwnUser> workers = new ArrayList<>();
        dto.getWorkersEmails().forEach(email -> {
            Optional<OwnUser> optionalUser1 = userService.findByEmailAndCompany(email, companyId);
            optionalUser1.ifPresent(workers::add);
        });
        location.setWorkers(workers);
        List<Team> teams = new ArrayList<>();
        dto.getTeamsNames().forEach(teamName -> {
            Optional<Team> optionalTeam = teamService.findByNameIgnoreCaseAndCompany(teamName, companyId);
            optionalTeam.ifPresent(teams::add);
        });
        location.setTeams(teams);
        List<Customer> customers = new ArrayList<>();
        dto.getCustomersNames().forEach(name -> {
            Optional<Customer> optionalCustomer = customerService.findByNameIgnoreCaseAndCompany(name, companyId);
            optionalCustomer.ifPresent(customers::add);
        });
        location.setCustomers(customers);
        List<Vendor> vendors = new ArrayList<>();
        dto.getVendorsNames().forEach(name -> {
            Optional<Vendor> optionalVendor = vendorService.findByNameIgnoreCaseAndCompany(name, companyId);
            optionalVendor.ifPresent(vendors::add);
        });
        location.setVendors(vendors);
        locationRepository.save(location);
    }

    public Optional<Location> findByIdAndCompany(Long id, Long companyId) {
        return locationRepository.findByIdAndCompany_Id(id, companyId);
    }

    public Page<LocationShowDTO> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<Location> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return locationRepository.findAll(builder.build(), page).map(location -> locationMapper.toShowDto(location,
                this));
    }

    public static List<LocationImportDTO> orderLocations(List<LocationImportDTO> locations) {
        Map<String, List<LocationImportDTO>> locationMap = new HashMap<>();
        List<LocationImportDTO> topLevelLocations = new ArrayList<>();

        // Group locations by parent name
        for (LocationImportDTO location : locations) {
            String parentName = location.getParentLocationName();
            locationMap.computeIfAbsent(parentName, k -> new ArrayList<>()).add(location);
            if (parentName == null || locations.stream().noneMatch(locationImportDTO -> locationImportDTO.getName().equals(parentName))) {
                topLevelLocations.add(location);
            }
        }

        // Order locations recursively
        List<LocationImportDTO> orderedLocations = new ArrayList<>();
        orderLocationsRecursive(locationMap, topLevelLocations, orderedLocations);

        return orderedLocations;
    }

    private static void orderLocationsRecursive(Map<String, List<LocationImportDTO>> locationMap,
                                                List<LocationImportDTO> locations,
                                                List<LocationImportDTO> orderedLocations) {
        for (LocationImportDTO location : locations) {
            orderedLocations.add(location);
            List<LocationImportDTO> children = locationMap.get(location.getName());
            if (children != null) {
                orderLocationsRecursive(locationMap, children, orderedLocations);
            }
        }
    }

    public boolean hasChildren(Long locationId) {
        return locationRepository.countByParentLocation_Id(locationId) > 0;
    }
}
