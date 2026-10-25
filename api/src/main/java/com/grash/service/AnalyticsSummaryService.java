package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.dto.DateRange;
import com.grash.dto.analytics.AnalyticsFilter;
import com.grash.dto.analytics.requests.RequestStats;
import com.grash.dto.analytics.summary.AnalyticsSummary;
import com.grash.dto.analytics.workOrders.WOStats;
import com.grash.exception.CustomException;
import com.grash.factory.StorageServiceFactory;
import com.grash.model.*;
import com.grash.model.enums.Status;
import com.grash.repository.AssetCategoryRepository;
import com.grash.repository.TeamRepository;
import com.grash.repository.WorkOrderCategoryRepository;
import com.grash.utils.Helper;
import com.grash.utils.PdfReportUtils;
import com.grash.utils.TextDirectionUtils;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maintenance summary of a period (issue #318, point 5): the main work order, maintenance, cost and request
 * figures compared with the previous period, the response times by priority and the assets with the lowest
 * availability, rendered as a PDF with the same pipeline as the work order report.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsSummaryService {

    static final int LOWEST_AVAILABILITY_ROWS = 10;
    private static final String TEMPLATE = "analytics-summary-report.html";

    private final WorkOrderService workOrderService;
    private final RequestService requestService;
    private final MaintenanceKpiService maintenanceKpiService;
    private final AnalyticsFilterService analyticsFilterService;
    private final CompanyService companyService;
    private final LocationService locationService;
    private final TeamRepository teamRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final WorkOrderCategoryRepository workOrderCategoryRepository;
    private final SpringTemplateEngine thymeleafTemplateEngine;
    private final ResourceBundleMessageSource emailMessageSource;
    private final StorageServiceFactory storageServiceFactory;
    private final BrandingService brandingService;

    /**
     * The summary PDF of the period and filters selected on the analytics pages.
     */
    public byte[] renderPdf(User user, Long companyId, DateRange dateRange) {
        if (dateRange == null || dateRange.getStart() == null || dateRange.getEnd() == null
                || dateRange.getStart().after(dateRange.getEnd())) {
            throw new CustomException("A valid start and end are required", HttpStatus.BAD_REQUEST);
        }
        Company company = companyService.findById(companyId)
                .orElseThrow(() -> new CustomException("Company not found", HttpStatus.NOT_FOUND));
        Locale locale = Helper.getLocale(user);
        AnalyticsFilter filter = analyticsFilterService.resolve(companyId, dateRange);
        AnalyticsSummary summary = build(companyId, dateRange.getStart(), dateRange.getEnd(), filter);
        return renderPdf(company, summary, describeFilters(company, dateRange.getFilterFields(), locale), locale);
    }

    public AnalyticsSummary build(Long companyId, Date start, Date end, AnalyticsFilter filter) {
        Date[] previous = previousPeriod(start, end);
        return new AnalyticsSummary(
                collect(companyId, start, end, filter),
                collect(companyId, previous[0], previous[1], filter),
                maintenanceKpiService.getAvailabilityByAsset(companyId, start, end, filter).stream()
                        .limit(LOWEST_AVAILABILITY_ROWS)
                        .toList());
    }

    public byte[] renderPdf(Company company, AnalyticsSummary summary, List<String> filters, Locale locale) {
        GeneralPreferences preferences = company.getCompanySettings().getGeneralPreferences();
        AnalyticsSummaryFormatter formatter = new AnalyticsSummaryFormatter(emailMessageSource, locale,
                preferences.getCurrency() == null ? "" : preferences.getCurrency().getCode());
        Function<Date, String> date = value -> Helper.getFormattedDate(value, preferences.getDateFormat(),
                preferences.getTimeZone());

        Map<String, Object> variables = new HashMap<>();
        variables.put("companyName", company.getName());
        variables.put("companyLogo", company.getLogo() == null ? null :
                PdfReportUtils.getImageReportStoragePath(company.getLogo()));
        variables.put("backgroundColor", PdfReportUtils.resolveReportColor(preferences.getColor(),
                brandingService.getMailBackgroundColor()));
        variables.put("dir", Helper.isRtl(company) ? "rtl" : "ltr");
        variables.put("period", date.apply(summary.current().start()) + " – "
                + date.apply(summary.current().end()));
        variables.put("previousPeriod", date.apply(summary.previous().start()) + " – "
                + date.apply(summary.previous().end()));
        variables.put("generatedOn", date.apply(new Date()));
        variables.put("filters", filters);
        variables.put("sections", formatter.sections(summary));
        variables.put("responseTimes", formatter.responseTimes(summary.current().kpis().getByPriority()));
        variables.put("assets", formatter.lowestAvailability(summary.lowestAvailability()));

        Context context = new Context(locale);
        context.setVariables(variables);
        String html = thymeleafTemplateEngine.process(TEMPLATE, context);
        if (Helper.isRtl(company)) html = TextDirectionUtils.transformHtmlText(html);

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, pdf, PdfReportUtils.createReportConverterProperties(
                storageServiceFactory.getStorageService()::download));
        return pdf.toByteArray();
    }

    AnalyticsSummary.Period collect(Long companyId, Date start, Date end, AnalyticsFilter filter) {
        List<WorkOrder> created = filter.apply(workOrderService.findByCompanyAndCreatedAtBetween(companyId, start,
                end));
        List<WorkOrder> completed = created.stream()
                .filter(workOrder -> workOrder.getStatus() == Status.COMPLETE)
                .toList();
        List<Request> requests = filter.apply(requestService.findByCreatedAtBetweenAndCompany(start, end,
                companyId));
        return new AnalyticsSummary.Period(start, end,
                workOrderStats(created),
                maintenanceKpiService.getKpis(companyId, start, end, filter),
                workOrderService.getCostsAndTime(completed),
                requestStats(requests));
    }

    /**
     * The period of the same length that ends right before {@code start}, as on the web: the bounds are inclusive,
     * so it stops 1 ms before {@code start}.
     */
    static Date[] previousPeriod(Date start, Date end) {
        long length = end.getTime() - start.getTime();
        return new Date[]{new Date(start.getTime() - length - 1), new Date(start.getTime() - 1)};
    }

    /**
     * "Location: ICU, Emergency"... for each filter, with the names of the selected items of the company only.
     */
    List<String> describeFilters(Company company, List<FilterField> filterFields, Locale locale) {
        if (filterFields == null || filterFields.isEmpty()) return List.of();
        Long companySettingsId = company.getCompanySettings().getId();
        List<String> lines = new ArrayList<>();
        for (FilterField filterField : filterFields) {
            List<String> names = switch (filterField.getField()) {
                case AnalyticsFilterService.LOCATION ->
                        locationService.findByIdsAndCompany(AnalyticsFilterService.ids(filterField), company.getId())
                                .stream().map(Location::getName).toList();
                case AnalyticsFilterService.ASSET_CATEGORY -> namesOf(
                        assetCategoryRepository.findByCompanySettings_Id(companySettingsId), filterField,
                        AssetCategory::getId, AssetCategory::getName);
                case AnalyticsFilterService.TEAM -> namesOf(teamRepository.findByCompany_Id(company.getId()),
                        filterField, Team::getId, Team::getName);
                case AnalyticsFilterService.CATEGORY -> namesOf(
                        workOrderCategoryRepository.findByCompanySettings_Id(companySettingsId), filterField,
                        WorkOrderCategory::getId, WorkOrderCategory::getName);
                case AnalyticsFilterService.PRIORITY -> AnalyticsFilterService.values(filterField).stream()
                        .map(value -> message(AnalyticsFilterService.priority(value).name(), locale))
                        .toList();
                default -> List.of();
            };
            if (!names.isEmpty()) {
                lines.add(message(FILTER_LABELS.get(filterField.getField()), locale) + ": "
                        + String.join(", ", names));
            }
        }
        return lines;
    }

    private static final Map<String, String> FILTER_LABELS = Map.of(
            AnalyticsFilterService.LOCATION, "summary_filter_location",
            AnalyticsFilterService.ASSET_CATEGORY, "summary_filter_asset_category",
            AnalyticsFilterService.TEAM, "summary_filter_team",
            AnalyticsFilterService.CATEGORY, "summary_filter_category",
            AnalyticsFilterService.PRIORITY, "summary_filter_priority");

    private static <T> List<String> namesOf(Collection<T> companyItems, FilterField filterField,
                                            Function<T, Long> id, Function<T, String> name) {
        Set<Long> selected = new HashSet<>(AnalyticsFilterService.ids(filterField));
        return companyItems.stream()
                .filter(item -> selected.contains(id.apply(item)))
                .map(name)
                .sorted()
                .toList();
    }

    private String message(String key, Locale locale) {
        return emailMessageSource.getMessage(key, null, key, locale);
    }

    /**
     * Overview of work orders: total, completed, compliant, mean time to acknowledge (hours) and average cycle
     * time (days).
     */
    public static WOStats workOrderStats(Collection<WorkOrder> workOrders) {
        Collection<WorkOrder> completed =
                workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).toList();
        // in fractional hours, averaged from minutes so that sub-hour responses are not counted as 0
        double mtta = workOrders.stream()
                .filter(workOrder -> workOrder.getFirstTimeToReact() != null)
                .mapToLong(workOrder -> Helper.getDateDiff(workOrder.getCreatedAt(), workOrder.getFirstTimeToReact(),
                        TimeUnit.MINUTES))
                .average().orElse(0) / 60;
        return WOStats.builder()
                .total(workOrders.size())
                .complete(completed.size())
                .compliant((int) completed.stream().filter(WorkOrder::isCompliant).count())
                .mtta(mtta)
                .avgCycleTime(WorkOrder.getAverageAge(completed))
                .build();
    }

    /**
     * Overview of requests: approved (with a work order), pending, cancelled and the average time to complete the
     * approved ones (days).
     */
    public static RequestStats requestStats(Collection<Request> requests) {
        List<Request> approved = requests.stream().filter(request -> request.getWorkOrder() != null).toList();
        long cancelled = requests.stream().filter(Request::isCancelled).count();
        long pending = requests.stream()
                .filter(request -> request.getWorkOrder() == null && !request.isCancelled())
                .count();
        List<WorkOrder> completeWorkOrders = approved.stream()
                .map(Request::getWorkOrder)
                .filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE))
                .collect(Collectors.toList());
        return RequestStats.builder()
                .approved(approved.size())
                .pending((int) pending)
                .cancelled((int) cancelled)
                .cycleTime(WorkOrder.getAverageAge(completeWorkOrders))
                .build();
    }
}
