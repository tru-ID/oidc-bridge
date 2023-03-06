package id.tru.sampleui;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableWebSecurity(debug = true)
public class SecurityConfig {

    @Value("${tru.id.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${tru.id.iam.logout-success-url}")
    private String logoutSuccessUrl;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .mvcMatchers(HttpMethod.GET, "/sample-ui", "/sample-ui/login", "/sample-ui/login/**",
                    "/sample-ui/assets/**", "/sample-ui/error", "/sample-ui/logout", "/actuator/**")
            .permitAll()
            .anyRequest()
            .authenticated();

        http.exceptionHandling()
            .accessDeniedPage("/sample-ui/error");

        http.logout()
            .invalidateHttpSession(true)
            .logoutUrl("/sample-ui/logout")
            .logoutSuccessUrl(logoutSuccessUrl);

        http.oauth2Login(oauth2 -> oauth2.loginPage("/sample-ui/login/iam")
                                         .defaultSuccessUrl("/sample-ui")
                                         .authorizationEndpoint()
                                         .baseUri("/sample-ui/login")
                                         .and()
                                         .redirectionEndpoint()
                                         .baseUri("/sample-ui/login/callback/*")
                                         .and()
                                         .failureUrl("/sample-ui/error"));
        http.cors()
            .configurationSource(corsConfigurationSource());

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration().applyPermitDefaultValues();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/sample-ui/**", configuration);
        return source;
    }
}