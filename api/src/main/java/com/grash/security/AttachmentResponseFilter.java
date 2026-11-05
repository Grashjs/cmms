package com.grash.security;

import com.grash.storage.AttachmentPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Also covers errors and requests reaching the API without the reverse proxy. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AttachmentResponseFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getServletPath().startsWith("/files/content/")) {
            response.setHeader("Content-Security-Policy", AttachmentPolicy.CSP);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("Referrer-Policy", "no-referrer");
            response.setHeader("Cache-Control", AttachmentPolicy.CACHE_CONTROL);
        }
        chain.doFilter(request, response);
    }
}
