package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.LocationPatchDTO;
import com.grash.dto.LocationShowDTO;
import com.grash.dto.imports.LocationImportDTO;
import com.grash.dto.license.LicenseEntitlement;
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

import static com.grash.utils.Consts.usageBasedLicenseLimits;

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
    private final LicenseService licenseService;

    @Transactional
    public Location create(Location location, Company company) {
        checkUsageBasedLimit(company);
        location.setCustomId(getLocationNumber(company));

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

    private void checkUsageBasedLimit(Company company) {
        Integer threshold = usageBasedLicenseLimits.get(LicenseEntitlement.UNLIMITED_LOCATIONS);
        if (!licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_LOCATIONS)
                && locationRepository.hasMoreThan(company.getId(), threshold.longValue() - 1
        ))
            throw new CustomException("You need a license to add a new location. Free Limit reached: " + threshold,
                    HttpStatus.FORBIDDEN);

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

    private String getLocationNumber(Company company) {
        Long nextSequence = customSequenceService.getNextLocationSequence(company);
        return "L" + String.format("%06d", nextSequence);
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

    public List<Location> findByNameIgnoreCaseAndCompany(String locationName, Long companyId) {
        return locationRepository.findByNameIgnoreCaseAndCompany_Id(locationName, companyId);
    }

    public void importLocation(Location location, LocationImportDTO dto, Company company) {
        checkUsageBasedLimit(company);
        Long companyId = company.getId();
        location.setName(dto.getName());
        location.setAddress(dto.getAddress());
        location.setLongitude(dto.getLongitude());
        location.setLatitude(dto.getLatitude());
        Optional<Location> optionalLocation =
                findByNameIgnoreCaseAndCompany(dto.getParentLocationName(), companyId).stream().findFirst();
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
        location.setCustomId(getLocationNumber(company));
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
        List<LocationImportDTO> identifiedTopLevelLocations = new ArrayList<>();

        Set<String> allLocationNames = new HashSet<>();
        for (LocationImportDTO location : locations) {
            if (location.getName() != null) { // Guard against locations with null names if possible
                allLocationNames.add(location.getName());
            }
        }

        // Group locations by parent name and identify top-level locations
        // Using a HashSet here to ensure we only consider each unique location object once
        // for building the map and topLevelLocations, in case the input list has duplicate object references.
        Set<LocationImportDTO> distinctInputLocations = new HashSet<>(locations);

        for (LocationImportDTO location : distinctInputLocations) { // Iterate over unique location objects
            String parentName = location.getParentLocationName();
            locationMap.computeIfAbsent(parentName, k -> new ArrayList<>()).add(location);

            // An location is top-level if it has no parent,
            // or its declared parent doesn't exist in the provided list of locations.
            if (parentName == null || !allLocationNames.contains(parentName)) {
                identifiedTopLevelLocations.add(location);
            }
        }

        List<LocationImportDTO> orderedLocations = new ArrayList<>();
        Set<LocationImportDTO> visited = new HashSet<>(); // Keep track of visited locations

        // Process identified top-level locations.
        // The `visited` set will ensure each location is added only once,
        // even if it appears multiple times in `identifiedTopLevelLocations`
        // (e.g., multiple distinct orphan objects point to the same non-existent parent)
        // or if children of different top-level locations overlap due to same names.
        orderLocationsRecursive(locationMap, identifiedTopLevelLocations, orderedLocations, visited);

        return orderedLocations;
    }

    private static void orderLocationsRecursive(Map<String, List<LocationImportDTO>> locationMap,
                                                List<LocationImportDTO> currentLevelLocations,
                                                List<LocationImportDTO> orderedLocations,
                                                Set<LocationImportDTO> visited) {
        if (currentLevelLocations == null) {
            return;
        }
        for (LocationImportDTO location : currentLevelLocations) {
            // Only process and add the location if it hasn't been visited yet
            if (visited.add(location)) { // .add() returns true if the element was new to the set
                orderedLocations.add(location);
                List<LocationImportDTO> children = locationMap.get(location.getName());
                if (children != null) {
                    orderLocationsRecursive(locationMap, children, orderedLocations, visited);
                }
            }
        }
    }

    public boolean hasChildren(Long locationId) {
        return locationRepository.countByParentLocation_Id(locationId) > 0;
    }
}
