package com.grash.controller;

import com.grash.exception.CustomException;
import com.grash.factory.StorageServiceFactory;
import com.grash.security.AttachmentResponseFilter;
import com.grash.service.StorageService;
import com.grash.storage.AttachmentImageVerifier;
import com.grash.storage.AttachmentLinks;
import com.grash.storage.AttachmentPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttachmentControllerTest extends AbstractControllerTest {
    @MockitoBean AttachmentLinks links;
    @MockitoBean StorageServiceFactory factory;
    @MockitoBean AttachmentImageVerifier imageVerifier;
    @Autowired WebApplicationContext context;
    private MockMvc mvc;
    private StorageService storage;
    private final String path = "/files/content/test-capability";

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(new AttachmentResponseFilter()).build();
        storage = mock(StorageService.class);
        when(factory.getStorageService()).thenReturn(storage);
        when(imageVerifier.verifiedType(any())).thenAnswer(invocation -> AttachmentPolicy.verifiedImageType(invocation.getArgument(0)));
        when(links.verify("test-capability")).thenReturn(new AttachmentLinks.Claim("object", "../fake.pdf", Long.MAX_VALUE, "scope"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"<html><script>document.title='test'</script></html>",
            "<svg onload='document.title=1'/>", "%PDF-1.7\n% harmless PDF fixture", "GIF89a fake raster", "unknown"})
    void activeDocumentsAreDownloadsWithSandboxEvenWhenNamedPdf(String payload) throws Exception {
        when(storage.openStream("object")).thenReturn(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)));
        mvc.perform(get(path).servletPath(path))
                .andExpect(status().isOk()).andExpect(content().string(payload))
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"fake.pdf\""))
                .andExpect(header().string("Content-Security-Policy", AttachmentPolicy.CSP))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "private, no-store"));
    }

    @Test
    void realImageIsInlineDespiteMisleadingFilenameAndHeadHasNoBody() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        when(storage.openStream("object")).thenAnswer(invocation -> new ByteArrayInputStream(bytes.toByteArray()));
        mvc.perform(get(path).servletPath(path)).andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"fake.pdf\""))
                .andExpect(header().string("Content-Security-Policy", AttachmentPolicy.CSP));
        mvc.perform(head(path).servletPath(path)).andExpect(status().isOk()).andExpect(content().bytes(new byte[0]))
                .andExpect(header().longValue("Content-Length", bytes.size()));
    }

    @Test
    void badCapabilityNeverReadsStorageAndErrorHasSecurityHeaders() throws Exception {
        when(links.verify("test-capability")).thenThrow(new CustomException("Invalid link", HttpStatus.FORBIDDEN));
        mvc.perform(get(path).servletPath(path)).andExpect(status().isForbidden())
                .andExpect(header().string("Content-Security-Policy", AttachmentPolicy.CSP))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        verifyNoInteractions(storage);
    }

    @Test
    void streamsLargeUnknownFilesWithoutTruncationAndClosesInput() throws Exception {
        byte[] data = new byte[AttachmentPolicy.MAX_IMAGE_BYTES + 100];
        data[data.length - 1] = 42;
        var input = spy(new ByteArrayInputStream(data));
        when(storage.openStream("object")).thenReturn(input);
        mvc.perform(get(path).servletPath(path)).andExpect(status().isOk())
                .andExpect(content().contentType(AttachmentPolicy.DOWNLOAD_TYPE)).andExpect(content().bytes(data));
        verify(input).close();
    }

    @Test
    void replacingAnObjectNeverReusesItsPreviousInlineHeaders() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        byte[] replacement = "<html>harmless replacement</html>".getBytes(StandardCharsets.UTF_8);
        when(storage.openStream("object")).thenReturn(new ByteArrayInputStream(bytes.toByteArray()),
                new ByteArrayInputStream(replacement));
        mvc.perform(get(path).servletPath(path)).andExpect(content().contentType("image/png"));
        mvc.perform(head(path).servletPath(path)).andExpect(status().isOk())
                .andExpect(content().contentType(AttachmentPolicy.DOWNLOAD_TYPE))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"fake.pdf\""))
                .andExpect(header().longValue("Content-Length", replacement.length))
                .andExpect(content().bytes(new byte[0]));
        verify(imageVerifier).verifiedType(replacement);
    }

    @Test
    void concurrentRequestsWaitBeforeAllocatingAnotherPrefixAndThenSucceed() throws Exception {
        var entered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        byte[] bytes = "harmless file".getBytes(StandardCharsets.UTF_8);
        when(storage.openStream("object")).thenAnswer(invocation -> new ByteArrayInputStream(bytes) {
            @Override
            public byte[] readNBytes(int length) throws IOException {
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Test did not release storage");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException(interrupted);
                }
                return super.readNBytes(length);
            }
        });
        var workers = Executors.newFixedThreadPool(2);
        var third = new FutureTask<>(() -> mvc.perform(head(path).servletPath(path)).andReturn());
        var thirdThread = new Thread(third, "third-attachment-request");
        try {
            var first = workers.submit(() -> mvc.perform(get(path).servletPath(path)).andReturn());
            var second = workers.submit(() -> mvc.perform(get(path).servletPath(path)).andReturn());
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            thirdThread.start();
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                while (thirdThread.getState() != Thread.State.TIMED_WAITING) Thread.sleep(1);
            });
            verify(storage, times(2)).openStream("object");
            assertThat(third.isDone()).isFalse();
            release.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS).getResponse().getContentAsByteArray()).isEqualTo(bytes);
            assertThat(second.get(5, TimeUnit.SECONDS).getResponse().getContentAsByteArray()).isEqualTo(bytes);
            var head = third.get(5, TimeUnit.SECONDS).getResponse();
            assertThat(head.getStatus()).isEqualTo(200);
            assertThat(head.getContentAsByteArray()).isEmpty();
            assertThat(head.getContentLength()).isEqualTo(bytes.length);
        } finally {
            release.countDown();
            workers.shutdownNow();
            thirdThread.interrupt();
            thirdThread.join(5000);
        }
    }

    @Test
    void failedStorageReadsReleaseTheInspectionSlots() throws Exception {
        when(storage.openStream("object")).thenThrow(new IllegalStateException("test storage failure"))
                .thenThrow(new IllegalStateException("second test failure"))
                .thenReturn(new ByteArrayInputStream(new byte[]{1}));
        mvc.perform(get(path).servletPath(path)).andExpect(status().isInternalServerError())
                .andExpect(header().string("Content-Security-Policy", AttachmentPolicy.CSP));
        mvc.perform(get(path).servletPath(path)).andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                mvc.perform(get(path).servletPath(path)).andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1})));
    }
}
