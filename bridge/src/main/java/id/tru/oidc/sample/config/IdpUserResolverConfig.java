package id.tru.oidc.sample.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.client.RestTemplate;

import id.tru.oidc.sample.service.IdpUserResolver;
import id.tru.oidc.sample.service.auth0.Auth0UserResolver;
import id.tru.oidc.sample.service.okta.OktaUserResolver;

@Configuration
public class IdpUserResolverConfig {

    @Value("${sample.resolver.type:}")
    private ResolverType resolverType;

    // Okta
    @Value("${sample.okta.apiKey:}")
    private String oktaApiKey;
    @Value("${sample.okta.domain:}")
    private String oktaDomain;

    // Auth0
    @Value("${sample.auth0.testToken:}")
    private String auth0TestToken;
    @Value("${sample.auth0.domain:}")
    private String auth0Domain;

    private RestTemplate oktaApiClient() {
        var restTemplate = new RestTemplate();
        restTemplate.getInterceptors()
                    .add((request, body, execution) -> {
                        request.getHeaders()
                               .set("Authorization", "SSWS " + oktaApiKey);
                        return execution.execute(request, body);
                    });
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        return restTemplate;
    }

    private RestTemplate auth0ApiClient() {
        var restTemplate = new RestTemplate();
        restTemplate.getInterceptors()
                    .add((request, body, execution) -> {
                        request.getHeaders()
                               .set("Authorization", "Bearer " + auth0TestToken);
                        return execution.execute(request, body);
                    });
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        return restTemplate;
    }

    @Bean
    IdpUserResolver idpUserResolver() {
        switch (resolverType) {
        case AUTH0:
            return new Auth0UserResolver(auth0Domain, auth0ApiClient());
        case OKTA:
            return new OktaUserResolver(oktaDomain, oktaApiClient());
        default:
            throw new IllegalStateException("unknown resolver type: " + resolverType);
        }
    }

    enum ResolverType {
        AUTH0, OKTA
    }
}
