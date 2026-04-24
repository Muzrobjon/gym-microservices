package com.epam.gym.trainerworkloadservice.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ServiceTokenFilter Unit Tests")
class ServiceTokenFilterTest {

    private ServiceTokenFilter serviceTokenFilter;

    private static final String EXPECTED_TOKEN = "internal-secret-token-12345";

    @BeforeEach
    void setUp() {
        serviceTokenFilter = new ServiceTokenFilter();
        ReflectionTestUtils.setField(serviceTokenFilter, "expectedToken", EXPECTED_TOKEN);
    }

    // ==================== Valid Cases ====================

    @Test
    @DisplayName("Valid token with 'Bearer ' prefix: returns true")
    void isValidServiceCall_ValidTokenWithBearerPrefix_ReturnsTrue() {
        String authHeader = "Bearer " + EXPECTED_TOKEN;

        boolean result = serviceTokenFilter.isValidServiceCall(authHeader);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Valid token without 'Bearer ' prefix: returns true (replace is no-op)")
    void isValidServiceCall_ValidTokenWithoutPrefix_ReturnsTrue() {
        String authHeader = EXPECTED_TOKEN;

        boolean result = serviceTokenFilter.isValidServiceCall(authHeader);

        assertThat(result).isTrue();
    }

    // ==================== Invalid Cases ====================

    @Test
    @DisplayName("Null authHeader: returns false")
    void isValidServiceCall_NullHeader_ReturnsFalse() {
        boolean result = serviceTokenFilter.isValidServiceCall(null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Empty authHeader: returns false")
    void isValidServiceCall_EmptyHeader_ReturnsFalse() {
        boolean result = serviceTokenFilter.isValidServiceCall("");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Wrong token with 'Bearer ' prefix: returns false")
    void isValidServiceCall_WrongTokenWithBearer_ReturnsFalse() {
        String authHeader = "Bearer wrong-token";

        boolean result = serviceTokenFilter.isValidServiceCall(authHeader);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Wrong token without 'Bearer ' prefix: returns false")
    void isValidServiceCall_WrongTokenWithoutBearer_ReturnsFalse() {
        boolean result = serviceTokenFilter.isValidServiceCall("some-random-token");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Only 'Bearer ' with no token: returns false")
    void isValidServiceCall_OnlyBearerPrefix_ReturnsFalse() {
        String authHeader = "Bearer ";

        boolean result = serviceTokenFilter.isValidServiceCall(authHeader);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Token with leading/trailing spaces: returns false")
    void isValidServiceCall_TokenWithExtraSpaces_ReturnsFalse() {
        String authHeader = "Bearer  " + EXPECTED_TOKEN + " ";

        boolean result = serviceTokenFilter.isValidServiceCall(authHeader);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Case-sensitive: different case returns false")
    void isValidServiceCall_CaseSensitive_ReturnsFalse() {
        String authHeader = "Bearer " + EXPECTED_TOKEN.toUpperCase();

        boolean result = serviceTokenFilter.isValidServiceCall(authHeader);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Partial token match: returns false")
    void isValidServiceCall_PartialToken_ReturnsFalse() {
        String partialToken = EXPECTED_TOKEN.substring(0, EXPECTED_TOKEN.length() - 2);
        String authHeader = "Bearer " + partialToken;

        boolean result = serviceTokenFilter.isValidServiceCall(authHeader);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Basic auth format: returns false")
    void isValidServiceCall_BasicAuthFormat_ReturnsFalse() {
        String authHeader = "Basic dXNlcjpwYXNz";

        boolean result = serviceTokenFilter.isValidServiceCall(authHeader);

        assertThat(result).isFalse();
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Valid token with single 'Bearer ' prefix: returns true")
    void isValidServiceCall_SingleBearerPrefix_ReturnsTrue() {
        ReflectionTestUtils.setField(serviceTokenFilter, "expectedToken", "some-token");
        String authHeader = "Bearer some-token";

        boolean result = serviceTokenFilter.isValidServiceCall(authHeader);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Multiple 'Bearer ' prefixes: all replaced, returns true if matches")
    void isValidServiceCall_MultipleBearerPrefixes_AllReplaced() {
        ReflectionTestUtils.setField(serviceTokenFilter, "expectedToken", "my-token");
        String authHeader = "Bearer Bearer my-token";

        boolean result = serviceTokenFilter.isValidServiceCall(authHeader);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Whitespace-only header: returns false")
    void isValidServiceCall_WhitespaceOnly_ReturnsFalse() {
        boolean result = serviceTokenFilter.isValidServiceCall("   ");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Expected token is null: any input returns false")
    void isValidServiceCall_ExpectedTokenNull_ReturnsFalse() {
        ReflectionTestUtils.setField(serviceTokenFilter, "expectedToken", null);

        boolean result = serviceTokenFilter.isValidServiceCall("Bearer some-token");

        assertThat(result).isFalse();
    }
}