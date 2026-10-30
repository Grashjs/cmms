package com.grash.service;

import com.grash.dto.analytics.meters.*;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.WorkOrderMeterTriggerCondition;
import com.grash.repository.MeterRepository;
import com.grash.repository.ReadingRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeterAnalyticsServiceTest {

    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    // 01/10/2026 00:00 to 03/10/2026 23:59:59.999 in Buenos Aires
    private static final Date START = date("2026-10-01T03:00:00Z");
    private static final Date END = date("2026-10-04T02:59:59.999Z");

    @Mock
    private MeterRepository meterRepository;
    @Mock
    private ReadingRepository readingRepository;
    @Mock
    private WorkOrderMeterTriggerService workOrderMeterTriggerService;
    @Mock
    private AssetDowntimeService assetDowntimeService;

    @InjectMocks
    private MeterAnalyticsService meterAnalyticsService;

    @Nested
    class Compute {

        @Test
        void readings_areGroupedByLocalDayAndComparedWithTheTriggers() {
            List<WorkOrderMeterTrigger> triggers = List.of(
                    trigger("High", WorkOrderMeterTriggerCondition.MORE_THAN, 8),
                    trigger("Low", WorkOrderMeterTriggerCondition.LESS_THAN, 3));

            MeterAnalytics result = MeterAnalyticsService.compute(meter(false), List.of(
                    reading("2026-10-01T13:00:00Z", 5),
                    // 20:00 on the 1st in Buenos Aires
                    reading("2026-10-01T23:00:00Z", 9),
                    // 02:00 on the 2nd in Buenos Aires
                    reading("2026-10-02T05:00:00Z", 2),
                    reading("2026-10-03T12:00:00Z", 4.5)), null, triggers, 1, START, END, BUENOS_AIRES);

            assertEquals(MeterInterval.DAY, result.getInterval());
            assertEquals(List.of(START, date("2026-10-02T03:00:00Z"), date("2026-10-03T03:00:00Z")),
                    result.getPoints().stream().map(MeterAnalyticsPoint::getStart).toList());
            MeterAnalyticsPoint first = result.getPoints().get(0);
            assertEquals(2, first.getCount());
            assertEquals(5.0, first.getMin());
            assertEquals(7.0, first.getAverage());
            assertEquals(9.0, first.getMax());
            assertEquals(9.0, first.getLast());
            assertEquals(1, first.getOutOfRange());
            assertNull(first.getConsumption());
            assertEquals(1, result.getPoints().get(1).getOutOfRange());

            assertEquals(4, result.getReadings());
            assertEquals(2.0, result.getMin());
            assertEquals(9.0, result.getMax());
            assertEquals(5.125, result.getAverage(), 1e-9);
            assertEquals(4.5, result.getLastValue());
            assertEquals(date("2026-10-03T12:00:00Z"), result.getLastReadingAt());
            assertEquals(2, result.getOutOfRange());
            assertEquals(List.of("Low", "High"), result.getThresholds().stream().map(MeterThreshold::getName).toList());
            assertNull(result.getConsumption());
            assertNull(result.getConsumptionBetweenFailures());
            assertEquals("Compressor 1", result.getAssetName());
        }

        @Test
        void cumulativeMeter_consumptionPerDayWithAResetAndBetweenFailures() {
            MeterAnalytics result = MeterAnalyticsService.compute(meter(true), List.of(
                            reading("2026-10-01T12:00:00Z", 1010),
                            reading("2026-10-01T20:00:00Z", 1018),
                            // counter replaced
                            reading("2026-10-02T12:00:00Z", 5),
                            reading("2026-10-02T20:00:00Z", 20),
                            reading("2026-10-03T12:00:00Z", 30)),
                    reading("2026-09-30T12:00:00Z", 1000), List.of(), 2, START, END, BUENOS_AIRES);

            assertEquals(List.of(18.0, 20.0, 10.0),
                    result.getPoints().stream().map(MeterAnalyticsPoint::getConsumption).toList());
            assertEquals(48.0, result.getConsumption());
            assertEquals(1, result.getResets());
            assertEquals(2, result.getFailures());
            assertEquals(24.0, result.getConsumptionBetweenFailures());
            assertEquals(30.0, result.getLastValue());
        }

        @Test
        void cumulativeMeter_withoutAPreviousReading_startsCountingFromTheFirstOne() {
            MeterAnalytics result = MeterAnalyticsService.compute(meter(true), List.of(
                    reading("2026-10-01T12:00:00Z", 1010),
                    reading("2026-10-02T12:00:00Z", 1030)), null, List.of(), 0, START, END, BUENOS_AIRES);

            assertEquals(20.0, result.getConsumption());
            assertNull(result.getConsumptionBetweenFailures());
        }

        @Test
        void noReadings_emptyPointsAndNoConsumption() {
            MeterAnalytics result = MeterAnalyticsService.compute(meter(true), List.of(), null, List.of(), 0, START,
                    date("2026-10-02T02:59:59.999Z"), BUENOS_AIRES);

            assertEquals(MeterInterval.HOUR, result.getInterval());
            assertEquals(24, result.getPoints().size());
            assertEquals(0, result.getReadings());
            assertNull(result.getMin());
            assertNull(result.getAverage());
            assertNull(result.getConsumption());
        }
    }

    @Nested
    class Intervals {

        @Test
        void intervalFollowsTheLengthOfThePeriod() {
            Date start = date("2026-08-01T00:00:00Z");
            assertEquals(MeterInterval.HOUR, MeterAnalyticsService.intervalFor(start, date("2026-08-03T00:00:00Z")));
            assertEquals(MeterInterval.DAY, MeterAnalyticsService.intervalFor(start, date("2026-08-03T00:00:01Z")));
            assertEquals(MeterInterval.DAY, MeterAnalyticsService.intervalFor(start, date("2026-10-02T00:00:00Z")));
            assertEquals(MeterInterval.WEEK, MeterAnalyticsService.intervalFor(start, date("2026-10-02T00:00:01Z")));
            assertEquals(MeterInterval.WEEK, MeterAnalyticsService.intervalFor(start, date("2027-08-02T00:00:00Z")));
            assertEquals(MeterInterval.MONTH, MeterAnalyticsService.intervalFor(start, date("2027-08-02T00:00:01Z")));
        }

        @Test
        void days_startAtLocalMidnightAcrossADaylightSavingChange() {
            // summer time ended in Madrid on 25/10/2026
            assertEquals(List.of(instant("2026-10-23T22:00:00Z"), instant("2026-10-24T22:00:00Z"),
                            instant("2026-10-25T23:00:00Z"), instant("2026-10-26T23:00:00Z")),
                    MeterAnalyticsService.bucketStarts(date("2026-10-23T22:00:00Z"), date("2026-10-27T22:59:59.999Z"),
                            MeterInterval.DAY, MADRID));
        }

        @Test
        void hours_keepTheRepeatedHour() {
            // 00:00 to 03:00 in Madrid on 25/10/2026: 02:00 happens twice
            assertEquals(6, MeterAnalyticsService.bucketStarts(date("2026-10-24T22:00:00Z"),
                    date("2026-10-25T03:00:00Z"), MeterInterval.HOUR, MADRID).size());
        }

        @Test
        void weeksStartOnMonday_monthsOnThe1st() {
            assertEquals(List.of(instant("2026-10-05T03:00:00Z"), instant("2026-10-12T03:00:00Z"),
                            instant("2026-10-19T03:00:00Z")),
                    MeterAnalyticsService.bucketStarts(date("2026-10-07T15:00:00Z"), date("2026-10-20T15:00:00Z"),
                            MeterInterval.WEEK, BUENOS_AIRES));
            assertEquals(List.of(instant("2025-11-01T03:00:00Z"), instant("2025-12-01T03:00:00Z"),
                            instant("2026-01-01T03:00:00Z"), instant("2026-02-01T03:00:00Z")),
                    MeterAnalyticsService.bucketStarts(date("2025-11-15T15:00:00Z"), date("2026-02-10T15:00:00Z"),
                            MeterInterval.MONTH, BUENOS_AIRES));
        }
    }

    @Nested
    class GetAnalytics {

        @Test
        void loadsTheMeterOfTheCompanyAndThePreviousReadingOfCumulativeMeters() {
            Meter meter = meter(true);
            when(meterRepository.findByIdAndCompany_Id(9L, 1L)).thenReturn(Optional.of(meter));
            when(readingRepository.findDatesAndValues(9L, START, END)).thenReturn(List.<Object[]>of(
                    new Object[]{date("2026-10-01T12:00:00Z"), 1010.0}));
            Reading previous = new Reading();
            previous.setCreatedAt(date("2026-09-30T12:00:00Z"));
            previous.setValue(1000);
            when(readingRepository.findFirstByMeter_IdAndCreatedAtBeforeOrderByCreatedAtDesc(9L, START))
                    .thenReturn(Optional.of(previous));
            when(workOrderMeterTriggerService.findByMeter(9L)).thenReturn(List.of());
            when(assetDowntimeService.findByAssetAndStartsOnBetween(3L, START, END))
                    .thenReturn(List.of(new AssetDowntime()));

            MeterAnalytics result = meterAnalyticsService.getAnalytics(1L, 9L, START, END);

            assertEquals(10.0, result.getConsumption());
            assertEquals(1, result.getFailures());
            assertEquals(10.0, result.getConsumptionBetweenFailures());
        }

        @Test
        void futureEnd_stopsNow() {
            Meter meter = meter(false);
            when(meterRepository.findByIdAndCompany_Id(9L, 1L)).thenReturn(Optional.of(meter));
            Date start = new Date(System.currentTimeMillis() - 3_600_000L);
            Date nextYear = new Date(System.currentTimeMillis() + 365L * 24 * 3_600_000L);

            MeterAnalytics result = meterAnalyticsService.getAnalytics(1L, 9L, start, nextYear);

            assertEquals(MeterInterval.HOUR, result.getInterval());
            verify(readingRepository).findDatesAndValues(eq(9L), eq(start), argThatIsBefore(nextYear));
            verify(readingRepository, never()).findFirstByMeter_IdAndCreatedAtBeforeOrderByCreatedAtDesc(any(), any());
        }

        @Test
        void meterOfAnotherCompany_isNotFound() {
            when(meterRepository.findByIdAndCompany_Id(9L, 1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> meterAnalyticsService.getAnalytics(1L, 9L, START, END));
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        void invalidRange_isABadRequest() {
            CustomException exception = assertThrows(CustomException.class,
                    () -> meterAnalyticsService.getAnalytics(1L, 9L, END, START));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
            verifyNoInteractions(meterRepository);
        }

        @Test
        void getMeters_byName() {
            Meter energy = meter(true);
            energy.setName("energy");
            Meter boiler = meter(false);
            boiler.setName("Boiler temperature");
            when(meterRepository.findByCompany_Id(1L)).thenReturn(List.of(energy, boiler));

            List<MeterOption> options = meterAnalyticsService.getMeters(1L);

            assertEquals(List.of("Boiler temperature", "energy"), options.stream().map(MeterOption::getName).toList());
            assertTrue(options.get(1).isCumulative());
            assertEquals("Compressor 1", options.get(0).getAssetName());
        }

        private Date argThatIsBefore(Date limit) {
            return org.mockito.ArgumentMatchers.argThat(date -> date.before(limit));
        }
    }

    private static Meter meter(boolean cumulative) {
        Company company = new Company();
        company.setId(1L);
        company.getCompanySettings().getGeneralPreferences().setTimeZone(BUENOS_AIRES.getId());
        Asset asset = new Asset();
        asset.setId(3L);
        asset.setName("Compressor 1");
        Meter meter = new Meter();
        meter.setId(9L);
        meter.setName("Hour meter");
        meter.setUnit("h");
        meter.setCumulative(cumulative);
        meter.setAsset(asset);
        meter.setCompany(company);
        return meter;
    }

    private static WorkOrderMeterTrigger trigger(String name, WorkOrderMeterTriggerCondition condition, int value) {
        WorkOrderMeterTrigger trigger = new WorkOrderMeterTrigger();
        trigger.setName(name);
        trigger.setTriggerCondition(condition);
        trigger.setValue(value);
        return trigger;
    }

    private static ReadingPoint reading(String date, double value) {
        return new ReadingPoint(date(date), value);
    }

    private static Date date(String instant) {
        return Date.from(Instant.parse(instant));
    }

    private static Instant instant(String instant) {
        return Instant.parse(instant);
    }
}
