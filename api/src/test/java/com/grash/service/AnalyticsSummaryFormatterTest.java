package com.grash.service;

import com.grash.dto.analytics.assets.AssetAvailability;
import com.grash.dto.analytics.requests.RequestStats;
import com.grash.dto.analytics.summary.AnalyticsSummary;
import com.grash.dto.analytics.workOrders.MaintenanceKpis;
import com.grash.dto.analytics.workOrders.PriorityResponseTimes;
import com.grash.dto.analytics.workOrders.WOCostsAndTime;
import com.grash.dto.analytics.workOrders.WOStats;
import com.grash.model.enums.Priority;
import com.grash.service.AnalyticsSummaryFormatter.Unit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.grash.service.AnalyticsSummaryFormatter.*;
import static org.junit.jupiter.api.Assertions.*;

class AnalyticsSummaryFormatterTest {

    private static final Locale SPANISH = new Locale("es", "ES");
    private final AnalyticsSummaryFormatter spanish = formatter(SPANISH);
    private final AnalyticsSummaryFormatter english = formatter(Locale.ENGLISH);

    @Nested
    class Format {

        @Test
        void valuesFollowTheLocale() {
            assertEquals("12.346", normalize(spanish.format(12345.6, Unit.COUNT)));
            assertEquals("85 %", normalize(spanish.format(85.4, Unit.PERCENT)));
            assertEquals("85%", normalize(english.format(85.4, Unit.PERCENT)));
            assertEquals("1,8 h", normalize(spanish.format(1.84, Unit.HOURS)));
            assertEquals("1.8 h", normalize(english.format(1.84, Unit.HOURS)));
            assertEquals("2,4 días", normalize(spanish.format(2.36, Unit.DAYS)));
            assertEquals("ARS 12.345,50", normalize(spanish.format(12345.5, Unit.CURRENCY)));
        }

        @Test
        void missingValue_isADash() {
            assertEquals("—", spanish.format(null, Unit.HOURS));
        }
    }

    @Nested
    class Change {

        @Test
        void relativeChange_coloredByWhetherItImproves() {
            assertChange("+20 %", GOOD, spanish.change(120.0, 100.0, Unit.COUNT, true));
            assertChange("−20 %", BAD, spanish.change(80.0, 100.0, Unit.COUNT, true));
            assertChange("−20 %", GOOD, spanish.change(80.0, 100.0, Unit.HOURS, false));
            assertChange("+10 %", NEUTRAL, spanish.change(110.0, 100.0, Unit.COUNT, null));
        }

        @Test
        void percentages_changeInPoints() {
            assertChange("+5 p. p.", GOOD, spanish.change(90.0, 85.0, Unit.PERCENT, true));
            assertChange("−5 pp", BAD, english.change(80.0, 85.0, Unit.PERCENT, true));
        }

        @Test
        void roundedToZero_isEqual() {
            assertChange("=", NEUTRAL, spanish.change(85.4, 85.0, Unit.PERCENT, true));
            assertChange("=", NEUTRAL, spanish.change(1000.0, 1001.0, Unit.COUNT, true));
        }

        @Test
        void withoutABase_isADash() {
            assertChange("—", NEUTRAL, spanish.change(5.0, 0.0, Unit.COUNT, true));
            assertChange("=", NEUTRAL, spanish.change(0.0, 0.0, Unit.COUNT, true));
            assertChange("—", NEUTRAL, spanish.change(null, 3.0, Unit.COUNT, true));
            assertChange("—", NEUTRAL, spanish.change(3.0, null, Unit.PERCENT, true));
        }

        private void assertChange(String text, String trend, String[] change) {
            assertEquals(text, normalize(change[0]));
            assertEquals(trend, change[1]);
        }
    }

    @Test
    void sections_compareEveryFigureWithThePreviousPeriod() {
        List<Section> sections = spanish.sections(summary());

        assertEquals(List.of("Órdenes de trabajo", "Mantenimiento", "Costos", "Solicitudes"),
                sections.stream().map(Section::getTitle).toList());
        Row created = sections.get(0).getRows().get(0);
        assertEquals("Creadas", created.getLabel());
        assertEquals("75", created.getCurrent());
        assertEquals("70", created.getPrevious());
        assertEquals("+7 %", normalize(created.getChange()));
        assertEquals(NEUTRAL, created.getTrend());
        // 44 / 52 = 84.6 % against 40 / 55 = 72.7 %
        Row onTime = sections.get(0).getRows().get(2);
        assertEquals("85 %", normalize(onTime.getCurrent()));
        assertEquals("+12 p. p.", normalize(onTime.getChange()));
        assertEquals(GOOD, onTime.getTrend());
        // nothing was due in the previous period
        Row pmCompliance = sections.get(1).getRows().get(0);
        assertEquals("—", pmCompliance.getPrevious());
        assertEquals(NEUTRAL, pmCompliance.getTrend());
        Row backlog = sections.get(1).getRows().get(2);
        assertEquals(BAD, backlog.getTrend());
        Row received = sections.get(3).getRows().get(0);
        assertEquals("10", received.getCurrent());
    }

    @Test
    void responseTimesAndAssets() {
        List<PriorityRow> times = spanish.responseTimes(List.of(PriorityResponseTimes.builder()
                .priority(Priority.HIGH).count(8).completed(7).mtta(0.75).mttr(null).build()));
        List<AssetRow> assets = spanish.lowestAvailability(List.of(
                AssetAvailability.builder().name("Respirador").availability(71.4).downtime(7200).downtimeEvents(1)
                        .build(),
                AssetAvailability.builder().name("Monitor").availability(100).build()));

        assertEquals("Alta", times.get(0).getPriority());
        assertEquals("0,8 h", normalize(times.get(0).getResponseTime()));
        assertEquals("—", times.get(0).getResolutionTime());
        assertTrue(assets.get(0).isLow());
        assertEquals("2 h", normalize(assets.get(0).getDowntime()));
        assertFalse(assets.get(1).isLow());
    }

    private static AnalyticsSummary summary() {
        AnalyticsSummary.Period current = new AnalyticsSummary.Period(new Date(0), new Date(1),
                WOStats.builder().total(75).complete(52).compliant(44).avgCycleTime(2.36).mtta(1.84).build(),
                MaintenanceKpis.builder().pmDue(20).pmCompletedOnTime(17).pmCompliance(85.0).plannedRatio(40.0)
                        .backlog(38).backlogHours(112.5).overdue(9).build(),
                WOCostsAndTime.builder().total(1000).laborCost(600).partCost(300).additionalCost(100)
                        .laborTime(36000).build(),
                RequestStats.builder().approved(6).pending(3).cancelled(1).cycleTime(1.5).build());
        AnalyticsSummary.Period previous = new AnalyticsSummary.Period(new Date(0), new Date(1),
                WOStats.builder().total(70).complete(55).compliant(40).avgCycleTime(3.1).mtta(1.84).build(),
                MaintenanceKpis.builder().pmCompliance(null).plannedRatio(34.3).backlog(31).backlogHours(90)
                        .overdue(12).build(),
                WOCostsAndTime.builder().total(1200).laborCost(700).partCost(400).additionalCost(100)
                        .laborTime(30000).build(),
                RequestStats.builder().approved(5).pending(4).cancelled(0).cycleTime(2).build());
        return new AnalyticsSummary(current, previous, List.of());
    }

    private static AnalyticsSummaryFormatter formatter(Locale locale) {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("mailMessages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        return new AnalyticsSummaryFormatter(messages, locale, "ARS");
    }

    // the JDK separates numbers from % and units with non-breaking spaces in some locales
    private static String normalize(String text) {
        return text.replace(' ', ' ').replace(' ', ' ');
    }
}
