package id.tru.oidc.sample.service.okta;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import id.tru.oidc.sample.service.IdpUserResolver;
import id.tru.oidc.sample.service.context.SampleContext;
import id.tru.oidc.sample.service.context.user.IdpUser;

public class OktaUserResolver implements IdpUserResolver {
    private static final Logger LOG = LoggerFactory.getLogger(OktaUserResolver.class);

    private final String oktaDomain;
    private final RestTemplate oktaApiClient;

    public OktaUserResolver(String oktaDomain, RestTemplate oktaApiClient) {
        this.oktaDomain = oktaDomain;
        this.oktaApiClient = oktaApiClient;
    }

    @Override
    public Optional<IdpUser> findUserForContext(SampleContext ctx) {
        String loginHint = ctx.getLoginHint();

        if (!StringUtils.hasLength(loginHint)) {
            throw new IllegalStateException("cannot resolve okta user - loginHint is blank");
        }
        // login hint in the okta case should be the user's email
        return findUser(loginHint);
    }

    private Optional<IdpUser> findUser(String email) {
        String baseUri = "https://" + oktaDomain + "/api/v1/users";
        String filter = "profile.email eq \"" + email + "\"";

        String requestUri = UriComponentsBuilder.fromUriString(baseUri)
                .queryParam("filter", filter)
                .build()
                .toString();

        OktaUserResult[] results = null;
        try {
            ResponseEntity<OktaUserResult[]> response = oktaApiClient.exchange(requestUri, HttpMethod.GET, null,
                    OktaUserResult[].class);
            results = response.getBody();
        } catch (RestClientException e) {
            LOG.error("failed to find Okta user for email={}", email, e);
            return Optional.empty();
        }

        if (results == null || results.length == 0) {
            LOG.warn("no Okta user found for email={}", email);
            return Optional.empty();
        }

        if (results.length > 1) {
            LOG.error("too many results found for email={} found {} result", email, results.length);
            throw new RuntimeException("too many results found for email=" + email);
        }

        LOG.debug("Found user id={} login={}", results[0].getId(), results[0].getProfile()
                .getLogin());
        return Optional.ofNullable(results[0])
                .map(result -> toUser(email, result));
    }

    private static OktaUser toUser(String email, OktaUserResult result) {
        if (result.getProfile() == null) {
            throw new IllegalStateException("failed to obtain profile for user with email=" + email);
        }

        OktaUser user = result.getProfile();

        if (result.getId() == null) {
            throw new IllegalStateException("failed to get id for user with email=" + email);
        }
        user.setId(result.getId());

        return user;
    }

    private static class OktaUserResult {
        private String id;
        private String status;
        private OktaUser profile;

        public String getId() {
            return id;
        }

        @SuppressWarnings("unused")
        public String getStatus() {
            return status;
        }

        public OktaUser getProfile() {
            return profile;
        }
    }
}
