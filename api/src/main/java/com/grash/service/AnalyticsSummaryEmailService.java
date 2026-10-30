package com.grash.service;

import com.grash.dto.EmailAttachmentDTO;
import com.grash.dto.analytics.AnalyticsFilter;
import com.grash.dto.analytics.summary.AnalyticsSummary;
import com.grash.factory.MailServiceFactory;
import com.grash.model.Company;
import com.grash.model.User;
import com.grash.model.enums.AnalyticsSummaryFrequency;
import com.grash.repository.UserRepository;
import com.grash.utils.Helper;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.HtmlUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Emails the maintenance summary (issue #318, point 5) to the users who subscribed to it in their notification
 * settings, with the PDF attached: on Mondays for the previous week and on the 1st for the previous month, at
 * {@value #SEND_HOUR}:00 in the time zone of the company.
 * <p>
 * Called every hour by {@link com.grash.job.AnalyticsSummaryJob}. The summary covers the whole company, without
 * filters, and is only sent to users who can see the analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsSummaryEmailService {

    static final int SEND_HOUR = 7;
    static final String TEMPLATE = "analytics-summary-email.html";
    static final List<AnalyticsSummaryFrequency> FREQUENCIES = List.of(AnalyticsSummaryFrequency.WEEKLY,
            AnalyticsSummaryFrequency.MONTHLY);
    // figures quoted in the body of the email, the rest is in the PDF
    static final List<String> HIGHLIGHTS = List.of("summary_wo_completed", "summary_wo_compliance",
            "summary_pm_compliance", "summary_backlog", "summary_overdue", "summary_total_cost");
    static final String ANALYTICS_PATH = "/app/analytics/work-orders/kpis";

    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final AnalyticsSummaryService analyticsSummaryService;
    private final MailServiceFactory mailServiceFactory;
    private final ResourceBundleMessageSource emailMessageSource;
    private final PlatformTransactionManager transactionManager;

    @Value("${frontend.url}")
    private String frontendUrl;

    /**
     * The period a summary covers and the one it is compared with. Bounds are inclusive, as on the web.
     */
    public record ScheduledPeriod(Date start, Date end, Date previousStart, Date previousEnd) {
    }

    /**
     * One summary to send: a company, a frequency and the users who subscribed to it.
     */
    record Delivery(Long companyId, AnalyticsSummaryFrequency frequency, ScheduledPeriod period,
                    List<Long> userIds) {
    }

    /**
     * Sends the summaries due at {@code now}.
     *
     * @return the number of emails sent
     */
    public int sendDue(Instant now) {
        List<Delivery> deliveries = readOnly().execute(status -> deliveriesDue(now));
        int sent = 0;
        for (Delivery delivery : deliveries) {
            try {
                // each company in its own transaction: one failing does not keep the others from theirs
                Integer count = readOnly().execute(status -> send(delivery));
                sent += count == null ? 0 : count;
            } catch (RuntimeException e) {
                log.error("Could not send the {} maintenance summary of company {}", delivery.frequency(),
                        delivery.companyId(), e);
                Sentry.captureException(e);
            }
        }
        return sent;
    }

    List<Delivery> deliveriesDue(Instant now) {
        Map<Long, List<User>> subscribersByCompany = userRepository.findAnalyticsSummarySubscribers(FREQUENCIES)
                .stream()
                .filter(AnalyticsSummaryEmailService::canReceive)
                .collect(Collectors.groupingBy(user -> user.getCompany().getId(), LinkedHashMap::new,
                        Collectors.toList()));
        List<Delivery> deliveries = new ArrayList<>();
        subscribersByCompany.forEach((companyId, subscribers) -> {
            Company company = subscribers.get(0).getCompany();
            if (company.isDemo()) return;
            ZonedDateTime localNow = now.atZone(zone(company));
            for (AnalyticsSummaryFrequency frequency : FREQUENCIES) {
                List<Long> userIds = subscribers.stream()
                        .filter(user -> user.getUserSettings().shouldEmailAnalyticsSummary(frequency))
                        .map(User::getId)
                        .toList();
                if (userIds.isEmpty()) continue;
                periodDue(frequency, localNow).ifPresent(period ->
                        deliveries.add(new Delivery(companyId, frequency, period, userIds)));
            }
        });
        return deliveries;
    }

    int send(Delivery delivery) {
        Company company = companyService.findById(delivery.companyId()).orElse(null);
        if (company == null) return 0;
        ScheduledPeriod period = delivery.period();
        AnalyticsSummary summary = analyticsSummaryService.build(company.getId(), period.start(), period.end(),
                period.previousStart(), period.previousEnd(), AnalyticsFilter.NONE);
        // one PDF per language
        Map<Locale, List<User>> recipientsByLocale = userRepository.findAllById(delivery.userIds()).stream()
                .filter(AnalyticsSummaryEmailService::canReceive)
                .collect(Collectors.groupingBy((User user) -> Helper.getLocale(user), LinkedHashMap::new,
                        Collectors.toList()));
        MailService mailService = mailServiceFactory.getMailService();
        int sent = 0;
        for (Map.Entry<Locale, List<User>> entry : recipientsByLocale.entrySet()) {
            Locale locale = entry.getKey();
            byte[] pdf = analyticsSummaryService.renderPdf(company, summary, List.of(), locale);
            String frequency = delivery.frequency().name().toLowerCase(Locale.ROOT);
            String subject = message("summary_email_subject_" + frequency, locale, company.getName());
            Map<String, Object> variables = new HashMap<>();
            variables.put("messageBody", body(company, summary, frequency, locale));
            variables.put("analyticsLink", frontendUrl + ANALYTICS_PATH);
            List<EmailAttachmentDTO> attachments = List.of(EmailAttachmentDTO.builder()
                    .attachmentName(attachmentName(company, period, locale))
                    .attachmentData(pdf)
                    .attachmentType("application/pdf")
                    .build());
            // one email per user, so that recipients do not see each other's addresses
            for (User user : entry.getValue()) {
                mailService.sendMessageUsingThymeleafTemplate(new String[]{user.getEmail()}, subject, variables,
                        TEMPLATE, locale, attachments);
                sent++;
            }
        }
        return sent;
    }

    /**
     * The period to send at {@code localNow} (in the time zone of the company), if a summary of that frequency is
     * due: on Mondays the previous week (compared with the week before), on the 1st the previous month (compared
     * with the month before), in the hour starting at {@value #SEND_HOUR}:00.
     */
    static Optional<ScheduledPeriod> periodDue(AnalyticsSummaryFrequency frequency, ZonedDateTime localNow) {
        if (localNow.getHour() != SEND_HOUR) return Optional.empty();
        LocalDate today = localNow.toLocalDate();
        ZoneId zone = localNow.getZone();
        return switch (frequency) {
            case WEEKLY -> today.getDayOfWeek() != DayOfWeek.MONDAY ? Optional.empty() :
                    Optional.of(period(today.minusWeeks(1), today, today.minusWeeks(2), zone));
            case MONTHLY -> today.getDayOfMonth() != 1 ? Optional.empty() :
                    Optional.of(period(today.minusMonths(1), today, today.minusMonths(2), zone));
            case NEVER -> Optional.empty();
        };
    }

    // [start, end) and [previousStart, start) as inclusive bounds
    private static ScheduledPeriod period(LocalDate start, LocalDate end, LocalDate previousStart, ZoneId zone) {
        Instant startInstant = start.atStartOfDay(zone).toInstant();
        return new ScheduledPeriod(Date.from(startInstant),
                Date.from(end.atStartOfDay(zone).toInstant().minusMillis(1)),
                Date.from(previousStart.atStartOfDay(zone).toInstant()),
                Date.from(startInstant.minusMillis(1)));
    }

    static boolean canReceive(User user) {
        return user.isEnabled() && user.isEnabledInSubscription()
                && user.getEmail() != null && !user.getEmail().isBlank()
                && user.getRole() != null && user.getCompany().getSubscription() != null
                && user.getCompany().getSubscription().getSubscriptionPlan() != null
                && user.canSeeAnalytics();
    }

    private static ZoneId zone(Company company) {
        String timeZone = company.getCompanySettings().getGeneralPreferences().getTimeZone();
        try {
            return timeZone == null ? ZoneId.systemDefault() : ZoneId.of(timeZone);
        } catch (DateTimeException e) {
            return ZoneId.systemDefault();
        }
    }

    /**
     * HTML of the message: the period, a few figures with their change and why the user receives it. Everything
     * is escaped, the layout prints it as is.
     */
    private String body(Company company, AnalyticsSummary summary, String frequency, Locale locale) {
        String period = analyticsSummaryService.formatPeriod(company, summary.current().start(),
                summary.current().end());
        StringBuilder body = new StringBuilder(escape(message("summary_email_intro_" + frequency, locale,
                company.getName(), period)));
        body.append("<br><br><b>").append(escape(message("summary_email_highlights", locale))).append("</b>");
        analyticsSummaryService.formatter(company, locale).sections(summary).stream()
                .flatMap(section -> section.getRows().stream())
                .filter(row -> HIGHLIGHTS.contains(row.getKey()))
                .sorted(Comparator.comparingInt(row -> HIGHLIGHTS.indexOf(row.getKey())))
                .forEach(row -> {
                    body.append("<br>• ").append(escape(row.getLabel())).append(": <b>")
                            .append(escape(row.getCurrent())).append("</b>");
                    if (!row.getChange().equals("—")) body.append(" (").append(escape(row.getChange())).append(")");
                });
        body.append("<br><br><small>").append(escape(message("summary_email_footer", locale))).append("</small>");
        return body.toString();
    }

    private String attachmentName(Company company, ScheduledPeriod period, Locale locale) {
        LocalDate start = period.start().toInstant().atZone(zone(company)).toLocalDate();
        return message("summary_title", locale) + " " + DateTimeFormatter.ISO_LOCAL_DATE.format(start) + ".pdf";
    }

    private String message(String key, Locale locale, Object... args) {
        return emailMessageSource.getMessage(key, args.length == 0 ? null : args, key, locale);
    }

    private static String escape(String text) {
        return HtmlUtils.htmlEscape(text, "UTF-8");
    }

    private TransactionTemplate readOnly() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(true);
        return template;
    }
}
