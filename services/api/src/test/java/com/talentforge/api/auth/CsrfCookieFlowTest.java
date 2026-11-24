package com.talentforge.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.talentforge.api.identity.AppUserDetails;
import com.talentforge.api.identity.MembershipRepository;
import com.talentforge.api.security.SecurityConfig;
import com.talentforge.api.web.ApiExceptionHandler;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the real cookie/header CSRF round trip the Angular client uses.
 *
 * <p>It runs in a fresh context and never uses the {@code csrf()} post-processor:
 * that post-processor swaps the filter's token repository for a session-based
 * test one in the shared context, which made header-name assertions depend on
 * test order.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CsrfCookieFlowTest {

    private static final AppUserDetails USER = new AppUserDetails(
            UUID.fromString("00000000-0000-4000-8000-000000000001"), "riley@example.org", "Riley Recruiter", null,
            true);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private MembershipRepository memberships;

    private Cookie fetchToken() throws Exception {
        Cookie cookie = mvc.perform(get("/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).as("Angular must be able to read the cookie").isFalse();
        return cookie;
    }

    @Test
    void cookieValueEchoedInAngularHeaderIsAccepted() throws Exception {
        Cookie token = fetchToken();

        mvc.perform(post("/v1/auth/logout").with(user(USER)).cookie(token).header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void cookieWithoutHeaderIsRejected() throws Exception {
        Cookie token = fetchToken();

        mvc.perform(post("/v1/auth/logout").with(user(USER)).cookie(token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void headerThatDoesNotMatchCookieIsRejected() throws Exception {
        Cookie token = fetchToken();

        mvc.perform(post("/v1/auth/logout").with(user(USER)).cookie(token).header("X-XSRF-TOKEN", "forged"))
                .andExpect(status().isForbidden());
    }
}
