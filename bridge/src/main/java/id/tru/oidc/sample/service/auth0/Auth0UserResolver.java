package id.tru.oidc.sample.service.auth0;

import java.net.URI;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import id.tru.oidc.sample.service.IdpUserResolver;
import id.tru.oidc.sample.service.context.SampleContext;
import id.tru.oidc.sample.service.context.user.IdpUser;

public class Auth0UserResolver implements IdpUserResolver {

    private final String baseUri;
    private final RestTemplate client;

    public Auth0UserResolver(String auth0Domain, RestTemplate auth0ApiClient) {
        this.baseUri = "https://" + auth0Domain;
        this.client = auth0ApiClient;
    }

    @Override
    public Optional<IdpUser> findUserForContext(SampleContext ctx) {
        if (ctx.getUser() == null) {
            // auth0 users should've been resolved by the action code
            // and stored in the existing context
            // if this is not the case, then it's likely the login flow
            // did not start through the action
            throw new IllegalStateException(
                    "unresolved Auth0 user - login flow could have not started through the action");
        }
        return Optional.of(ctx.getUser());
    }

    @Override
    public Optional<IdpUser> findUserById(String userId) {
        URI uri = UriComponentsBuilder.fromUriString(baseUri)
                                      .pathSegment("api", "v2", "users", "{id}")
                                      .build(userId);
        Auth0ApiUser apiUser = null;
        try {
            apiUser = client.getForObject(uri, Auth0ApiUser.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }

        return Optional.of(Auth0User.ofApiUser(apiUser));
    }

}
