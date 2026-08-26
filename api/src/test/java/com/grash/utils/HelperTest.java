package com.grash.utils;

import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.Language;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.Priority;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.dto.imports.WorkOrderImportDTO;
import com.grash.service.LocationService;
import com.grash.service.TeamService;
import com.grash.service.UserService;
import com.grash.service.AssetService;
import com.grash.service.WorkOrderCategoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HelperTest {

    private final Helper helper = new Helper();

    @Nested
    class GenerateString {
        @Test
        void returnsNonNullUuidString() {
            String result = helper.generateString();
            assertNotNull(result);
            assertDoesNotThrow(() -> UUID.fromString(result));
        }
    }

    @Nested
    class GetDateDiff {
        @Test
        void positiveDiff() {
            Date d1 = new Date(1000);
            Date d2 = new Date(3000);
            assertEquals(2, Helper.getDateDiff(d1, d2, TimeUnit.SECONDS));
        }

        @Test
        void zeroDiff() {
            Date now = new Date();
            assertEquals(0, Helper.getDateDiff(now, now, TimeUnit.MILLISECONDS));
        }

        @Test
        void negativeDiffWhenDatesReversed() {
            Date d1 = new Date(3000);
            Date d2 = new Date(1000);
            assertEquals(-2000, Helper.getDateDiff(d1, d2, TimeUnit.MILLISECONDS));
        }
    }

    @Nested
    class IsValidEmailAddress {
        @Test
        void validEmail_returnsTrue() {
            assertTrue(Helper.isValidEmailAddress("user@example.com"));
        }

        @Test
        void invalidEmail_returnsFalse() {
            assertFalse(Helper.isValidEmailAddress("not-an-email"));
        }

        @Test
        void nullEmail_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () -> Helper.isValidEmailAddress(null));
        }

        @Test
        void emptyEmail_returnsFalse() {
            assertFalse(Helper.isValidEmailAddress(""));
        }
    }

    @Nested
    class IncrementDays {
        @Test
        void positiveDays() {
            Date base = new Date(0);
            Calendar cal = Calendar.getInstance();
            cal.setTime(base);
            cal.add(Calendar.DATE, 5);
            Date expected = cal.getTime();

            Date result = Helper.incrementDays(base, 5);
            assertEquals(expected, result);
        }

        @Test
        void negativeDays() {
            Date base = new Date(0);
            Calendar cal = Calendar.getInstance();
            cal.setTime(base);
            cal.add(Calendar.DATE, -3);
            Date expected = cal.getTime();

            Date result = Helper.incrementDays(base, -3);
            assertEquals(expected, result);
        }

        @Test
        void zeroDays() {
            Date base = new Date();
            assertEquals(base, Helper.incrementDays(base, 0));
        }
    }

    @Nested
    class LocalDateToDate {
        @Test
        void convertsCorrectly() {
            LocalDate local = LocalDate.of(2025, 6, 15);
            Date result = Helper.localDateToDate(local);
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2025, cal.get(Calendar.YEAR));
            assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
            assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
        }
    }

    @Nested
    class LocalDateTimeToDate {
        @Test
        void convertsCorrectly() {
            LocalDateTime local = LocalDateTime.of(2025, 6, 15, 10, 30, 0);
            Date result = Helper.localDateTimeToDate(local);
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2025, cal.get(Calendar.YEAR));
            assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
            assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(30, cal.get(Calendar.MINUTE));
        }
    }

    @Nested
    class DateToLocalDate {
        @Test
        void convertsCorrectly() {
            Calendar cal = Calendar.getInstance();
            cal.set(2025, Calendar.DECEMBER, 25);
            Date date = cal.getTime();
            LocalDate result = Helper.dateToLocalDate(date);
            assertEquals(LocalDate.of(2025, 12, 25), result);
        }
    }

    @Nested
    class AddSeconds {
        @Test
        void addsSeconds() {
            Date base = new Date(0);
            Date result = Helper.addSeconds(base, 10);
            assertEquals(10_000, result.getTime());
        }

        @Test
        void negativeSeconds() {
            Date base = new Date(20_000);
            Date result = Helper.addSeconds(base, -5);
            assertEquals(15_000, result.getTime());
        }
    }

    @Nested
    class GetLocale {
        private Company companyWithLanguage(Language language) {
            GeneralPreferences prefs = new GeneralPreferences();
            prefs.setLanguage(language);
            CompanySettings settings = new CompanySettings();
            settings.setGeneralPreferences(prefs);
            Company company = new Company();
            company.setCompanySettings(settings);
            return company;
        }

        @Test
        void french() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.FR));
            assertEquals(Locale.FRANCE, locale);
        }

        @Test
        void turkish() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.TR));
            assertEquals(new Locale("tr", "TR"), locale);
        }

        @Test
        void spanish() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.ES));
            assertEquals(new Locale("es", "ES"), locale);
        }

        @Test
        void portugueseBrazil() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.PT_BR));
            assertEquals(new Locale("pt", "BR"), locale);
        }

        @Test
        void portuguese() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.PT));
            assertEquals(new Locale("pt", "BR"), locale);
        }

        @Test
        void polish() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.PL));
            assertEquals(new Locale("pl", "PL"), locale);
        }

        @Test
        void german() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.DE));
            assertEquals(new Locale("de", "DE"), locale);
        }

        @Test
        void arabic() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.AR));
            assertEquals(new Locale("ar", "AR"), locale);
        }

        @Test
        void italian() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.IT));
            assertEquals(new Locale("it", "IT"), locale);
        }

        @Test
        void swedish() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.SV));
            assertEquals(new Locale("sv", "SE"), locale);
        }

        @Test
        void russian() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.RU));
            assertEquals(new Locale("ru", "RU"), locale);
        }

        @Test
        void hungarian() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.HU));
            assertEquals(new Locale("hu", "HU"), locale);
        }

        @Test
        void dutch() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.NL));
            assertEquals(new Locale("nl", "NL"), locale);
        }

        @Test
        void chineseSimplified() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.ZH_CN));
            assertEquals(new Locale("zh", "CN"), locale);
        }

        @Test
        void chinese() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.ZH));
            assertEquals(new Locale("zh", "CN"), locale);
        }

        @Test
        void bosnian() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.BA));
            assertEquals(new Locale("ba", "BA"), locale);
        }

        @Test
        void japanese() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.JA));
            assertEquals(new Locale("ja", "JP"), locale);
        }

        @Test
        void english() {
            Locale locale = Helper.getLocale(companyWithLanguage(Language.EN));
            assertEquals(Locale.getDefault(), locale);
        }

        @Test
        void fromUserDelegatesToCompany() {
            Company company = companyWithLanguage(Language.FR);
            User user = new User();
            user.setCompany(company);
            assertEquals(Locale.FRANCE, Helper.getLocale(user));
        }
    }

    @Nested
    class GetDateFromJsString {
        @Test
        void validDateString_returnsDate() {
            Date result = Helper.getDateFromJsString("2025-06-15T10:30:00.000Z");
            assertNotNull(result);
            Calendar cal = Calendar.getInstance();
            cal.setTime(result);
            assertEquals(2025, cal.get(Calendar.YEAR));
            assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
            assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
        }

        @Test
        void invalidDateString_returnsNull() {
            assertNull(Helper.getDateFromJsString("not-a-date"));
        }

        @Test
        void nullString_returnsNull() {
            assertNull(Helper.getDateFromJsString(null));
        }
    }

    @Nested
    class GetDateFromExcelDate {
        @Test
        void validExcelDate_returnsDate() {
            Date result = Helper.getDateFromExcelDate(45000.0);
            assertNotNull(result);
        }

        @Test
        void nullValue_returnsNull() {
            assertNull(Helper.getDateFromExcelDate(null));
        }

        @Test
        void zeroExcelDate_returnsEpochAdjusted() {
            Date result = Helper.getDateFromExcelDate(0.0);
            assertNotNull(result);
        }
    }

    @Nested
    class IsSameDay {
        @Test
        void sameDay_returnsTrue() {
            Calendar cal = Calendar.getInstance();
            Date d1 = cal.getTime();
            Date d2 = cal.getTime();
            assertTrue(Helper.isSameDay(d1, d2));
        }

        @Test
        void differentDay_returnsFalse() {
            Calendar cal1 = Calendar.getInstance();
            cal1.set(2025, Calendar.JANUARY, 1);
            Calendar cal2 = Calendar.getInstance();
            cal2.set(2025, Calendar.JANUARY, 2);
            assertFalse(Helper.isSameDay(cal1.getTime(), cal2.getTime()));
        }
    }

    @Nested
    class Enumerate {
        @Test
        void multipleStrings_joinsWithCommas() {
            List<String> items = Arrays.asList("a", "b", "c");
            assertEquals("a,b,c", Helper.enumerate(items));
        }

        @Test
        void singleString_returnsIt() {
            assertEquals("only", Helper.enumerate(Collections.singletonList("only")));
        }

        @Test
        void emptyCollection_returnsEmptyString() {
            assertEquals("", Helper.enumerate(Collections.emptyList()));
        }
    }

    @Nested
    class GetBooleanFromString {
        @Test
        void trueString_returnsTrue() {
            assertTrue(Helper.getBooleanFromString("true"));
        }

        @Test
        void yesString_returnsTrue() {
            assertTrue(Helper.getBooleanFromString("Yes"));
        }

        @Test
        void ouiString_returnsTrue() {
            assertTrue(Helper.getBooleanFromString("Oui"));
        }

        @Test
        void evetString_returnsTrue() {
            assertTrue(Helper.getBooleanFromString("Evet"));
        }

        @Test
        void caseInsensitive_returnsTrue() {
            assertTrue(Helper.getBooleanFromString("TRUE"));
            assertTrue(Helper.getBooleanFromString("yes"));
            assertTrue(Helper.getBooleanFromString("OUI"));
            assertTrue(Helper.getBooleanFromString("EVET"));
        }

        @Test
        void falseString_returnsFalse() {
            assertFalse(Helper.getBooleanFromString("false"));
        }

        @Test
        void randomString_returnsFalse() {
            assertFalse(Helper.getBooleanFromString("maybe"));
        }
    }

    @Nested
    class GetStringFromBoolean {
        @Test
        void trueValue_returnsYesMessage() {
            MessageSource messageSource = mock(MessageSource.class);
            when(messageSource.getMessage(eq("Yes"), isNull(), any(Locale.class))).thenReturn("Yes");
            assertEquals("Yes", Helper.getStringFromBoolean(true, messageSource, Locale.US));
        }

        @Test
        void falseValue_returnsNoMessage() {
            MessageSource messageSource = mock(MessageSource.class);
            when(messageSource.getMessage(eq("No"), isNull(), any(Locale.class))).thenReturn("No");
            assertEquals("No", Helper.getStringFromBoolean(false, messageSource, Locale.US));
        }
    }

    @Nested
    class IsNumeric {
        @Test
        void integerString_returnsTrue() {
            assertTrue(Helper.isNumeric("123"));
        }

        @Test
        void decimalString_returnsTrue() {
            assertTrue(Helper.isNumeric("123.45"));
        }

        @Test
        void negativeNumber_returnsTrue() {
            assertTrue(Helper.isNumeric("-42"));
        }

        @Test
        void nonNumericString_returnsFalse() {
            assertFalse(Helper.isNumeric("abc"));
        }

        @Test
        void emptyString_returnsFalse() {
            assertFalse(Helper.isNumeric(""));
        }

        @Test
        void nullString_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () -> Helper.isNumeric(null));
        }
    }

    @Nested
    class GetDefaultRoles {
        @Test
        void returnsSixRoles() {
            List<Role> roles = Helper.getDefaultRoles();
            assertEquals(6, roles.size());
        }

        @Test
        void firstRoleIsAdmin() {
            List<Role> roles = Helper.getDefaultRoles();
            assertEquals(RoleCode.ADMIN, roles.get(0).getCode());
            assertEquals("Administrator", roles.get(0).getName());
            assertTrue(roles.get(0).isPaid());
        }

        @Test
        void adminHasAllPermissions() {
            Role admin = Helper.getDefaultRoles().get(0);
            assertEquals(new HashSet<>(Arrays.asList(PermissionEntity.values())), admin.getCreatePermissions());
            assertEquals(new HashSet<>(Arrays.asList(PermissionEntity.values())), admin.getViewPermissions());
            assertEquals(new HashSet<>(Arrays.asList(PermissionEntity.values())), admin.getViewOtherPermissions());
            assertEquals(new HashSet<>(Arrays.asList(PermissionEntity.values())), admin.getEditOtherPermissions());
            assertEquals(new HashSet<>(Arrays.asList(PermissionEntity.values())), admin.getDeleteOtherPermissions());
        }

        @Test
        void limitedAdminHasNoDeleteOtherPermissions() {
            Role limitedAdmin = Helper.getDefaultRoles().get(1);
            assertEquals(RoleCode.LIMITED_ADMIN, limitedAdmin.getCode());
            assertTrue(limitedAdmin.getDeleteOtherPermissions().isEmpty());
        }

        @Test
        void technicianHasSpecificPermissions() {
            Role tech = Helper.getDefaultRoles().get(2);
            assertEquals(RoleCode.TECHNICIAN, tech.getCode());
            assertTrue(tech.getEditOtherPermissions().isEmpty());
            assertTrue(tech.getDeleteOtherPermissions().isEmpty());
        }

        @Test
        void viewOnlyIsNotPaid() {
            Role viewOnly = Helper.getDefaultRoles().get(4);
            assertEquals(RoleCode.VIEW_ONLY, viewOnly.getCode());
            assertFalse(viewOnly.isPaid());
        }

        @Test
        void requesterHasOnlyRequestsAndFiles() {
            Role requester = Helper.getDefaultRoles().get(5);
            assertEquals(RoleCode.REQUESTER, requester.getCode());
            assertEquals(new HashSet<>(Arrays.asList(PermissionEntity.REQUESTS, PermissionEntity.FILES)),
                    requester.getCreatePermissions());
        }
    }

    @Nested
    class IsLocalhost {
        @Test
        void localhostHost_returnsTrue() {
            assertTrue(Helper.isLocalhost("http://localhost:8080"));
        }

        @Test
        void loopbackIp_returnsTrue() {
            assertTrue(Helper.isLocalhost("http://127.0.0.1:8080"));
        }

        @Test
        void externalUrl_returnsFalse() {
            assertFalse(Helper.isLocalhost("http://google.com"));
        }

        @Test
        void malformedUrl_returnsFalse() {
            assertFalse(Helper.isLocalhost("not a url"));
        }
    }

    @Nested
    class HashKey {
        @Test
        void returnsBase64EncodedSha256() throws NoSuchAlgorithmException {
            String hash = Helper.hashKey("hello");
            assertNotNull(hash);
            assertFalse(hash.isEmpty());
            assertEquals(44, hash.length());
        }

        @Test
        void sameInput_returnsSameHash() throws NoSuchAlgorithmException {
            assertEquals(Helper.hashKey("test"), Helper.hashKey("test"));
        }

        @Test
        void differentInput_returnsDifferentHash() throws NoSuchAlgorithmException {
            assertNotEquals(Helper.hashKey("abc"), Helper.hashKey("xyz"));
        }

        @Test
        void emptyString_returnsValidHash() throws NoSuchAlgorithmException {
            String hash = Helper.hashKey("");
            assertNotNull(hash);
        }
    }

    @Nested
    class SetCurrentUser {
        @Test
        void setsAuthenticationInSecurityContext() {
            User user = new User();
            user.setId(1L);
            Role role = new Role();
            role.setRoleType(RoleType.ROLE_CLIENT);
            user.setRole(role);

            Helper.setCurrentUser(user);

            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
            SecurityContextHolder.clearContext();
        }
    }

    @Nested
    class ResolveCompanyId {
        @Test
        void nullCompanyId_returnsUserCompanyId() {
            Company company = new Company();
            company.setId(1L);
            User user = new User();
            user.setCompany(company);
            assertEquals(1L, Helper.resolveCompanyId(user, null));
        }

        @Test
        void sameCompanyId_returnsIt() {
            Company company = new Company();
            company.setId(1L);
            User user = new User();
            user.setCompany(company);
            assertEquals(1L, Helper.resolveCompanyId(user, 1L));
        }

        @Test
        void differentCompanyIdWithoutAccess_throwsForbidden() {
            Company userCompany = new Company();
            userCompany.setId(1L);
            User user = new User();
            user.setCompany(userCompany);
            user.setSuperAccountRelations(new ArrayList<>());
            assertThrows(CustomException.class, () -> Helper.resolveCompanyId(user, 2L));
        }

        @Test
        void differentCompanyIdWithAccess_returnsIt() {
            Company userCompany = new Company();
            userCompany.setId(1L);
            Company childCompany = new Company();
            childCompany.setId(2L);
            User childUser = new User();
            childUser.setCompany(childCompany);
            SuperAccountRelation rel = SuperAccountRelation.builder()
                    .childUser(childUser)
                    .build();
            User user = new User();
            user.setCompany(userCompany);
            user.setSuperAccountRelations(Collections.singletonList(rel));

            assertEquals(2L, Helper.resolveCompanyId(user, 2L));
        }

        @Test
        void differentCompanyIdWithAccess_throwsCustomExceptionWithForbiddenStatus() {
            Company userCompany = new Company();
            userCompany.setId(1L);
            User user = new User();
            user.setCompany(userCompany);
            user.setSuperAccountRelations(new ArrayList<>());
            CustomException ex = assertThrows(CustomException.class,
                    () -> Helper.resolveCompanyId(user, 99L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class PopulateWorkOrderBaseFromImportDTO {
        @Mock
        private LocationService locationService;
        @Mock
        private TeamService teamService;
        @Mock
        private UserService userService;
        @Mock
        private AssetService assetService;
        @Mock
        private WorkOrderCategoryService workOrderCategoryService;

        private WorkOrderBase workOrderBase;
        private WorkOrderImportDTO dto;
        private Company company;

        @BeforeEach
        void setUp() {
            lenient().when(userService.findByEmailAndCompany(isNull(), anyLong())).thenReturn(Optional.empty());
        }

        @Test
        void populatesAllFields() {
            workOrderBase = mock(WorkOrderBase.class);
            dto = new WorkOrderImportDTO();
            dto.setTitle("Test WO");
            dto.setDescription("Test description");
            dto.setPriority("High");
            dto.setEstimatedDuration(2.5);
            dto.setAssignedToEmails(new ArrayList<>());

            company = new Company();
            company.setId(1L);

            Helper.populateWorkOrderBaseFromImportDTO(workOrderBase, dto, company,
                    locationService, teamService, userService, assetService, workOrderCategoryService);

            verify(workOrderBase).setTitle("Test WO");
            verify(workOrderBase).setDescription("Test description");
            verify(workOrderBase).setPriority(Priority.HIGH);
            verify(workOrderBase).setEstimatedDuration(2.5);
        }

        @Test
        void withCategory_createsOrGetsCategory() {
            workOrderBase = mock(WorkOrderBase.class);
            dto = new WorkOrderImportDTO();
            dto.setTitle("WO");
            dto.setPriority("None");
            dto.setEstimatedDuration(0);
            dto.setCategory("Electrical");
            dto.setAssignedToEmails(new ArrayList<>());

            CompanySettings settings = new CompanySettings();
            company = new Company();
            company.setId(1L);
            company.setCompanySettings(settings);

            WorkOrderCategory category = new WorkOrderCategory();
            when(workOrderCategoryService.getOrCreate("Electrical", settings)).thenReturn(category);

            Helper.populateWorkOrderBaseFromImportDTO(workOrderBase, dto, company,
                    locationService, teamService, userService, assetService, workOrderCategoryService);

            verify(workOrderBase).setCategory(category);
        }

        @Test
        void withLocation_setsIt() {
            workOrderBase = mock(WorkOrderBase.class);
            dto = new WorkOrderImportDTO();
            dto.setTitle("WO");
            dto.setPriority("None");
            dto.setEstimatedDuration(0);
            dto.setLocationName("Warehouse A");
            dto.setAssignedToEmails(new ArrayList<>());

            company = new Company();
            company.setId(1L);

            Location location = new Location();
            when(locationService.findByNameIgnoreCaseAndCompany("Warehouse A", 1L))
                    .thenReturn(Collections.singletonList(location));

            Helper.populateWorkOrderBaseFromImportDTO(workOrderBase, dto, company,
                    locationService, teamService, userService, assetService, workOrderCategoryService);

            verify(workOrderBase).setLocation(location);
        }

        @Test
        void withPrimaryUser_setsIt() {
            workOrderBase = mock(WorkOrderBase.class);
            dto = new WorkOrderImportDTO();
            dto.setTitle("WO");
            dto.setPriority("None");
            dto.setEstimatedDuration(0);
            dto.setPrimaryUserEmail("user@example.com");
            dto.setAssignedToEmails(new ArrayList<>());

            company = new Company();
            company.setId(1L);

            User user = new User();
            when(userService.findByEmailAndCompany("user@example.com", 1L))
                    .thenReturn(Optional.of(user));

            Helper.populateWorkOrderBaseFromImportDTO(workOrderBase, dto, company,
                    locationService, teamService, userService, assetService, workOrderCategoryService);

            verify(workOrderBase).setPrimaryUser(user);
        }

        @Test
        void withAssignedToEmails_setsList() {
            workOrderBase = mock(WorkOrderBase.class);
            dto = new WorkOrderImportDTO();
            dto.setTitle("WO");
            dto.setPriority("None");
            dto.setEstimatedDuration(0);
            dto.setAssignedToEmails(Arrays.asList("a@example.com", "b@example.com"));

            company = new Company();
            company.setId(1L);

            User userA = new User();
            userA.setId(1L);
            User userB = new User();
            userB.setId(2L);
            when(userService.findByEmailAndCompany(null, 1L)).thenReturn(Optional.empty());
            when(userService.findByEmailAndCompany("a@example.com", 1L)).thenReturn(Optional.of(userA));
            when(userService.findByEmailAndCompany("b@example.com", 1L)).thenReturn(Optional.of(userB));

            Helper.populateWorkOrderBaseFromImportDTO(workOrderBase, dto, company,
                    locationService, teamService, userService, assetService, workOrderCategoryService);

            verify(workOrderBase).setAssignedTo(Arrays.asList(userA, userB));
        }

        @Test
        void withAsset_setsIt() {
            workOrderBase = mock(WorkOrderBase.class);
            dto = new WorkOrderImportDTO();
            dto.setTitle("WO");
            dto.setPriority("None");
            dto.setEstimatedDuration(0);
            dto.setAssetName("Pump-01");
            dto.setAssignedToEmails(new ArrayList<>());

            company = new Company();
            company.setId(1L);

            Asset asset = new Asset();
            when(assetService.findByNameIgnoreCaseAndCompany("Pump-01", 1L))
                    .thenReturn(Collections.singletonList(asset));

            Helper.populateWorkOrderBaseFromImportDTO(workOrderBase, dto, company,
                    locationService, teamService, userService, assetService, workOrderCategoryService);

            verify(workOrderBase).setAsset(asset);
        }
    }
}
