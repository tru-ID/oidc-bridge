package id.tru.oidc.sample.service.gluu;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

import id.tru.oidc.sample.service.IdpUserResolver;
import id.tru.oidc.sample.service.context.VerificationContext;
import id.tru.oidc.sample.service.context.user.IdpUser;

public class GluuUserResolver implements IdpUserResolver {
    private static final Logger LOG = LoggerFactory.getLogger(GluuUserResolver.class);

    private final String gluuBaseUrl;
    private final RestTemplate gluuClient;

    public GluuUserResolver(String gluuBaseUrl, RestTemplate gluuClient) {
        this.gluuBaseUrl = gluuBaseUrl;
        this.gluuClient = gluuClient;
    }

    @Override
    public Optional<IdpUser> findUserById(String userId) {
        String baseUri = gluuBaseUrl + "/identity/restv1/scim/v2/Users/{id}";

        String requestUri = UriComponentsBuilder.fromUriString(baseUri)
                                                .build(userId)
                                                .toString();

        GluuScimUser user;
        try {
            ResponseEntity<GluuScimUser> response = gluuClient.exchange(requestUri, HttpMethod.GET, null,
                    GluuScimUser.class);
            user = response.getBody();
        } catch (RestClientException e) {
            LOG.error("failed to find Gluu user for user_id={}", userId, e);
            return Optional.empty();
        }

        return Optional.of(GluuUser.ofScimUser(user));
    }

    @Override
    public Optional<IdpUser> findUserForContext(VerificationContext ctx) {
        // in Gluu's case, the login hint will be the username, as it comes from the py
        // script installed as a person authentication script
        String username = ctx.getLoginHint();

        return findUser(username);
    }

    private Optional<IdpUser> findUser(String username) {
        String baseUri = gluuBaseUrl + "/identity/restv1/scim/v2/Users";
        String filter = "userName eq \"" + username + "\"";

        String requestUri = UriComponentsBuilder.fromUriString(baseUri)
                                                .queryParam("filter", filter)
                                                .build()
                                                .toString();

        GluuScimPage results;
        try {
            ResponseEntity<GluuScimPage> response = gluuClient.exchange(requestUri, HttpMethod.GET, null,
                    GluuScimPage.class);
            results = response.getBody();
        } catch (RestClientException e) {
            LOG.error("failed to find Gluu user for username={}", username, e);
            return Optional.empty();
        }

        if (results.getTotalResults() == 0) {
            return Optional.empty();
        }

        if (results.getTotalResults() > 1) {
            LOG.error("too many results found for username={} found {} result", username, results.getTotalResults());
            throw new RuntimeException("too many results found for username=" + username);
        }

        return results.getResources()
                      .stream()
                      .limit(1)
                      .findAny()
                      .map(GluuUser::ofScimUser);
    }

    private static class GluuScimPage {
        private int totalResults;

        @JsonProperty("Resources")
        private Collection<GluuScimUser> resources;

        public Collection<GluuScimUser> getResources() {
            return resources;
        }

        @SuppressWarnings("unused")
        public int getTotalResults() {
            return totalResults;
        }
    }
}
