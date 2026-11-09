package com.grash.security;

import com.grash.dto.offline.MobileOfflineLimits;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobileOfflineRequestSizeFilterTest {
    final MobileOfflineRequestSizeFilter filter = new MobileOfflineRequestSizeFilter();

    @Test void boundsTheActualBodyEvenWhenTheDeclaredLengthIsUnknownOrWrong() throws Exception {
        for (long declared : new long[]{-1, 1}) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mobile-offline/operations") {
                @Override public long getContentLengthLong() { return declared; }
            };
            request.setContent(new byte[MobileOfflineLimits.PILOT.snapshotBytes() + 1]);
            var response = new MockHttpServletResponse(); var chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            assertEquals(413, response.getStatus());
            assertTrue(response.getContentAsString().contains("OPERATION_LIMIT_EXCEEDED"));
            verifyNoInteractions(chain);
        }
    }

    @Test void validBodyIsPreservedAndOtherRoutesAreUntouched() throws Exception {
        byte[] body = "{\"content\":\"é\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/mobile-offline/operations;test=1");
        request.setContextPath("/api"); request.setContent(body);
        filter.doFilter(request, new MockHttpServletResponse(), (wrapped, response) ->
                assertArrayEquals(body, wrapped.getInputStream().readAllBytes()));
        MockHttpServletRequest other = new MockHttpServletRequest("POST", "/comments");
        other.setContent(new byte[MobileOfflineLimits.PILOT.snapshotBytes() + 1]);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(other, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(eq(other), any());
    }
}
