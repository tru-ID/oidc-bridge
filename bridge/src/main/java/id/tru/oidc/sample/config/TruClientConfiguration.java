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

@Configuration
public class TruClientConfiguration {

    @Value("${tru.tokenUri:}")
    protected String tokenUri;

    @Value("${tru.clientId:}")
    protected String clientId;

    @Value("${tru.clientSecret:}")
    protected String clientSecret;

    @Bean("truClient")
    public RestTemplate truClient() {
        String registrationId = "truid";
        var client = ClientRegistration.withRegistrationId(registrationId)
                                       .clientId(clientId)
                                       .clientSecret(clientSecret)
                                       .clientAuthenticationMethod(
                                               ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                                       .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                       .scope("phone_check", "oidc", "authenticator")
                                       .tokenUri(tokenUri)
                                       .build();

        var clientRegistrationRepository = new InMemoryClientRegistrationRepository(client);
        var authorizedClientService = new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        var authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);

        var authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
                                                     .principal("none")
                                                     .build();
        var restTemplate = new RestTemplate();
        restTemplate.getInterceptors()
                    .add((request, body, execution) -> {
                        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
                        String token = authorizedClient.getAccessToken()
                                                       .getTokenValue();
                        request.getHeaders()
                               .setBearerAuth(token);
                        return execution.execute(request, body);
                    });

        // to allow PATCH to work
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        return restTemplate;
    }
}
