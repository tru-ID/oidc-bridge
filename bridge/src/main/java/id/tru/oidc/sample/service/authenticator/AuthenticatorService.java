package id.tru.oidc.sample.service.authenticator;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import id.tru.oidc.sample.util.RandomStrings;

@Service
public class AuthenticatorService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticatorService.class);

    private final RestTemplate truClient;
    private final String truApiBaseUrl;

    public AuthenticatorService(@Qualifier("truClient") RestTemplate truClient,
            @Value("${tru.api}") String truApiBaseUrl) {
        this.truClient = truClient;
        this.truApiBaseUrl = truApiBaseUrl;
    }

    public Factor createFactor(String userId, String phoneNumber) {
        String requestUrl = truApiBaseUrl + "/authenticator/v0.1/factors";
        var createRequest = new FactorCreateRequest();
        createRequest.setPhoneNumber(phoneNumber);
        createRequest.setExternalUserId(userId);

        Factor response = null;
        try {
            response = truClient.postForObject(requestUrl, createRequest, Factor.class);
        } catch (RestClientException e) {
            LOG.error("failed to create factor for userId={} and phoneNumber={}", userId, phoneNumber, e);
            throw e;
        }

        return response;
    }

    public Factor enableFactor(Factor factor, String code) {
        String factorId = factor.getFactorId();
        String requestUrl = truApiBaseUrl + "/authenticator/v0.1/factors/" + factorId;
        var updateCode = Map.of("op", "add", "path", "/code", "value", code);
        var operations = List.of(updateCode);

        Factor response = null;
        try {
            var headers = new HttpHeaders();
            headers.add("Content-Type", "application/json-patch+json");
            var requestBody = new HttpEntity<>(operations, headers);

            response = truClient.patchForObject(requestUrl, requestBody, Factor.class);
        } catch (RestClientException e) {
            LOG.error("failed to enable factor factorId={}", factorId, e);
            throw e;
        }
        return response;
    }

    public Factor disableFactor(Factor factor) {
        String factorId = factor.getFactorId();
        String requestUrl = truApiBaseUrl + "/authenticator/v0.1/factors/" + factorId;
        String recycledUserId = "disabled-" + RandomStrings.ofLength(15);
        var updateCode = Map.of("op", "replace", "path", "/external_user_id", "value", recycledUserId);
        var updateStatus = Map.of("op", "replace", "path", "/status", "value", "INACTIVE");
        var operations = List.of(updateCode, updateStatus);

        Factor response = null;
        try {
            var headers = new HttpHeaders();
            headers.add("Content-Type", "application/json-patch+json");
            var requestBody = new HttpEntity<>(operations, headers);

            response = truClient.patchForObject(requestUrl, requestBody, Factor.class);
        } catch (RestClientException e) {
            LOG.error("failed to disable factor factorId={}", factorId, e);
            throw e;
        }
        return response;
    }

    public Optional<Factor> findById(String factorId) {
        String requestUrl = truApiBaseUrl + "/authenticator/v0.1/factors/" + factorId;
        Factor response = null;
        try {
            response = truClient.getForObject(requestUrl, Factor.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }

            LOG.error("failed to find factor with id {}", factorId, e);
            throw e;
        } catch (RestClientException e) {
            LOG.error("failed to find factor with id {}", factorId, e);
            throw e;
        }

        return Optional.ofNullable(response);
    }

    public Collection<Factor> findAll() {
        String requestUrl = truApiBaseUrl + "/authenticator/v0.1/factors";
        FactorSearchResponse response = null;
        try {
            response = truClient.getForObject(requestUrl, FactorSearchResponse.class);
        } catch (RestClientException e) {
            LOG.error("failed to find any factor", e);
            throw e;
        }

        return response.getEmbedded()
                       .getFactors();
    }

    public Collection<Factor> findFactorsByUserId(String userId) {
        String search = "external_user_id==" + userId;
        String requestUrl = UriComponentsBuilder.fromUriString(
                truApiBaseUrl + "/authenticator/v0.1/factors")
                                                .queryParam("search", search)
                                                .build()
                                                .toString();

        FactorSearchResponse response = null;
        try {
            response = truClient.getForObject(requestUrl, FactorSearchResponse.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                if (e.getResponseBodyAsString()
                     .contains("Project has no authenticator config")) {
                    // it's fine if the project doesn't have an authenticator config
                    LOG.warn("no authenticator config found - no factors to try");
                    return Collections.emptyList();
                }
            }
            throw e;
        } catch (RestClientException e) {
            LOG.error("failed to find factors for userId={}", userId, e);
            throw e;
        }

        return response.getEmbedded()
                       .getFactors();
    }

    public String createTotpChallenge(String factorId) {
        String url = truApiBaseUrl + "/authenticator/v0.1/factors/" + factorId + "/challenges";

        ChallengeResponse response = null;
        try {
            ChallengeRequest req = new ChallengeRequest();
            req.setReferenceId(factorId);
            response = truClient.postForObject(url, req, ChallengeResponse.class);
        } catch (RestClientException e) {
            LOG.error("failed to create challenge for factorId={}", factorId, e);
            throw e;
        }

        return response.getChallengeId();
    }

    public String createPushChallenge(String factorId, String checkId, String checkUrl, String message) {
        String defaultMessage = message;
        if (message == null) {
            defaultMessage = "Please confirm your login";
        }

        String url = truApiBaseUrl + "/authenticator/v0.1/factors/" + factorId + "/challenges";

        ChallengeResponse response = null;
        try {
            ChallengeRequest req = new ChallengeRequest();
            req.setReferenceId(factorId);
            req.setCheckId(checkId);
            req.setCheckUrl(checkUrl);
            req.setMessage(defaultMessage);
            response = truClient.postForObject(url, req, ChallengeResponse.class);
        } catch (RestClientException e) {
            LOG.error("failed to create challenge for factorId={}", factorId, e);
            throw e;
        }

        return response.getChallengeId();
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static class FactorCreateRequest {
        private String phoneNumber;
        private String externalUserId;

        @SuppressWarnings("unused")
        public String getExternalUserId() {
            return externalUserId;
        }

        public void setExternalUserId(String externalUserId) {
            this.externalUserId = externalUserId;
        }

        @SuppressWarnings("unused")
        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    private static class FactorSearchResponse {
        private EmbeddedResult embedded;

        @JsonProperty("_embedded")
        public EmbeddedResult getEmbedded() {
            return embedded;
        }
    }

    private static class EmbeddedResult {
        private Collection<Factor> factors;

        public Collection<Factor> getFactors() {
            return factors;
        }
    }

    @JsonInclude(Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static class ChallengeRequest {
        private String referenceId;
        private String checkId;
        private String checkUrl;
        private String message;

        @SuppressWarnings("unused")
        public String getReferenceId() {
            return referenceId;
        }

        public void setReferenceId(String referenceId) {
            this.referenceId = referenceId;
        }

        @SuppressWarnings("unused")
        public String getCheckId() {
            return checkId;
        }

        public void setCheckId(String checkId) {
            this.checkId = checkId;
        }

        @SuppressWarnings("unused")
        public String getCheckUrl() {
            return checkUrl;
        }

        public void setCheckUrl(String checkUrl) {
            this.checkUrl = checkUrl;
        }

        @SuppressWarnings("unused")
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static class ChallengeResponse {
        private String challengeId;

        public String getChallengeId() {
            return challengeId;
        }
    }
}
