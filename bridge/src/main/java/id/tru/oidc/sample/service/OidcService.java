package id.tru.oidc.sample.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class OidcService {

    private static final Logger LOG = LoggerFactory.getLogger(OidcService.class);

    private final RestTemplate truClient;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;

    public OidcService(@Qualifier("truClient") RestTemplate truClient,
            @Value("${tru.api}") String apiBaseUrl,
            ObjectMapper objectMapper) {
        this.truClient = truClient;
        this.objectMapper = objectMapper;
        this.apiBaseUrl = apiBaseUrl;
    }

    public void updateFlowForCheck(String flowId, String checkUrl, String issuerName) {
        var updateCheck = Map.of("op", "add", "path", "/check_url", "value", checkUrl);
        var updateIssuer = Map.of("op", "add", "path", "/issuer_name", "value", issuerName);
        var operations = List.of(updateCheck, updateIssuer);

        String patchBody = null;
        try {
            patchBody = objectMapper.writeValueAsString(operations);
        } catch (JsonProcessingException e) {
            LOG.error("failed to update flowId={}", flowId, e);
            throw new RuntimeException(e);
        }

        LOG.info("updating flowId={} with checkUrl={}", flowId, checkUrl);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json-patch+json");
            HttpEntity<String> request = new HttpEntity<>(patchBody, headers);

            String url = apiBaseUrl + "/oidc/v0.1/flows/" + flowId;

            truClient.patchForObject(url, request, Void.class);
        } catch (RestClientException ex) {
            LOG.error("failed update flowId={}", flowId, ex);
            throw ex;
        }
    }

    public void updateFlowForChallenge(String flowId, String challengeId) {
        var updateChallenge = Map.of("op", "add", "path", "/challenge_id", "value", challengeId);
        var operations = new ArrayList<Map<String, ?>>();
        operations.add(updateChallenge);

        String patchBody = null;
        try {
            patchBody = objectMapper.writeValueAsString(operations);
        } catch (JsonProcessingException e) {
            LOG.error("failed to update flowId={}", flowId, e);
            throw new RuntimeException(e);
        }

        LOG.info("updating flowId={} with challengeId={}", flowId, challengeId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json-patch+json");
            HttpEntity<String> request = new HttpEntity<>(patchBody, headers);

            String url = apiBaseUrl + "/oidc/v0.1/flows/" + flowId;

            truClient.patchForObject(url, request, Void.class);
        } catch (RestClientException ex) {
            LOG.error("failed update flowId={}", flowId, ex);
            throw ex;
        }
    }

    public void resolveFlow(String flowId, Map<String, Object> userinfo) {
        completeFlow(flowId, userinfo);
    }

    public void rejectFlow(String flowId) {
        completeFlow(flowId, Map.of());
    }

    private void completeFlow(String flowId, Map<String, Object> userinfo) {
        var updateChallenge = Map.of("op", "add", "path", "/userinfo", "value", userinfo);
        var operations = new ArrayList<Map<String, ?>>();
        operations.add(updateChallenge);

        String patchBody = null;
        try {
            patchBody = objectMapper.writeValueAsString(operations);
        } catch (JsonProcessingException e) {
            LOG.error("failed to complete flowId={}", flowId, e);
            throw new RuntimeException(e);
        }

        LOG.info("completing flowId={} sucessfully={}", flowId, !userinfo.isEmpty());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json-patch+json");
            HttpEntity<String> request = new HttpEntity<>(patchBody, headers);

            String url = apiBaseUrl + "/oidc/v0.1/flows/" + flowId;

            truClient.patchForObject(url, request, Void.class);
        } catch (RestClientException ex) {
            LOG.error("failed to complete flowId={}", flowId, ex);
            throw ex;
        }
    }
}
