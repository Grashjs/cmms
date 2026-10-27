package com.grash.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.license.LicensingState;
import com.grash.model.RequestPortal;
import com.grash.model.RequestPortalField;
import com.grash.model.enums.PortalFieldType;
import com.grash.repository.RequestPortalRepository;
import com.grash.repository.UserRepository;
import com.grash.service.LicenseService;
import com.grash.service.MinioService;
import com.grash.storage.AttachmentLinks;
import com.grash.storage.AttachmentPolicy;
import com.grash.storage.StorageMetadataMigration;
import io.minio.*;
import io.minio.http.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Opt-in local journey. Requires disposable PostgreSQL/MinIO/Nginx; never point it at real data. */
@EnabledIfEnvironmentVariable(named = "CMMS_LOCAL_ATTACHMENT_TEST", matches = "true")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
        "spring.config.location=classpath:application-test.yml", "server.port=8098", "server.address=0.0.0.0",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:55439/attachment_test",
        "spring.datasource.username=attachment_test", "spring.datasource.password=attachment_test_only",
        "spring.jackson.serialization.fail-on-empty-beans=false", "springdoc.pre-loading-enabled=false",
        "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false",
        "api.host=http://127.0.0.1:3007/api", "frontend.url=http://127.0.0.1:3007",
        "allowed-organization-admins=", "license-key=", "intercom.token=", "ldap.sync.enabled=false",
        "storage.minio.endpoint=http://127.0.0.1:59000", "storage.minio.bucket=attachment-security-test",
        "storage.minio.access-key=attachment-test", "storage.minio.secret-key=attachment-test-password",
        "security.rate-limit.enabled=true", "security.recaptcha-secret-key=", "mail.enable=false"
})
class AttachmentLocalIntegrationTest {
    @Autowired MinioService storage;
    @Autowired AttachmentLinks links;
    @Autowired UserRepository users;
    @Autowired RequestPortalRepository portals;
    @Autowired PlatformTransactionManager transactions;
    // Only commercial entitlement checks are simulated. HTTP/security, persistence and storage are real.
    @MockitoSpyBean LicenseService license;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String api = "http://127.0.0.1:8098";
    private final String bucket = "attachment-security-test";

    @Test
    void realUploadsDeliveryMigrationAndBrowser() throws Exception {
        when(license.hasEntitlement(any())).thenReturn(true);
        when(license.getLicensingState()).thenReturn(LicensingState.builder().hasLicense(true).valid(true)
                .planName("Local test").usersCount(300).entitlements(Arrays.stream(LicenseEntitlement.values())
                        .map(Enum::name).collect(Collectors.toSet())).build());
        String email = "attachment-" + UUID.randomUUID() + "@example.com";
        String password = "Local-attachment-2026!";
        JsonNode signup = postJson("/auth/signup", Map.of("email", email, "password", password,
                "firstName", "Attachment", "lastName", "Test", "phone", "00000000",
                "companyName", "Attachment test", "employeesCount", 1, "timeZone", "UTC"));
        assertThat(signup.path("success").asBoolean()).isTrue();
        String jwt = postJson("/auth/signin", Map.of("email", email, "password", password, "type", "CLIENT"))
                .path("accessToken").asText();
        assertThat(jwt).isNotBlank();
        String portalUuid = UUID.randomUUID().toString();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            RequestPortal portal = new RequestPortal();
            portal.setCompany(users.findByEmailIgnoreCase(email).orElseThrow().getCompany());
            portal.setTitle("Harmless attachment test");
            portal.setUuid(portalUuid);
            RequestPortalField field = new RequestPortalField();
            field.setCompany(portal.getCompany());
            field.setType(PortalFieldType.FILES);
            field.setRequestPortal(portal);
            portal.getFields().add(field);
            portals.saveAndFlush(portal);
        });

        byte[] html = "<!doctype html><script>localStorage.setItem('attachment-executed','yes')</script>Harmless test"
                .getBytes(StandardCharsets.UTF_8);
        byte[] svg = "<svg xmlns='http://www.w3.org/2000/svg' onload=\"localStorage.setItem('attachment-executed','yes')\"><rect width='2' height='2'/></svg>"
                .getBytes(StandardCharsets.UTF_8);
        List<Fixture> fixtures = new ArrayList<>(List.of(
                new Fixture("active.html", "text/html", html, null),
                new Fixture("fake.pdf", "application/pdf", html, null),
                new Fixture("fake.png", "image/png", svg, null),
                new Fixture("active.svg", "image/svg+xml", svg, null),
                new Fixture("unknown.txt", "text/plain", "unrecognized".getBytes(), null),
                new Fixture("truncated.gif", "image/gif", "GIF89a<html>test</html>".getBytes(), null)));
        for (String format : List.of("png", "jpeg", "gif")) {
            var bytes = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), format, bytes);
            fixtures.add(new Fixture("real-" + format + ".html", "text/html", bytes.toByteArray(), "image/" + format));
        }
        try (var input = getClass().getResourceAsStream("/attachments/raster.webp")) {
            fixtures.add(new Fixture("real-webp.pdf", "application/pdf", input.readAllBytes(), "image/webp"));
        }

        JsonNode publicFiles = upload("/files/upload/request-portal/" + portalUuid + "?type=OTHER", fixtures, null);
        JsonNode privateFiles = upload("/files/upload?type=OTHER&hidden=false", List.of(fixtures.get(1)), jwt);
        assertThat(privateFiles.size()).isEqualTo(1);
        List<Map<String, Object>> browserFiles = new ArrayList<>();
        for (int i = 0; i < fixtures.size(); i++) {
            Fixture fixture = fixtures.get(i);
            String url = publicFiles.get(i).path("url").asText();
            assertThat(url).startsWith("http://127.0.0.1:3007/api/files/content/");
            checkResponse(url, fixture);
            checkResponse(url.replace("http://127.0.0.1:3007/api", api), fixture);
            browserFiles.add(Map.of("name", fixture.name(), "url", url, "image", fixture.imageType() != null));
        }
        checkResponse(privateFiles.get(0).path("url").asText(), fixtures.get(1));
        String overrideAttempt = publicFiles.get(0).path("url").asText()
                + "?response-content-type=text/html&response-content-disposition=inline";
        checkResponse(overrideAttempt, fixtures.get(0));
        browserFiles.add(Map.of("name", "response-header-override", "url", overrideAttempt, "image", false));
        // Existing bytes/metadata and provider-signed URLs, independently of the upload pipeline.
        try (MinioClient client = MinioClient.builder().endpoint("http://127.0.0.1:59000")
                .credentials("attachment-test", "attachment-test-password").build()) {
            client.putObject(PutObjectArgs.builder().bucket(bucket).object("legacy.html")
                    .stream(new ByteArrayInputStream(html), html.length, -1).contentType("text/html").build());
            String legacyGateway = links.sign("legacy.html", "legacy.html", 5);
            checkResponse(legacyGateway, fixtures.get(0));
            String direct = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET)
                    .bucket(bucket).object("legacy.html").expiry(300).build());
            String proxySigned;
            try (MinioClient signer = MinioClient.builder().endpoint("http://minio:9000").region("us-east-1")
                    .credentials("attachment-test", "attachment-test-password").build()) {
                proxySigned = signer.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET)
                        .bucket(bucket).object("legacy.html").expiry(300).build())
                        .replace("http://minio:9000", "http://127.0.0.1:3007/storage");
            }
            checkResponse(proxySigned, fixtures.get(0));
            byte[] before;
            try (var input = client.getObject(GetObjectArgs.builder().bucket(bucket).object("legacy.html").build())) {
                before = input.readAllBytes();
            }
            assertThat(StorageMetadataMigration.minio(client, bucket, "", false).changed()).isGreaterThan(0);
            assertThat(client.statObject(StatObjectArgs.builder().bucket(bucket).object("legacy.html").build()).contentType()).isEqualTo("text/html");
            assertThat(StorageMetadataMigration.minio(client, bucket, "", true).changed()).isGreaterThan(0);
            assertThat(StorageMetadataMigration.minio(client, bucket, "", false).changed()).isZero();
            try (var input = client.getObject(GetObjectArgs.builder().bucket(bucket).object("legacy.html").build())) {
                assertThat(input.readAllBytes()).isEqualTo(before);
            }
            HttpResponse<byte[]> directResponse = get(direct);
            assertThat(directResponse.statusCode()).isEqualTo(200);
            assertThat(directResponse.headers().firstValue("Content-Type").orElse("")).isEqualTo(AttachmentPolicy.DOWNLOAD_TYPE);
            assertThat(directResponse.headers().firstValue("Content-Disposition").orElse("")).startsWith("attachment;");
            // No pretend CSP on a native provider response: the public app always uses the API gateway.
            assertThat(get("http://127.0.0.1:59000/" + bucket + "/legacy.html").statusCode()).isEqualTo(403);
            browserFiles.add(Map.of("name", "legacy-proxy", "url", proxySigned, "image", false));
            browserFiles.add(Map.of("name", "legacy-direct-migrated", "url", direct, "image", false, "nativeStorage", true));
        }

        // Generated PDFs and exports use the same byte[] storage overload.
        var pdf = new ByteArrayOutputStream();
        try (var document = new com.itextpdf.kernel.pdf.PdfDocument(new com.itextpdf.kernel.pdf.PdfWriter(pdf))) {
            document.addNewPage();
        }
        String generated = storage.upload(pdf.toByteArray(), "Work Order Report.pdf", "generated");
        String reportUrl = storage.generateSignedUrl(generated, 5);
        checkResponse(reportUrl, new Fixture("report.pdf", "application/pdf", pdf.toByteArray(), null));
        browserFiles.add(Map.of("name", "generated-report.pdf", "url", reportUrl, "image", false));
        byte[] csv = "Name,Count\nHarmless,1\n".getBytes(StandardCharsets.UTF_8);
        String exportUrl = storage.uploadAndSign(new org.springframework.mock.web.MockMultipartFile(
                "file", "export.csv", "text/csv", csv), "exports");
        checkResponse(exportUrl, new Fixture("export.csv", "text/csv", csv, null));
        browserFiles.add(Map.of("name", "generated-export.csv", "url", exportUrl, "image", false));
        String expired = links.sign("legacy.html", "legacy.html", 5);
        String invalid = expired.substring(0, expired.length() - 8) + "invalid!";
        assertThat(get(invalid).statusCode()).isEqualTo(403);
        assertThat(get(invalid).headers().firstValue("Content-Security-Policy").orElse("")).isEqualTo(AttachmentPolicy.CSP);

        String node = System.getenv("CMMS_BROWSER_NODE");
        if (node != null) {
            Path manifest = Files.createTempFile("cmms-attachment-browser-", ".json");
            Path browserLog = Files.createTempFile("cmms-attachment-browser-", ".log");
            try {
                Files.writeString(manifest, json.writeValueAsString(Map.of("files", browserFiles,
                        "email", email, "password", password, "portalUuid", portalUuid)));
                Process browser = new ProcessBuilder(node, "../scripts/test-attachment-browser.cjs", manifest.toString())
                        .redirectErrorStream(true).redirectOutput(browserLog.toFile()).start();
                boolean finished = browser.waitFor(180, TimeUnit.SECONDS);
                if (!finished) browser.destroyForcibly();
                System.out.print(Files.readString(browserLog));
                assertThat(finished).as("Browser test timeout").isTrue();
                assertThat(browser.exitValue()).as("Browser verification").isZero();
            } finally {
                Files.deleteIfExists(manifest);
                Files.deleteIfExists(browserLog);
            }
        } else System.out.println("Browser verification NOT RUN: CMMS_BROWSER_NODE is unset");
    }

    private JsonNode postJson(String path, Object body) throws Exception {
        var response = http.send(HttpRequest.newBuilder(URI.create(api + path)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(path + " " + response.body()).isBetween(200, 299);
        return json.readTree(response.body());
    }

    private JsonNode upload(String path, List<Fixture> files, String token) throws Exception {
        String boundary = "attachment-test-" + UUID.randomUUID();
        var body = new ByteArrayOutputStream();
        for (Fixture file : files) {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"" + file.name()
                    + "\"\r\nContent-Type: " + file.mime() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(file.bytes());
            body.write("\r\n".getBytes());
        }
        body.write(("--" + boundary + "--\r\n").getBytes());
        var request = HttpRequest.newBuilder(URI.create(api + path)).header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (token != null) request.header("Authorization", "Bearer " + token);
        var response = http.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(path + " " + response.body()).isEqualTo(200);
        return json.readTree(response.body());
    }

    private HttpResponse<byte[]> get(String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private void checkResponse(String url, Fixture fixture) throws Exception {
        var response = get(url);
        assertThat(response.statusCode()).as(fixture.name()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(fixture.bytes());
        assertThat(response.headers().allValues("Content-Type")).containsExactly(fixture.imageType() == null
                ? AttachmentPolicy.DOWNLOAD_TYPE : fixture.imageType());
        assertThat(response.headers().firstValue("Content-Disposition").orElse(""))
                .startsWith(fixture.imageType() == null ? "attachment;" : "inline;");
        assertThat(response.headers().firstValue("Content-Security-Policy").orElse("")).isEqualTo(AttachmentPolicy.CSP);
        assertThat(response.headers().firstValue("X-Content-Type-Options").orElse("")).isEqualTo("nosniff");
    }

    private record Fixture(String name, String mime, byte[] bytes, String imageType) { }
}
