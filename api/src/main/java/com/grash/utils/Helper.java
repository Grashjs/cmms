package com.grash.utils;


import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.Language;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Helper {

    public String generateString() {
        return UUID.randomUUID().toString();
    }

    public HttpHeaders getPagingHeaders(Page<?> page, int size, String name) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Range", name + (page.getNumber() - 1) * size + "-" + page.getNumber() * size +
                "/" + page.getTotalElements());
        responseHeaders.set("Access-Control-Expose-Headers", "Content-Range");
        return responseHeaders;
    }

    /**
     * Get a diff between two dates
     *
     * @param date1    the oldest date
     * @param date2    the newest date
     * @param timeUnit the unit in which you want the diff
     * @return the diff value, in the provided unit
     */
    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    public static Date incrementDays(Date date, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, days);
        return c.getTime();
    }

    public static Date getNextOccurence(Date date, int days) {
        if (days == 0)
            throw new CustomException("getNextOccurence should not have 0 as parameter",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        Date result = date;
        if (result.after(new Date())) {
            result = incrementDays(result, days);
        } else
            while (result.before(new Date())) {
                result = incrementDays(result, days);
            }
        return result;
    }

    public static Date localDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate dateToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Date addSeconds(Date date, int seconds) {
        return new Date(date.getTime() + seconds * 1000);
    }

    public static Locale getLocale(OwnUser user) {
        return getLocale(user.getCompany());
    }

    public static Locale getLocale(Company company) {
        Language language = company.getCompanySettings().getGeneralPreferences().getLanguage();
        switch (language) {
            case FR:
                return Locale.FRANCE;
            case TR:
                return new Locale("tr", "TR");
            case ES:
                return new Locale("es", "ES");
            case PT_BR:
                return new Locale("pt", "BR");
            case PL:
                return new Locale("pl", "PL");
            case DE:
                return new Locale("de", "DE");
            case AR:
                return new Locale("ar", "AR");
            case IT:
                return new Locale("it", "IT");
            case SV:
                return new Locale("sv", "SE");
            case RU:
                return new Locale("ru", "RU");
            default:
                return Locale.getDefault();
        }
    }

    public static Date getDateFromJsString(String string) {
        DateFormat jsfmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        try {
            return jsfmt.parse(string);
        } catch (Exception exception) {
            return null;
        }
    }

    public static Date getDateFromExcelDate(Double excelDate) {
        if (excelDate == null) {
            return null;
        }
        try {
            //https://stackoverflow.com/questions/66985321/date-is-appearing-in-number-format-while-upload-import-excel-sheet-in-angular
            return new Date(Math.round((excelDate - 25569) * 24 * 60 * 60 * 1000));
        } catch (Exception exception) {
            return null;
        }
    }

    public static boolean isSameDay(Date date1, Date date2) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(date1).equals(fmt.format(date2));
    }

    public static String enumerate(Collection<String> strings) {
        StringBuilder stringBuilder = new StringBuilder();
        int size = strings.size();
        int index = 0;
        for (String string : strings) {
            stringBuilder.append(string);
            if (index < size - 1) {
                stringBuilder.append(",");
            }
        }
        return stringBuilder.toString();
    }

    public static boolean getBooleanFromString(String string) {
        List<String> trues = Arrays.asList("true", "Yes", "Oui", "Evet");
        return trues.stream().anyMatch(value -> value.equalsIgnoreCase(string));
    }

    public static String getStringFromBoolean(boolean bool, MessageSource messageSource, Locale locale) {
        return messageSource.getMessage(bool ? "Yes" : "No", null, locale);
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static <T> ResponseEntity<T> withCache(T entity) {
        CacheControl cacheControl = CacheControl.maxAge(30, TimeUnit.MINUTES).cachePublic();
        return ResponseEntity.ok()
                .cacheControl(cacheControl).body(entity);
    }

    public static Date minusDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -days);

        // Get the resulting date after subtracting days
        return calendar.getTime();
    }

    public static List<Role> getDefaultRoles() {
        List<PermissionEntity> allEntities = Arrays.asList(PermissionEntity.values());
        return Arrays.asList(
                Role.builder()
                        .roleType(RoleType.ROLE_CLIENT)
                        .code(RoleCode.ADMIN)
                        .name("Administrator")
                        .paid(true)
                        .createPermissions(new HashSet<>(allEntities))
                        .editOtherPermissions(new HashSet<>(allEntities))
                        .deleteOtherPermissions(new HashSet<>(allEntities))
                        .viewOtherPermissions(new HashSet<>(allEntities))
                        .viewPermissions(new HashSet<>(allEntities))
                        .build(),
                Role.builder()
                        .roleType(RoleType.ROLE_CLIENT)
                        .code(RoleCode.LIMITED_ADMIN)
                        .name("Limited Administrator")
                        .paid(true)
                        .createPermissions(new HashSet<>(allEntities.stream().filter(permissionEntity -> !Arrays.asList(PermissionEntity.PEOPLE_AND_TEAMS, PermissionEntity.REQUESTS).contains(permissionEntity)).collect(Collectors.toList())))
                        .editOtherPermissions(new HashSet<>(allEntities.stream().filter(permissionEntity -> !permissionEntity.equals(PermissionEntity.PEOPLE_AND_TEAMS)).collect(Collectors.toList())))
                        .viewOtherPermissions(new HashSet<>(allEntities))
                        .viewPermissions(new HashSet<>(allEntities.stream().filter(permissionEntity -> permissionEntity != PermissionEntity.SETTINGS).collect(Collectors.toList())))
                        .deleteOtherPermissions(new HashSet<>())
                        .build(),
                Role.builder()
                        .roleType(RoleType.ROLE_CLIENT)
                        .code(RoleCode.TECHNICIAN)
                        .name("Technician")
                        .paid(true)
                        .createPermissions(new HashSet<>(Arrays.asList(PermissionEntity.WORK_ORDERS,
                                PermissionEntity.ASSETS, PermissionEntity.LOCATIONS, PermissionEntity.FILES)))
                        .editOtherPermissions(new HashSet<>())
                        .deleteOtherPermissions(new HashSet<>())
                        .viewOtherPermissions(new HashSet<>(Arrays.asList(PermissionEntity.WORK_ORDERS,
                                PermissionEntity.PARTS_AND_MULTIPARTS, PermissionEntity.LOCATIONS,
                                PermissionEntity.ASSETS)))
                        .viewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.WORK_ORDERS,
                                PermissionEntity.LOCATIONS, PermissionEntity.ASSETS, PermissionEntity.CATEGORIES,
                                PermissionEntity.PREVENTIVE_MAINTENANCES, PermissionEntity.METERS)))
                        .build(),
                Role.builder()
                        .roleType(RoleType.ROLE_CLIENT)
                        .code(RoleCode.LIMITED_TECHNICIAN)
                        .name("Limited Technician")
                        .paid(true)
                        .createPermissions(new HashSet<>(Arrays.asList(PermissionEntity.FILES)))
                        .editOtherPermissions(new HashSet<>())
                        .deleteOtherPermissions(new HashSet<>())
                        .viewOtherPermissions(new HashSet<>(Arrays.asList(PermissionEntity.ASSETS,
                                PermissionEntity.PARTS_AND_MULTIPARTS, PermissionEntity.LOCATIONS)))
                        .viewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.WORK_ORDERS,
                                PermissionEntity.CATEGORIES, PermissionEntity.PARTS_AND_MULTIPARTS,
                                PermissionEntity.LOCATIONS, PermissionEntity.ASSETS,
                                PermissionEntity.PREVENTIVE_MAINTENANCES, PermissionEntity.METERS)))
                        .build(),
                Role.builder()
                        .roleType(RoleType.ROLE_CLIENT)
                        .code(RoleCode.VIEW_ONLY)
                        .name("View Only")
                        .paid(false)
                        .createPermissions(new HashSet<>())
                        .editOtherPermissions(new HashSet<>())
                        .deleteOtherPermissions(new HashSet<>())
                        .viewOtherPermissions(new HashSet<>(allEntities))
                        .viewPermissions(new HashSet<>(allEntities.stream().filter(permissionEntity -> permissionEntity != PermissionEntity.SETTINGS).collect(Collectors.toList())))
                        .build(),
                Role.builder()
                        .roleType(RoleType.ROLE_CLIENT)
                        .code(RoleCode.REQUESTER)
                        .name("Requester")
                        .paid(false)
                        .createPermissions(new HashSet<>(Arrays.asList(PermissionEntity.REQUESTS,
                                PermissionEntity.FILES)))
                        .editOtherPermissions(new HashSet<>())
                        .deleteOtherPermissions(new HashSet<>())
                        .viewOtherPermissions(new HashSet<>())
                        .viewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.REQUESTS,
                                PermissionEntity.CATEGORIES)))
                        .build()
        );
    }

    public static boolean isLocalhost(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();

            // Check for common localhost values
            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host)) {
                return true;
            }

            // Resolve hostname to IP address and check if it's a local address
            InetAddress address = InetAddress.getByName(host);
            return address.isLoopbackAddress();

        } catch (MalformedURLException | UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }

}
