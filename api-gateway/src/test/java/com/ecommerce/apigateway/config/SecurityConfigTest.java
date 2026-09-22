package com.ecommerce.apigateway.config;

import com.ecommerce.apigateway.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest
@Import(SecurityConfig.class)
public class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    @BeforeEach
    void setUp() {

        when(jwtAuthenticationFilter.filter(any(), any()))
                .thenAnswer(invocation -> {

                    var exchange = invocation.getArgument(0);
                    var chain = invocation.getArgument(1);

                    return ((org.springframework.web.server.WebFilterChain) chain)
                            .filter(
                                    (org.springframework.web.server.ServerWebExchange) exchange
                            );
                });
    }

    @Test
    void login_shouldBePublic() {

        webTestClient.post()
                .uri("/auth/login")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void register_shouldBePublic() {

        webTestClient.post()
                .uri("/auth/register")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void users_withoutAuthentication_shouldReturnUnauthorized() {

        webTestClient.get()
                .uri("/users/profile")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    @WithMockUser(roles = "USER")
    void users_withUserRole_shouldPassSecurity() {

        webTestClient.get()
                .uri("/users/profile")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    @WithMockUser(roles = "USER")
    void manager_withUserRole_shouldReturnForbidden() {

        webTestClient.get()
                .uri("/manager/test")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void manager_withManagerRole_shouldPassSecurity() {

        webTestClient.get()
                .uri("/manager/test")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void admin_withManagerRole_shouldReturnForbidden() {

        webTestClient.get()
                .uri("/admin/testadmin")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_withAdminRole_shouldPassSecurity() {

        webTestClient.get()
                .uri("/admin/testadmin")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    @WithMockUser(roles = "USER")
    void roles_withUserRole_shouldReturnForbidden() {

        webTestClient.get()
                .uri("/roles")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void roles_withAdminRole_shouldPassSecurity() {

        webTestClient.get()
                .uri("/roles")
                .exchange()
                .expectStatus()
                .isNotFound();
    }
}
