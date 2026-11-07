package com.grash.service;

import com.grash.dto.analytics.meters.*;
import com.grash.exception.CustomException;
import com.grash.model.Meter;
import com.grash.model.WorkOrderMeterTrigger;
import com.grash.model.enums.WorkOrderMeterTriggerCondition;
import com.grash.repository.MeterRepository;
import com.grash.repository.ReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Meter analytics (issue #318, point 6): the readings of a meter grouped by hour, day, week or month in the time
 * zone of the company, compared with the limits of its work order triggers and, for cumulative meters (hour meters,
 * energy meters...), turned into consumption per period and consumption between failures of the asset.
 */
@Service
@RequiredArgsConstructor
public class MeterAnalyticsService {

    // keeps the trend readable and bounded whatever the period
    static final int MAX_POINTS = 400;

    private final MeterRepository meterRepository;
    private final ReadingRepository readingRepository;
    private final WorkOrderMeterTriggerService workOrderMeterTriggerService;
    private final AssetDowntimeService assetDowntimeService;

    /**
     * The meters of the company, by name.
     */
    @Transactional(readOnly = true)
    public List<MeterOption> getMeters(Long companyId) {
        return meterRepository.findByCompany_Id(companyId).stream()
                .map(meter -> MeterOption.builder()
                        .id(meter.getId())
                        .name(meter.getName())
                        .unit(meter.getUnit())
                        .cumulative(meter.isCumulative())
                        .assetName(meter.getAsset() == null ? null : meter.getAsset().getName())
                        .build())
                .sorted(Comparator.comparing(MeterOption::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    @Transactional(readOnly = true)
    public MeterAnalytics getAnalytics(Long companyId, Long meterId, Date start, Date end) {
        if (start == null || end == null || start.after(end)) {
            throw new CustomException("A valid start and end are required", HttpStatus.BAD_REQUEST);
        }
        Meter meter = meterRepository.findByIdAndCompany_Id(meterId, companyId)
                .orElseThrow(() -> new CustomException("Meter not found", HttpStatus.NOT_FOUND));
        // no empty points for the future
        Date effectiveEnd = end.after(new Date()) ? new Date() : end;
        List<ReadingPoint> readings = readingRepository.findDatesAndValues(meterId, start, effectiveEnd).stream()
                .map(row -> new ReadingPoint((Date) row[0], ((Number) row[1]).doubleValue()))
                .toList();
        // the consumption of the first reading of the period counts from the reading before it
        ReadingPoint before = !meter.isCumulative() ? null :
                readingRepository.findFirstByMeter_IdAndCreatedAtBeforeOrderByCreatedAtDesc(meterId, start)
                        .map(reading -> new ReadingPoint(reading.getCreatedAt(), reading.getValue()))
                        .orElse(null);
        int failures = meter.getAsset() == null ? 0 :
                assetDowntimeService.findByAssetAndStartsOnBetween(meter.getAsset().getId(), start, effectiveEnd)
                        .size();
        return compute(meter, readings, before, workOrderMeterTriggerService.findByMeter(meterId), failures, start,
                effectiveEnd, zone(meter));
    }

    /**
     * @param readings oldest first
     * @param before   for cumulative meters, the last reading before the period, if any
     * @param failures downtimes of the asset that started in the period
     */
    static MeterAnalytics compute(Meter meter, List<ReadingPoint> readings, ReadingPoint before,
                                  Collection<WorkOrderMeterTrigger> triggers, int failures, Date start, Date end,
                                  ZoneId zone) {
        List<MeterThreshold> thresholds = triggers.stream()
                .map(trigger -> MeterThreshold.builder()
                        .name(trigger.getName())
                        .condition(trigger.getTriggerCondition())
                        .value(trigger.getValue())
                        .build())
                .sorted(Comparator.comparingDouble(MeterThreshold::getValue))
                .toList();
        MeterInterval interval = intervalFor(start, end);
        List<Instant> bucketStarts = bucketStarts(start, end, interval, zone);
        List<Bucket> buckets = bucketStarts.stream().map(bucketStart -> new Bucket()).toList();

        Bucket total = new Bucket();
        double consumption = 0;
        int resets = 0;
        ReadingPoint previous = before;
        int index = 0;
        for (ReadingPoint reading : readings) {
            Instant date = reading.date().toInstant();
            while (index + 1 < bucketStarts.size() && !date.isBefore(bucketStarts.get(index + 1))) index++;
            Bucket bucket = buckets.isEmpty() || date.isBefore(bucketStarts.get(0)) ? null : buckets.get(index);
            boolean outOfRange = isOutOfRange(reading.value(), thresholds);
            total.add(reading.value(), outOfRange);
            if (bucket != null) bucket.add(reading.value(), outOfRange);
            if (meter.isCumulative() && previous != null) {
                double increase = reading.value() - previous.value();
                if (increase < 0) {
                    // the counter was reset or replaced: it counts again from 0
                    resets++;
                    increase = Math.max(0, reading.value());
                }
                consumption += increase;
                if (bucket != null) bucket.consumption += increase;
            }
            previous = reading;
        }

        List<MeterAnalyticsPoint> points = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            Bucket bucket = buckets.get(i);
            points.add(MeterAnalyticsPoint.builder()
                    .start(Date.from(bucketStarts.get(i)))
                    .count(bucket.count)
                    .min(bucket.min())
                    .average(bucket.average())
                    .max(bucket.max())
                    .last(bucket.last())
                    .consumption(meter.isCumulative() ? bucket.consumption : null)
                    .outOfRange(bucket.outOfRange)
                    .build());
        }
        ReadingPoint last = readings.isEmpty() ? null : readings.get(readings.size() - 1);
        return MeterAnalytics.builder()
                .meterId(meter.getId())
                .name(meter.getName())
                .unit(meter.getUnit())
                .cumulative(meter.isCumulative())
                .assetId(meter.getAsset() == null ? null : meter.getAsset().getId())
                .assetName(meter.getAsset() == null ? null : meter.getAsset().getName())
                .interval(interval)
                .points(points)
                .thresholds(thresholds)
                .readings(total.count)
                .min(total.min())
                .average(total.average())
                .max(total.max())
                .lastValue(last == null ? null : last.value())
                .lastReadingAt(last == null ? null : last.date())
                .outOfRange(total.outOfRange)
                // unknown without readings
                .consumption(meter.isCumulative() && !readings.isEmpty() ? consumption : null)
                .resets(resets)
                .failures(failures)
                .consumptionBetweenFailures(meter.isCumulative() && !readings.isEmpty() && failures > 0 ?
                        consumption / failures : null)
                .build();
    }

    /**
     * Hours up to 2 days, days up to 2 months, weeks up to a year, months beyond.
     */
    static MeterInterval intervalFor(Date start, Date end) {
        Duration length = Duration.between(start.toInstant(), end.toInstant());
        if (length.compareTo(Duration.ofDays(2)) <= 0) return MeterInterval.HOUR;
        if (length.compareTo(Duration.ofDays(62)) <= 0) return MeterInterval.DAY;
        if (length.compareTo(Duration.ofDays(366)) <= 0) return MeterInterval.WEEK;
        return MeterInterval.MONTH;
    }

    /**
     * Starts of the intervals covering [start, end], aligned on the hour, local midnight, Monday or the 1st of the
     * month in {@code zone}. The first one can be before {@code start}.
     */
    static List<Instant> bucketStarts(Date start, Date end, MeterInterval interval, ZoneId zone) {
        List<Instant> starts = new ArrayList<>();
        ZonedDateTime localStart = start.toInstant().atZone(zone);
        ZonedDateTime current = switch (interval) {
            case HOUR -> localStart.truncatedTo(ChronoUnit.HOURS);
            case DAY -> localStart.toLocalDate().atStartOfDay(zone);
            case WEEK -> localStart.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay(zone);
            case MONTH -> localStart.toLocalDate().withDayOfMonth(1).atStartOfDay(zone);
        };
        Instant endInstant = end.toInstant();
        while (!current.toInstant().isAfter(endInstant) && starts.size() < MAX_POINTS) {
            starts.add(current.toInstant());
            current = switch (interval) {
                // on the time line, so that the repeated or skipped hour of a daylight saving change is kept
                case HOUR -> current.plusHours(1);
                // from the local date, so that every point starts at midnight
                case DAY -> current.toLocalDate().plusDays(1).atStartOfDay(zone);
                case WEEK -> current.toLocalDate().plusWeeks(1).atStartOfDay(zone);
                case MONTH -> current.toLocalDate().plusMonths(1).atStartOfDay(zone);
            };
        }
        return starts;
    }

    static boolean isOutOfRange(double value, List<MeterThreshold> thresholds) {
        return thresholds.stream().anyMatch(threshold ->
                threshold.getCondition() == WorkOrderMeterTriggerCondition.LESS_THAN ? value < threshold.getValue()
                        : value > threshold.getValue());
    }

    private static ZoneId zone(Meter meter) {
        try {
            return ZoneId.of(meter.getCompany().getCompanySettings().getGeneralPreferences().getTimeZone());
        } catch (RuntimeException e) {
            return ZoneId.systemDefault();
        }
    }

    private static class Bucket {
        int count;
        int outOfRange;
        double sum;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double last;
        double consumption;

        void add(double value, boolean outOfRange) {
            count++;
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
            last = value;
            if (outOfRange) this.outOfRange++;
        }

        Double min() {
            return count == 0 ? null : min;
        }

        Double max() {
            return count == 0 ? null : max;
        }

        Double average() {
            return count == 0 ? null : sum / count;
        }

        Double last() {
            return count == 0 ? null : last;
        }
    }
}
