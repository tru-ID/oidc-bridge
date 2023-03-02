package id.tru.oidc.sample.service.pingid;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import id.tru.oidc.sample.service.IdpUserResolver;
import id.tru.oidc.sample.service.context.VerificationContext;
import id.tru.oidc.sample.service.context.user.IdpUser;

public class PingOneUserResolver implements IdpUserResolver {
    private static final Logger LOG = LoggerFactory.getLogger(PingOneUserResolver.class);

    private final String apiBaseUrl;
    private final String environmentId;
    private final RestTemplate apiClient;

    public PingOneUserResolver(String apiBaseUrl, String environmentId, RestTemplate apiClient) {
        this.apiBaseUrl = apiBaseUrl;
        this.environmentId = environmentId;
        this.apiClient = apiClient;
    }

    @Override
    public Optional<IdpUser> findUserForContext(VerificationContext ctx) {
        String username = ctx.getLoginHint();
        return findByUsername(username);
    }

    @Override
    public Optional<IdpUser> findUserById(String userId) {
        String requestUri = UriComponentsBuilder.fromUriString(apiBaseUrl)
                                                .pathSegment("v1", "environments", "{environmentId}", "users",
                                                        "{userId}")
                                                .build(environmentId, userId)
                                                .toString();

        PingOneApiUser apiUser;
        try {
            ResponseEntity<PingOneApiUser> response = apiClient.exchange(requestUri, HttpMethod.GET, null,
                    PingOneApiUser.class);
            apiUser = response.getBody();
        } catch (RestClientException e) {
            LOG.error("failed to find PingOne user for userId={}", userId, e);
            return Optional.empty();
        }

        return Optional.of(toIdpUser(apiUser));
    }

    private Optional<IdpUser> findByUsername(String username) {
        String filter = "username eq \"" + username + "\"";

        // for some reason the management API doesn't like URL encoded strings (?), thus
        // we aren't using the URLComponentsBuilder
        String requestUri = apiBaseUrl + "/v1/environments/" + environmentId + "/users?filter=" + filter;

        PingOneUsersResponse usersResponse = null;
        try {
            ResponseEntity<PingOneUsersResponse> response = apiClient.exchange(requestUri, HttpMethod.GET, null,
                    PingOneUsersResponse.class);
            usersResponse = response.getBody();
        } catch (RestClientException e) {
            LOG.error("failed to find PingOne user for username={}", username, e);
            return Optional.empty();
        }

        if (usersResponse.getSize() == 0) {
            LOG.warn("no PingOne user found for username={}", username);
            return Optional.empty();
        }

        if (usersResponse.getSize() > 1) {
            LOG.error("too many results found for username={} found {} results", username, usersResponse.getSize());
            throw new RuntimeException("too many results found for username=" + username);
        }

        return usersResponse.getEmbedded()
                            .getUsers()
                            .stream()
                            .limit(1)
                            .findAny()
                            .map(PingOneUserResolver::toIdpUser);
    }

    private static PingOneUser toIdpUser(PingOneApiUser apiUser) {
        return new PingOneUser(apiUser.getId(), apiUser.getUsername(), apiUser.getMobilePhone());
    }

    private static class PingOneUsersResponse {
        private PingOneUsers _embedded;
        private long size;

        public PingOneUsers getEmbedded() {
            return _embedded;
        }

        @SuppressWarnings("unused")
        void set_embedded(PingOneUsers _embedded) {
            this._embedded = _embedded;
        }

        public long getSize() {
            return size;
        }

        @SuppressWarnings("unused")
        void setSize(long size) {
            this.size = size;
        }
    }

    private static class PingOneUsers {
        private Collection<PingOneApiUser> users;

        public Collection<PingOneApiUser> getUsers() {
            return users;
        }
    }

    private static class PingOneApiUser {
        private String id;
        private String username;
        private String mobilePhone;

        public String getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getMobilePhone() {
            return mobilePhone;
        }
    }
}
