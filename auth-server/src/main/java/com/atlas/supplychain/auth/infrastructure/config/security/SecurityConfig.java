package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.infrastructure.common.exceptions.ErrorResponseWriter;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.JwtAuthenticationFilter;
import com.atlas.supplychain.auth.infrastructure.config.security.tenant.TenantBoundaryFilter;
import com.atlas.supplychain.auth.infrastructure.config.security.tenant.TenantMismatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TenantBoundaryFilter tenantBoundaryFilter;
    private final ErrorResponseWriter errorResponseWriter;
    private final CorsProperties corsProperties;

    @Value("${atlas.openapi.public-access:false}")
    private boolean openApiPublicAccess;

    @Value("${springdoc.api-docs.enabled:true}")
    private boolean openApiDocsEnabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            if (authException instanceof LockedException) {
                errorResponseWriter.write(response, HttpStatus.LOCKED, ErrorResponseWriter.LOCKED_MESSAGE, authException);
            } else {
                errorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "No autorizado", authException);
            }
        };
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            String message = accessDeniedException instanceof TenantMismatchException
                    ? accessDeniedException.getMessage()
                    : "Acceso denegado. No tienes permisos para realizar esta acción.";
            errorResponseWriter.write(response, HttpStatus.FORBIDDEN, message, accessDeniedException);
        };
    }

    /**
     * Si springdoc está deshabilitado (perfil prod) no hay handler detrás de los matchers de
     * OpenAPI -- exigir autenticación ahí devolvería 401 antes de que la request llegue al
     * DispatcherServlet, ocultando el 404 real. Por eso también se consideran públicos.
     */
    private boolean openApiMatchersDebenSerPublicos() {
        return openApiPublicAccess || !openApiDocsEnabled;
    }

    /**
     * La autorización granular (Spec 1.7) vive en @PreAuthorize sobre cada método de
     * controller, con autoridades en formato MODULO:OPERACION (ej. "USUARIOS:CREAR"),
     * resueltas en cada request contra la BD -- no en estos matchers, que solo exigen
     * autenticación. Ver AuthorityName y User.permisosEfectivos().
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     AuthenticationEntryPoint restAuthenticationEntryPoint,
                                                     AccessDeniedHandler restAccessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                    CorsConfigurationSource source = request -> {
                        CorsConfiguration config = new CorsConfiguration();
                        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
                        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
                        config.setAllowCredentials(true);
                        return config;
                    };
                    cors.configurationSource(source);
                })
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .headers(h -> h
                        .contentTypeOptions(cto -> {
                        })
                        .frameOptions(fo -> fo.deny())
                )
                .authorizeHttpRequests(auth -> {
                    var openApiMatchers = auth.requestMatchers("/v3/api-docs", "/v3/api-docs/**",
                            "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/**");

                    auth.requestMatchers("/auth/login", "/auth/refresh", "/auth/password/forgot", "/auth/password/reset").permitAll()
                            // Spec 6.1: no puede exigir JWT -- es el endpoint que le da a un
                            // servicio sin JWT propio (ej. un job batch) su primer JWT de
                            // servicio. Autenticación real vía X-Service-Secret dentro del
                            // controller (ServiceTokenController), no acá.
                            .requestMatchers("/service-tokens").permitAll()
                            .requestMatchers("/.well-known/jwks.json").permitAll()
                            .requestMatchers("/auth/change-password", "/auth/logout").authenticated()
                            .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                            .requestMatchers("/actuator/info").authenticated();

                    if (openApiMatchersDebenSerPublicos()) {
                        openApiMatchers.permitAll();
                    } else {
                        openApiMatchers.authenticated();
                    }
                    auth.anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantBoundaryFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

}
