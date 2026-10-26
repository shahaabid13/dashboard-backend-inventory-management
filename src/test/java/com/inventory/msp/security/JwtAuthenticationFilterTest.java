package com.inventory.msp.security;

import com.inventory.msp.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @Test
    void invalidOrExpiredBearerTokenReturnsUnauthorizedWithoutContinuing() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(jwtUtil.validateToken("expired")).thenReturn(false);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] continued = {false};

        SecurityContextHolder.clearContext();
        try {
            filter.doFilter(request, response, (req, res) -> continued[0] = true);
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertFalse(continued[0]);
    }
}
