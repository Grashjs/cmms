package com.grash.service;

import com.grash.dto.analytics.assets.AssetAvailability;
import com.grash.dto.analytics.summary.AnalyticsSummary;
import com.grash.dto.analytics.workOrders.MaintenanceKpis;
import com.grash.dto.analytics.workOrders.PriorityResponseTimes;
import com.grash.dto.analytics.workOrders.WOCostsAndTime;
import com.grash.dto.analytics.workOrders.WOStats;
import com.grash.dto.analytics.requests.RequestStats;
import lombok.Value;
import org.springframework.context.MessageSource;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Turns an {@link AnalyticsSummary} into the localized rows of the summary report, with the change against the
 * previous period. Kept out of the template so that the rules are the same as on the web (see DeltaBadge) and can
 * be tested.
 */
public class AnalyticsSummaryFormatter {

    public static final String GOOD = "good";
    public static final String BAD = "bad";
    public static final String NEUTRAL = "neutral";
    private static final String NO_VALUE = "—";
    private static final String MINUS = "−";

    public enum Unit {COUNT, PERCENT, HOURS, DAYS, CURRENCY}

    @Value
    public static class Section {
        String title;
        List<Row> rows;
    }

    @Value
    public static class Row {
        String label;
        String current;
        String previous;
        String change;
        // GOOD, BAD or NEUTRAL
        String trend;
    }

    @Value
    public static class PriorityRow {
        String priority;
        String count;
        String completed;
        String responseTime;
        String resolutionTime;
    }

    @Value
    public static class AssetRow {
        String name;
        String availability;
        String downtime;
        String downtimeEvents;
        boolean low;
    }

    // availabilities below this percentage are highlighted, as on the web
    static final double AVAILABILITY_THRESHOLD = 95;

    private final MessageSource messages;
    private final Locale locale;
    private final String currencyCode;

    public AnalyticsSummaryFormatter(MessageSource messages, Locale locale, String currencyCode) {
        this.messages = messages;
        this.locale = locale;
        this.currencyCode = currencyCode;
    }

    public List<Section> sections(AnalyticsSummary summary) {
        AnalyticsSummary.Period current = summary.current();
        AnalyticsSummary.Period previous = summary.previous();
        return List.of(
                section("summary_work_orders", current.workOrders(), previous.workOrders(), List.of(
                        row("summary_wo_created", (WOStats stats) -> (double) stats.getTotal(), Unit.COUNT, null),
                        row("summary_wo_completed", (WOStats stats) -> (double) stats.getComplete(), Unit.COUNT, true),
                        row("summary_wo_compliance", AnalyticsSummaryFormatter::complianceRate, Unit.PERCENT, true),
                        row("summary_cycle_time", WOStats::getAvgCycleTime, Unit.DAYS, false),
                        row("summary_mtta", WOStats::getMtta, Unit.HOURS, false))),
                section("summary_maintenance", current.kpis(), previous.kpis(), List.of(
                        row("summary_pm_compliance", MaintenanceKpis::getPmCompliance, Unit.PERCENT, true),
                        row("summary_planned_work", MaintenanceKpis::getPlannedRatio, Unit.PERCENT, true),
                        row("summary_backlog", (MaintenanceKpis kpis) -> (double) kpis.getBacklog(), Unit.COUNT, false),
                        row("summary_backlog_hours", MaintenanceKpis::getBacklogHours, Unit.HOURS, false),
                        row("summary_overdue", (MaintenanceKpis kpis) -> (double) kpis.getOverdue(), Unit.COUNT, false))),
                section("summary_costs", current.costs(), previous.costs(), List.of(
                        row("summary_total_cost", WOCostsAndTime::getTotal, Unit.CURRENCY, false),
                        row("summary_labor_cost", WOCostsAndTime::getLaborCost, Unit.CURRENCY, false),
                        row("summary_part_cost", WOCostsAndTime::getPartCost, Unit.CURRENCY, false),
                        row("summary_additional_cost", WOCostsAndTime::getAdditionalCost, Unit.CURRENCY, false),
                        row("summary_labor_hours", (WOCostsAndTime costs) -> costs.getLaborTime() / 3600.0, Unit.HOURS, null))),
                section("summary_requests", current.requests(), previous.requests(), List.of(
                        row("summary_requests_received",
                                (RequestStats requests) -> (double) (requests.getApproved() + requests.getPending()
                                        + requests.getCancelled()), Unit.COUNT, null),
                        row("summary_requests_approved", (RequestStats requests) -> (double) requests.getApproved(), Unit.COUNT,
                                null),
                        row("summary_requests_pending", (RequestStats requests) -> (double) requests.getPending(), Unit.COUNT,
                                false),
                        row("summary_requests_cancelled", (RequestStats requests) -> (double) requests.getCancelled(), Unit.COUNT,
                                null),
                        row("summary_requests_cycle_time", RequestStats::getCycleTime, Unit.DAYS, false))));
    }

    public List<PriorityRow> responseTimes(List<PriorityResponseTimes> byPriority) {
        if (byPriority == null) return List.of();
        return byPriority.stream().map(times -> new PriorityRow(
                message(times.getPriority().name()),
                format(times.getCount(), Unit.COUNT),
                format(times.getCompleted(), Unit.COUNT),
                format(times.getMtta(), Unit.HOURS),
                format(times.getMttr(), Unit.HOURS))).toList();
    }

    public List<AssetRow> lowestAvailability(List<AssetAvailability> assets) {
        return assets.stream().map(asset -> new AssetRow(
                asset.getName(),
                format(asset.getAvailability(), Unit.PERCENT),
                format(asset.getDowntime() / 3600.0, Unit.HOURS),
                format(asset.getDowntimeEvents(), Unit.COUNT),
                asset.getAvailability() < AVAILABILITY_THRESHOLD)).toList();
    }

    /**
     * The value formatted for the locale; an em dash when there is no value.
     */
    public String format(Number value, Unit unit) {
        if (value == null) return NO_VALUE;
        double number = value.doubleValue();
        return switch (unit) {
            case COUNT -> numberFormat(0).format(number);
            case PERCENT -> percentFormat().format(number / 100);
            case HOURS -> messages.getMessage("summary_hours", new Object[]{numberFormat(1).format(number)}, locale);
            case DAYS -> messages.getMessage("summary_days", new Object[]{numberFormat(1).format(number)}, locale);
            case CURRENCY -> currencyCode + " " + numberFormat(2).format(number);
        };
    }

    /**
     * Change of a value against the previous period: relative, or in percentage points for {@link Unit#PERCENT}.
     *
     * @param higherIsBetter whether an increase is an improvement; null when it is neither
     * @return the text and the trend ({@link #GOOD}, {@link #BAD} or {@link #NEUTRAL})
     */
    public String[] change(Double current, Double previous, Unit unit, Boolean higherIsBetter) {
        if (current == null || previous == null) return new String[]{NO_VALUE, NEUTRAL};
        long rounded;
        String text;
        if (unit == Unit.PERCENT) {
            rounded = Math.round(current - previous);
            text = Math.abs(rounded) + " " + message("summary_points");
        } else {
            if (previous == 0) return new String[]{current == 0 ? "=" : NO_VALUE, NEUTRAL};
            rounded = Math.round((current - previous) * 100 / Math.abs(previous));
            text = percentFormat().format(Math.abs(rounded) / 100.0);
        }
        if (rounded == 0) return new String[]{"=", NEUTRAL};
        String trend = higherIsBetter == null ? NEUTRAL : (rounded > 0) == higherIsBetter ? GOOD : BAD;
        return new String[]{(rounded > 0 ? "+" : MINUS) + text, trend};
    }

    private <T> Section section(String title, T current, T previous, List<RowSpec<T>> specs) {
        List<Row> rows = new ArrayList<>();
        for (RowSpec<T> spec : specs) {
            Double currentValue = current == null ? null : spec.value().apply(current);
            Double previousValue = previous == null ? null : spec.value().apply(previous);
            String[] change = change(currentValue, previousValue, spec.unit(), spec.higherIsBetter());
            rows.add(new Row(message(spec.label()), format(currentValue, spec.unit()),
                    format(previousValue, spec.unit()), change[0], change[1]));
        }
        return new Section(message(title), rows);
    }

    private static <T> RowSpec<T> row(String label, Function<T, Double> value, Unit unit, Boolean higherIsBetter) {
        return new RowSpec<>(label, value, unit, higherIsBetter);
    }

    private record RowSpec<T>(String label, Function<T, Double> value, Unit unit, Boolean higherIsBetter) {
    }

    private static Double complianceRate(WOStats stats) {
        return stats.getComplete() == 0 ? null : stats.getCompliant() * 100.0 / stats.getComplete();
    }

    private String message(String key) {
        return messages.getMessage(key, null, key, locale);
    }

    private NumberFormat numberFormat(int maxDecimals) {
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMinimumFractionDigits(maxDecimals == 2 ? 2 : 0);
        format.setMaximumFractionDigits(maxDecimals);
        return format;
    }

    private NumberFormat percentFormat() {
        NumberFormat format = NumberFormat.getPercentInstance(locale);
        format.setMaximumFractionDigits(0);
        return format;
    }
}
