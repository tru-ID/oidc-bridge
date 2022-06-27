package id.tru.oidc.sample.config;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleConfig {

    @Bean
    HttpClient httpClient() {
        // expose a client for everyone
        return HttpClient.newHttpClient();
    }
}
