package com.talentforge.api.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.talentforge.api.identity.AppUserDetails;
import com.talentforge.api.identity.MembershipRepository;
import com.talentforge.api.identity.MembershipView;
import com.talentforge.api.identity.Role;
import com.talentforge.api.security.SecurityConfig;
import com.talentforge.api.web.ApiExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class AuthControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID ORG_ID = UUID.fromString("00000000-0000-4000-8000-0000000000aa");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private MembershipRepository memberships;

    @BeforeEach
    void setUp() {
        when(memberships.findViewsByUserId(USER_ID))
                .thenReturn(List.of(new MembershipView(ORG_ID, "Northwind Talent (synthetic)", Role.RECRUITER)));
        when(userDetailsService.loadUserByUsername(any())).thenThrow(new UsernameNotFoundException("none"));
    }

    private void givenAccount(boolean enabled) {
        AppUserDetails details = new AppUserDetails(USER_ID, "riley@example.org", "Riley Recruiter",
                passwordEncoder.encode("correct horse"), enabled);
        // doReturn: when(...) would call the mock and hit the catch-all thenThrow stub.
        doReturn(details).when(userDetailsService).loadUserByUsername("riley@example.org");
    }

    @Test
    void loginReturnsCurrentUserAndStartsSession() throws Exception {
        givenAccount(true);

        mvc.perform(post("/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Riley@Example.org\",\"password\":\"correct horse\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("riley@example.org"))
                .andExpect(jsonPath("$.memberships[0].organizationId").value(ORG_ID.toString()))
                .andExpect(jsonPath("$.memberships[0].role").value("recruiter"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void wrongPasswordGetsGeneric401() throws Exception {
        givenAccount(true);

        mvc.perform(post("/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"riley@example.org\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    void unknownEmailGetsSame401() throws Exception {
        mvc.perform(post("/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.org\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    void disabledAccountGetsSame401() throws Exception {
        givenAccount(false);

        mvc.perform(post("/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"riley@example.org\",\"password\":\"correct horse\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    void invalidFieldsGet422() throws Exception {
        mvc.perform(post("/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fields[0].field").value("email"))
                .andExpect(jsonPath("$.fields[1].field").value("password"));
    }

    @Test
    void loginWithoutCsrfTokenIsForbidden() throws Exception {
        givenAccount(true);

        mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"riley@example.org\",\"password\":\"correct horse\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mvc.perform(get("/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
    }

    @Test
    void meReturnsMemberships() throws Exception {
        AppUserDetails details = new AppUserDetails(USER_ID, "riley@example.org", "Riley Recruiter", null, true);

        mvc.perform(get("/v1/auth/me").with(user(details)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Riley Recruiter"))
                .andExpect(jsonPath("$.memberships[0].organizationName").value("Northwind Talent (synthetic)"));
    }

    @Test
    void logoutReturnsNoContent() throws Exception {
        AppUserDetails details = new AppUserDetails(USER_ID, "riley@example.org", "Riley Recruiter", null, true);

        mvc.perform(post("/v1/auth/logout").with(user(details)).with(csrf()))
                .andExpect(status().isNoContent());
    }
}
