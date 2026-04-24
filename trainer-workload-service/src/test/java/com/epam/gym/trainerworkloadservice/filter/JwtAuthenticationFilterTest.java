package com.epam.gym.trainerworkloadservice.filter;

import com.epam.gym.trainerworkloadservice.config.service.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private ServiceTokenFilter serviceTokenFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== Service Token Tests ====================

    @Test
    @DisplayName("Valid service token: sets ROLE_SERVICE authentication and returns")
    void doFilterInternal_ValidServiceToken_SetsServiceAuthentication() throws ServletException, IOException {
        // Given
        String serviceToken = "Service internal-secret-token";
        when(request.getHeader("Authorization")).thenReturn(serviceToken);
        when(serviceTokenFilter.isValidServiceCall(serviceToken)).thenReturn(true);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("gym-crm-service");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_SERVICE");
        assertThat(auth.isAuthenticated()).isTrue();

        verify(filterChain, times(1)).doFilter(request, response);
        // JWT validation should NOT happen for service calls
        verify(jwtProvider, never()).validateToken(any());
        verify(jwtProvider, never()).getUsernameFromToken(any());
    }

    @Test
    @DisplayName("Service token invalid: proceeds to JWT validation")
    void doFilterInternal_InvalidServiceToken_ProceedsToJwtCheck() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer some.jwt.token");
        when(serviceTokenFilter.isValidServiceCall(anyString())).thenReturn(false);
        when(jwtProvider.validateToken("some.jwt.token")).thenReturn(true);
        when(jwtProvider.getUsernameFromToken("some.jwt.token")).thenReturn("john.doe");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("john.doe");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        verify(filterChain, times(1)).doFilter(request, response);
    }

    // ==================== Valid JWT Tests ====================

    @Test
    @DisplayName("Valid JWT: sets ROLE_USER authentication with username")
    void doFilterInternal_ValidJwt_SetsUserAuthentication() throws ServletException, IOException {
        // Given
        String jwtToken = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(serviceTokenFilter.isValidServiceCall(anyString())).thenReturn(false);
        when(jwtProvider.validateToken(jwtToken)).thenReturn(true);
        when(jwtProvider.getUsernameFromToken(jwtToken)).thenReturn("alice");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("alice");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        verify(jwtProvider, times(1)).validateToken(jwtToken);
        verify(jwtProvider, times(1)).getUsernameFromToken(jwtToken);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Invalid JWT: no authentication is set")
    void doFilterInternal_InvalidJwt_NoAuthenticationSet() throws ServletException, IOException {
        // Given
        String jwtToken = "invalid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(serviceTokenFilter.isValidServiceCall(anyString())).thenReturn(false);
        when(jwtProvider.validateToken(jwtToken)).thenReturn(false);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(jwtProvider, times(1)).validateToken(jwtToken);
        verify(jwtProvider, never()).getUsernameFromToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // ==================== No Token Tests ====================

    @Test
    @DisplayName("No Authorization header: proceeds without authentication")
    void doFilterInternal_NoAuthHeader_NoAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(serviceTokenFilter.isValidServiceCall(null)).thenReturn(false);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(jwtProvider, never()).validateToken(any());
        verify(jwtProvider, never()).getUsernameFromToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Empty Authorization header: proceeds without authentication")
    void doFilterInternal_EmptyAuthHeader_NoAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("");
        when(serviceTokenFilter.isValidServiceCall("")).thenReturn(false);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(jwtProvider, never()).validateToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Header without 'Bearer ' prefix: no JWT extracted, no authentication")
    void doFilterInternal_HeaderWithoutBearerPrefix_NoAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");
        when(serviceTokenFilter.isValidServiceCall("Basic dXNlcjpwYXNz")).thenReturn(false);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(jwtProvider, never()).validateToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Only 'Bearer ' with no token: validateToken called with empty string")
    void doFilterInternal_OnlyBearerNoToken_NoAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        when(serviceTokenFilter.isValidServiceCall("Bearer ")).thenReturn(false);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        // "Bearer " after substring(7) gives "" — StringUtils.hasText("") is false
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(jwtProvider, never()).validateToken(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("JwtProvider throws exception: authentication not set, filterChain still continues")
    void doFilterInternal_JwtProviderThrowsException_ContinuesFilterChain() throws ServletException, IOException {
        // Given
        String jwtToken = "bad.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(serviceTokenFilter.isValidServiceCall(anyString())).thenReturn(false);
        when(jwtProvider.validateToken(jwtToken)).thenThrow(new RuntimeException("Token parse error"));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then: no crash, chain continues, no auth set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("getUsernameFromToken throws exception: authentication not set, chain continues")
    void doFilterInternal_GetUsernameThrowsException_ContinuesFilterChain() throws ServletException, IOException {
        // Given
        String jwtToken = "valid.but.broken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(serviceTokenFilter.isValidServiceCall(anyString())).thenReturn(false);
        when(jwtProvider.validateToken(jwtToken)).thenReturn(true);
        when(jwtProvider.getUsernameFromToken(jwtToken))
                .thenThrow(new RuntimeException("Username extraction error"));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("ServiceTokenFilter throws exception: chain still continues")
    void doFilterInternal_ServiceTokenFilterThrowsException_ContinuesFilterChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("something");
        when(serviceTokenFilter.isValidServiceCall(anyString()))
                .thenThrow(new RuntimeException("Service check error"));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain, times(1)).doFilter(request, response);
    }

    // ==================== Filter Chain Tests ====================

    @Test
    @DisplayName("Filter chain is always called (even with no auth)")
    void doFilterInternal_AlwaysCallsFilterChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(serviceTokenFilter.isValidServiceCall(null)).thenReturn(false);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Service call: filter chain called exactly once (early return)")
    void doFilterInternal_ServiceCall_FilterChainCalledOnce() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Service token");
        when(serviceTokenFilter.isValidServiceCall("Service token")).thenReturn(true);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then — only ONE call (early return after service auth)
        verify(filterChain, times(1)).doFilter(request, response);
    }
}