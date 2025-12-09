package com.grash.controller;

import com.grash.dto.SignupSuccessResponse;
import com.grash.dto.SuccessResponse;
import com.grash.dto.UserSignupRequest;
import com.grash.dto.imports.*;
import com.grash.model.Asset;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.enums.Language;
import com.grash.security.CurrentUser;
import com.grash.security.CustomUserDetail;
import com.grash.service.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

    private final UserService userService;
    private final RateLimiterService rateLimiterService;
    private final ImportService importService;
    private final AssetService assetService;
    private final DemoDataService demoDataService;

    @GetMapping("/generate-account")
    public SuccessResponse generateAccount(HttpServletRequest req) {
        String clientIp = req.getRemoteAddr(); // use IP as the key
        if (!rateLimiterService.resolveBucket(clientIp).tryConsume(1)) {
            return new SuccessResponse(false, "Rate limit exceeded. Try again later.");
        }
        UserSignupRequest userSignupRequest = new UserSignupRequest();
        userSignupRequest.setFirstName("Demo");
        userSignupRequest.setLastName("Account");
        userSignupRequest.setEmail(UUID.randomUUID().toString().replace("-", "") + "@demo.com");
        userSignupRequest.setPassword("demo1234");
        userSignupRequest.setPhone(UUID.randomUUID().toString().replace("-", ""));
        userSignupRequest.setCompanyName("Demo");
        userSignupRequest.setLanguage(Language.EN);
        userSignupRequest.setDemo(true);
        SignupSuccessResponse<OwnUser> response = userService.signup(userSignupRequest);

        if (response.isSuccess()) {
            OwnUser user = response.getUser();
            CustomUserDetail customUserDetail =
                    CustomUserDetail.builder().user(user).build();
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    customUserDetail,
                    null,
                    customUserDetail.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            importDemoData(user.getCompany());
        }

        return new SuccessResponse(response.isSuccess(), response.getMessage());
    }

    @DeleteMapping("/demo-data")
    @PreAuthorize("permitAll()")
    public SuccessResponse deleteDemoData(@ApiIgnore @CurrentUser OwnUser user) {
        demoDataService.deleteDemoData(user.getCompany().getId());
        return new SuccessResponse(true, "Demo data deleted successfully");
    }

    private void importDemoData(Company company) {
        try {
            importLocations(company);
            importAssets(company);
            List<Asset> assets = assetService.findByCompany(company.getId());
            importMeters(company, assets);
            importParts(company);
            importWorkOrders(company);
        } catch (IOException e) {
            // In a real application, you'd want to handle this exception more gracefully
            e.printStackTrace();
        }
    }

    private void importLocations(Company company) throws IOException {
        List<LocationImportDTO> locations = parseCsv("demo-data/telecom/Locations.csv", this::toLocationImportDTO);
        importService.importLocations(locations, company);
    }

    private void importWorkOrders(Company company) throws IOException {
        List<WorkOrderImportDTO> workOrders = parseCsv("demo-data/telecom/WorkOrders.csv", this::toWorkOrderImportDTO);
        importService.importWorkOrders(workOrders, company);
    }

    private void importAssets(Company company) throws IOException {
        List<AssetImportDTO> assets = parseCsv("demo-data/telecom/Assets.csv", this::toAssetImportDTO);
        importService.importAssets(assets, company);
    }

    private void importMeters(Company company, List<Asset> assets) throws IOException {
        List<MeterImportDTO> meters = parseCsv("demo-data/telecom/Meters.csv", this::toMeterImportDTO);
        meters.forEach(meterImportDTO -> meterImportDTO.setAssetName(
                assets.get(new Random().nextInt(assets.size())).getName()
        ));
        importService.importMeters(meters, company);
    }

    private void importParts(Company company) throws IOException {
        List<PartImportDTO> parts = parseCsv("demo-data/telecom/Parts.csv", this::toPartImportDTO);
        importService.importParts(parts, company);
    }

    private <T> List<T> parseCsv(String filePath, java.util.function.Function<CSVRecord, T> mapper) throws IOException {
        ClassPathResource resource = new ClassPathResource(filePath);
        try (Reader reader = new InputStreamReader(resource.getInputStream());
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim())) {
            return StreamSupport.stream(csvParser.spliterator(), false)
                    .map(mapper)
                    .collect(Collectors.toList());
        }
    }

    private LocationImportDTO toLocationImportDTO(CSVRecord record) {
        return LocationImportDTO.builder()
                .name(record.get("Name"))
                .address(record.get("Address"))
                .parentLocationName(record.get("Parent Location"))
                .build();
    }

    public WorkOrderImportDTO toWorkOrderImportDTO(CSVRecord record) {
        return WorkOrderImportDTO.builder()
                .id(parseLong(record.get("ID")))
                .title(record.get("Title"))
                .status(record.get("Status"))
                .priority(record.get("Priority"))
                .description(record.get("Description"))
                .dueDate(parseDouble(record.get("Due Date")))
                .estimatedDuration(parseDoubleOrDefault(record.get("Estimated Duration"), 0.0))
                .requiredSignature(record.get("Requires Signature"))
                .category(record.get("Category"))
                .locationName(record.get("Location Name"))
                .teamName(record.get("Team Name"))
                .primaryUserEmail(record.get("Primary User"))
                .assignedToEmails(splitList(record.get("Assigned To")))
                .assetName(record.get("Asset Name"))
                .completedByEmail(record.get("Completed By"))
                .completedOn(parseDouble(record.get("Completed On")))
                .archived(record.get("Archived"))
                .feedback(record.get("Feedback"))
                .customersNames(splitList(record.get("Contractors")))
                .build();
    }

    private static List<String> splitList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(value.split("\\s*,\\s*"));
    }

    private static Double parseDouble(String value) {
        try {
            return (value == null || value.trim().isEmpty()) ? null : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDoubleOrDefault(String value, double defaultValue) {
        Double result = parseDouble(value);
        return result == null ? defaultValue : result;
    }

    private static Long parseLong(String value) {
        try {
            return (value == null || value.trim().isEmpty()) ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AssetImportDTO toAssetImportDTO(CSVRecord record) {
        return AssetImportDTO.builder()
                .name(record.get("Name"))
                .description(record.get("Description"))
                .status(record.get("Status"))
                .area(record.get("Area"))
                .locationName(record.get("Location Name"))
                .parentAssetName(record.get("Parent Asset"))
                .barCode(record.get("Barcode"))
                .category(record.get("Category"))
                .build();
    }

    private MeterImportDTO toMeterImportDTO(CSVRecord record) {
        return MeterImportDTO.builder()
                .name(record.get("Name"))
                .unit(record.get("Unit"))
                .updateFrequency(Integer.parseInt(record.get("Update Frequency")))
                .meterCategory(record.get("Category"))
                .locationName(record.get("Location Name"))
                .build();
    }

    private PartImportDTO toPartImportDTO(CSVRecord record) {
        return PartImportDTO.builder()
                .name(record.get("Name"))
                .cost(Double.parseDouble(record.get("Cost")))
                .category(record.get("Category"))
                .nonStock(record.get("Is a consumable?"))
                .description(record.get("Description"))
                .locationName(record.get("Location"))
                .quantity(Double.parseDouble(record.get("Quantity")))
                .barcode(record.get("Barcode").isEmpty() ? null : record.get("Barcode"))
                .build();
    }
}
