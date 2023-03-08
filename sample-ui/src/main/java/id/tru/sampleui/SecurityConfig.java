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

@EnableWebSecurity()
public class SecurityConfig {

    @Value("${sample-ui.url}")
    private String sampleUiPublicBaseUrl;

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

        // because some of these urls will be called in a 302, we want to make sure they
        // are both servlet context aware and hostname aware
        //
        // without the 'withBaseUrl' method, they'll only be context aware and some
        // redirects might fail (similarly to spring MVC 'redirect:<url>' return
        // requiring the full hostname)
        http.oauth2Login(oauth2 -> oauth2.loginPage(withBaseUrl("/sample-ui/login/iam"))
                                         .defaultSuccessUrl(withBaseUrl("/sample-ui"))
                                         .authorizationEndpoint()
                                         .baseUri(withBaseUrl("/sample-ui/login"))
                                         .and()
                                         .redirectionEndpoint()
                                         .baseUri(withBaseUrl("/sample-ui/login/callback/*"))
                                         .and()
                                         .failureUrl(withBaseUrl("/sample-ui/error")));
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

    private String withBaseUrl(String relativeUrl) {
        return sampleUiPublicBaseUrl + relativeUrl;
    }
}