package com.grash.service;

import com.grash.dto.EmailAttachmentDTO;
import com.grash.dto.analytics.AnalyticsFilter;
import com.grash.dto.analytics.requests.RequestStats;
import com.grash.dto.analytics.summary.AnalyticsSummary;
import com.grash.dto.analytics.workOrders.MaintenanceKpis;
import com.grash.dto.analytics.workOrders.WOCostsAndTime;
import com.grash.dto.analytics.workOrders.WOStats;
import com.grash.factory.MailServiceFactory;
import com.grash.model.*;
import com.grash.model.enums.AnalyticsSummaryFrequency;
import com.grash.model.enums.Language;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.repository.UserRepository;
import com.grash.service.AnalyticsSummaryEmailService.ScheduledPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.grash.model.enums.AnalyticsSummaryFrequency.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsSummaryEmailServiceTest {

    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    // a Monday
    private static final Instant MONDAY_7_UTC = Instant.parse("2026-10-26T07:00:00Z");
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};

    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyService companyService;
    @Mock
    private AnalyticsSummaryService analyticsSummaryService;
    @Mock
    private MailServiceFactory mailServiceFactory;
    @Mock
    private MailService mailService;
    @Mock
    private PlatformTransactionManager transactionManager;

    @Captor
    private ArgumentCaptor<String[]> recipients;
    @Captor
    private ArgumentCaptor<String> subjects;
    @Captor
    private ArgumentCaptor<Map<String, Object>> variables;
    @Captor
    private ArgumentCaptor<Locale> locales;
    @Captor
    private ArgumentCaptor<List<EmailAttachmentDTO>> attachments;

    private ResourceBundleMessageSource messages;
    private AnalyticsSummaryEmailService service;

    @BeforeEach
    void setUp() {
        messages = new ResourceBundleMessageSource();
        messages.setBasename("mailMessages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        service = new AnalyticsSummaryEmailService(userRepository, companyService, analyticsSummaryService,
                mailServiceFactory, messages, transactionManager);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://cmms.example.com");
    }

    @Nested
    class PeriodDue {

        @Test
        void weekly_onMondayAtSeven_isThePreviousWeek() {
            ScheduledPeriod period = AnalyticsSummaryEmailService.periodDue(WEEKLY,
                    ZonedDateTime.of(2026, 10, 26, 7, 0, 0, 0, BUENOS_AIRES)).orElseThrow();

            assertEquals(Instant.parse("2026-10-19T03:00:00Z"), period.start().toInstant());
            assertEquals(Instant.parse("2026-10-26T02:59:59.999Z"), period.end().toInstant());
            assertEquals(Instant.parse("2026-10-12T03:00:00Z"), period.previousStart().toInstant());
            assertEquals(Instant.parse("2026-10-19T02:59:59.999Z"), period.previousEnd().toInstant());
        }

        @Test
        void weekly_otherHourOrDay_isNotDue() {
            assertTrue(AnalyticsSummaryEmailService.periodDue(WEEKLY,
                    ZonedDateTime.of(2026, 10, 26, 8, 0, 0, 0, BUENOS_AIRES)).isEmpty());
            assertTrue(AnalyticsSummaryEmailService.periodDue(WEEKLY,
                    ZonedDateTime.of(2026, 10, 27, 7, 0, 0, 0, BUENOS_AIRES)).isEmpty());
        }

        @Test
        void monthly_onTheFirst_isThePreviousMonthComparedWithTheOneBefore() {
            ScheduledPeriod period = AnalyticsSummaryEmailService.periodDue(MONTHLY,
                    ZonedDateTime.of(2026, 11, 1, 7, 30, 0, 0, BUENOS_AIRES)).orElseThrow();

            assertEquals(Instant.parse("2026-10-01T03:00:00Z"), period.start().toInstant());
            assertEquals(Instant.parse("2026-11-01T02:59:59.999Z"), period.end().toInstant());
            // September, not the 31 days before October
            assertEquals(Instant.parse("2026-09-01T03:00:00Z"), period.previousStart().toInstant());
            assertEquals(Instant.parse("2026-10-01T02:59:59.999Z"), period.previousEnd().toInstant());
            assertTrue(AnalyticsSummaryEmailService.periodDue(MONTHLY,
                    ZonedDateTime.of(2026, 10, 26, 7, 0, 0, 0, BUENOS_AIRES)).isEmpty());
        }

        @Test
        void monthly_acrossADaylightSavingChange_followsTheLocalMidnights() {
            // summer time ended in Madrid on 25/10/2026
            ScheduledPeriod period = AnalyticsSummaryEmailService.periodDue(MONTHLY,
                    ZonedDateTime.of(2026, 11, 1, 7, 0, 0, 0, MADRID)).orElseThrow();

            assertEquals(Instant.parse("2026-09-30T22:00:00Z"), period.start().toInstant());
            assertEquals(Instant.parse("2026-10-31T22:59:59.999Z"), period.end().toInstant());
        }

        @Test
        void never_isNeverDue() {
            assertTrue(AnalyticsSummaryEmailService.periodDue(NEVER,
                    ZonedDateTime.of(2026, 11, 2, 7, 0, 0, 0, BUENOS_AIRES)).isEmpty());
        }
    }

    @Nested
    class SendDue {

        @Test
        void sendsOneEmailPerSubscriberWithThePdfInTheirLanguage() {
            Company company = company(1L, "Hospital & Clínica", "UTC");
            User alice = user(10L, company, Language.FR, WEEKLY);
            User bruno = user(11L, company, Language.ES, WEEKLY);
            User withoutAnalytics = user(12L, company, Language.ES, WEEKLY);
            withoutAnalytics.getRole().setViewPermissions(new HashSet<>());
            User emailsOff = user(13L, company, Language.ES, WEEKLY);
            emailsOff.getUserSettings().setEmailNotified(false);
            User monthly = user(14L, company, Language.ES, MONTHLY);
            when(userRepository.findAnalyticsSummarySubscribers(AnalyticsSummaryEmailService.FREQUENCIES))
                    .thenReturn(List.of(alice, bruno, withoutAnalytics, emailsOff, monthly));
            when(userRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(alice, bruno));
            AnalyticsSummary summary = stubSummary(company);

            int sent = service.sendDue(MONDAY_7_UTC);

            assertEquals(2, sent);
            verify(analyticsSummaryService).build(1L, Date.from(Instant.parse("2026-10-19T00:00:00Z")),
                    Date.from(Instant.parse("2026-10-25T23:59:59.999Z")),
                    Date.from(Instant.parse("2026-10-12T00:00:00Z")),
                    Date.from(Instant.parse("2026-10-18T23:59:59.999Z")), AnalyticsFilter.NONE);
            verify(analyticsSummaryService).renderPdf(company, summary, List.of(), Locale.FRANCE);
            verify(analyticsSummaryService).renderPdf(company, summary, List.of(), new Locale("es", "ES"));
            verify(mailService, times(2)).sendMessageUsingThymeleafTemplate(recipients.capture(), subjects.capture(),
                    variables.capture(), eq(AnalyticsSummaryEmailService.TEMPLATE), locales.capture(),
                    attachments.capture());

            assertArrayEquals(new String[]{"user10@example.com"}, recipients.getAllValues().get(0));
            // no summary_email_* messages in French: English
            assertEquals("Weekly maintenance summary: Hospital & Clínica", subjects.getAllValues().get(0));
            assertArrayEquals(new String[]{"user11@example.com"}, recipients.getAllValues().get(1));
            assertEquals("Resumen semanal de mantenimiento: Hospital & Clínica", subjects.getAllValues().get(1));
            assertEquals(new Locale("es", "ES"), locales.getAllValues().get(1));

            String body = normalize((String) variables.getAllValues().get(1).get("messageBody"));
            assertTrue(body.startsWith("Adjuntamos el resumen de mantenimiento de Hospital &amp; Clínica de la " +
                    "semana 19/10/26 – 25/10/26, comparado con la semana anterior."), body);
            assertTrue(body.contains("<br>• Completadas: <b>52</b> (−5 %)<br>• Completadas a tiempo: <b>85 %</b> " +
                    "(+12 p. p.)<br>• Cumplimiento de PM: <b>85 %</b><br>• Pendientes: <b>38</b> (+23 %)"), body);
            assertEquals("https://cmms.example.com/app/analytics/work-orders/kpis",
                    variables.getAllValues().get(1).get("analyticsLink"));

            EmailAttachmentDTO attachment = attachments.getAllValues().get(1).get(0);
            assertEquals("Resumen de mantenimiento 2026-10-19.pdf", attachment.getAttachmentName());
            assertEquals("application/pdf", attachment.getAttachmentType());
            assertArrayEquals(PDF, attachment.getAttachmentData());
        }

        @Test
        void sevenInTheMorningOfTheCompany_notOfTheServer() {
            Company company = company(1L, "Hospital", BUENOS_AIRES.getId());
            User bruno = user(11L, company, Language.ES, WEEKLY);
            when(userRepository.findAnalyticsSummarySubscribers(any())).thenReturn(List.of(bruno));

            // 04:00 in Buenos Aires
            assertEquals(0, service.sendDue(MONDAY_7_UTC));
            verifyNoInteractions(analyticsSummaryService, mailServiceFactory);

            when(userRepository.findAllById(List.of(11L))).thenReturn(List.of(bruno));
            stubSummary(company);
            assertEquals(1, service.sendDue(Instant.parse("2026-10-26T10:00:00Z")));
        }

        @Test
        void oneCompanyFailing_doesNotStopTheOthers() {
            Company failing = company(1L, "Failing", "UTC");
            Company working = company(2L, "Working", "UTC");
            User first = user(10L, failing, Language.ES, WEEKLY);
            User second = user(11L, working, Language.ES, WEEKLY);
            when(userRepository.findAnalyticsSummarySubscribers(any())).thenReturn(List.of(first, second));
            when(userRepository.findAllById(List.of(11L))).thenReturn(List.of(second));
            when(companyService.findById(1L)).thenReturn(Optional.of(failing));
            when(analyticsSummaryService.build(eq(1L), any(), any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("boom"));
            stubSummary(working);

            assertEquals(1, service.sendDue(MONDAY_7_UTC));

            verify(mailService).sendMessageUsingThymeleafTemplate(eq(new String[]{"user11@example.com"}), any(),
                    any(), any(), any(), any());
        }

        @Test
        void demoCompanies_areSkipped() {
            Company demo = company(1L, "Demo", "UTC");
            demo.setDemo(true);
            when(userRepository.findAnalyticsSummarySubscribers(any()))
                    .thenReturn(List.of(user(10L, demo, Language.ES, WEEKLY)));

            assertEquals(0, service.sendDue(MONDAY_7_UTC));
            verifyNoInteractions(analyticsSummaryService);
        }

        private AnalyticsSummary stubSummary(Company company) {
            AnalyticsSummary summary = summary();
            when(companyService.findById(company.getId())).thenReturn(Optional.of(company));
            when(analyticsSummaryService.build(eq(company.getId()), any(), any(), any(), any(),
                    eq(AnalyticsFilter.NONE))).thenReturn(summary);
            when(analyticsSummaryService.renderPdf(eq(company), eq(summary), eq(List.of()), any(Locale.class)))
                    .thenReturn(PDF);
            when(analyticsSummaryService.formatter(eq(company), any(Locale.class))).thenAnswer(invocation ->
                    new AnalyticsSummaryFormatter(messages, invocation.getArgument(1), "ARS"));
            when(analyticsSummaryService.formatPeriod(eq(company), any(), any())).thenReturn("19/10/26 – 25/10/26");
            when(mailServiceFactory.getMailService()).thenReturn(mailService);
            return summary;
        }
    }

    @Nested
    class Settings {

        @Test
        void missingFrequency_isNever() {
            UserSettings settings = new UserSettings();
            settings.setAnalyticsSummaryFrequency(null);

            assertEquals(NEVER, settings.getAnalyticsSummaryFrequency());
        }

        @Test
        void turningOffEmailNotifications_stopsTheSummary() {
            UserSettings settings = new UserSettings();
            settings.setAnalyticsSummaryFrequency(MONTHLY);
            assertTrue(settings.shouldEmailAnalyticsSummary(MONTHLY));
            assertFalse(settings.shouldEmailAnalyticsSummary(WEEKLY));

            settings.setEmailNotified(false);
            assertFalse(settings.shouldEmailAnalyticsSummary(MONTHLY));
        }
    }

    private static Company company(Long id, String name, String timeZone) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .features(new HashSet<>(Set.of(PlanFeatures.ANALYTICS)))
                .build();
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        company.setSubscription(Subscription.builder().subscriptionPlan(plan).build());
        company.getCompanySettings().getGeneralPreferences().setTimeZone(timeZone);
        return company;
    }

    private static User user(Long id, Company company, Language language, AnalyticsSummaryFrequency frequency) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setEnabled(true);
        user.setCompany(company);
        user.setLanguage(language);
        user.setRole(Role.builder()
                .id(id)
                .name("Admin")
                .viewPermissions(new HashSet<>(Set.of(PermissionEntity.ANALYTICS)))
                .build());
        UserSettings settings = new UserSettings();
        settings.setAnalyticsSummaryFrequency(frequency);
        user.setUserSettings(settings);
        return user;
    }

    private static AnalyticsSummary summary() {
        AnalyticsSummary.Period current = new AnalyticsSummary.Period(new Date(0), new Date(1),
                WOStats.builder().total(75).complete(52).compliant(44).avgCycleTime(2.36).mtta(1.84).build(),
                MaintenanceKpis.builder().pmCompliance(85.0).plannedRatio(40.0).backlog(38).backlogHours(112.5)
                        .overdue(9).byPriority(List.of()).build(),
                WOCostsAndTime.builder().total(1000).build(),
                RequestStats.builder().approved(6).build());
        AnalyticsSummary.Period previous = new AnalyticsSummary.Period(new Date(0), new Date(1),
                WOStats.builder().total(70).complete(55).compliant(40).avgCycleTime(3.1).mtta(1.84).build(),
                MaintenanceKpis.builder().pmCompliance(null).plannedRatio(34.3).backlog(31).backlogHours(90)
                        .overdue(12).build(),
                WOCostsAndTime.builder().total(1200).build(),
                RequestStats.builder().approved(5).build());
        return new AnalyticsSummary(current, previous, List.of());
    }

    // the JDK separates numbers from % with non-breaking spaces in some locales
    private static String normalize(String text) {
        return text.replace(' ', ' ').replace(' ', ' ');
    }
}
