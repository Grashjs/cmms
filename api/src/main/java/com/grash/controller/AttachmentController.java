package com.grash.controller;

import com.grash.factory.StorageServiceFactory;
import com.grash.storage.AttachmentImageVerifier;
import com.grash.storage.AttachmentLinks;
import com.grash.storage.AttachmentPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentLinks links;
    private final StorageServiceFactory storageServiceFactory;
    private final AttachmentImageVerifier imageVerifier;
    // Bound buffered prefixes and decoding across all requests, including HEAD, before allocating them.
    private final Semaphore inspections = new Semaphore(2, true);

    @GetMapping("/files/content/{token}")
    public void content(@PathVariable String token, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AttachmentLinks.Claim claim = links.verify(token);
        try {
            if (!inspections.tryAcquire(30, TimeUnit.SECONDS)) {
                busy(response);
                return;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            busy(response);
            return;
        }
        boolean inspecting = true;
        try (var input = storageServiceFactory.getStorageService().openStream(claim.path())) {
            writeVerifiedPrefix(input, claim.filename(), request, response);
            // The prefix is now out of scope. Large downloads can stream without occupying a decoding slot.
            inspecting = false;
            inspections.release();
            if (!"HEAD".equals(request.getMethod())) {
                input.transferTo(response.getOutputStream());
            }
        } finally {
            if (inspecting) inspections.release();
        }
    }

    private void writeVerifiedPrefix(InputStream input, String filename, HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        byte[] prefix = input.readNBytes(AttachmentPolicy.MAX_IMAGE_BYTES + 1);
        String imageType = imageVerifier.verifiedType(prefix);
        response.setContentType(imageType == null ? AttachmentPolicy.DOWNLOAD_TYPE : imageType);
        response.setHeader("Content-Disposition", AttachmentPolicy.disposition(filename, imageType != null));
        response.setHeader("Cache-Control", AttachmentPolicy.CACHE_CONTROL);
        if (prefix.length <= AttachmentPolicy.MAX_IMAGE_BYTES) response.setContentLengthLong(prefix.length);
        if (!"HEAD".equals(request.getMethod())) response.getOutputStream().write(prefix);
    }

    private void busy(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Retry-After", "1");
    }
}
