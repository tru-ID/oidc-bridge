package id.tru.oidc.sample.service.phonecheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class PhoneCheckService {
    private static final Logger LOG = LoggerFactory.getLogger(PhoneCheckService.class);

    private RestTemplate truClient;
    private String phoneCheckUrl;
    private String authenticatorCallback;
    private String sampleBaseUrl;
    private ObjectMapper objectMapper;

    public PhoneCheckService(@Qualifier("truClient") RestTemplate truClient,
            @Value("${tru.phoneCheck}") String phoneCheckUrl,
            @Value("${tru.authenticatorCallback}") String authenticatorCallback,
            @Value("${sample.url}") String sampleBaseUrl,
            ObjectMapper objectMapper) {
        this.truClient = truClient;
        this.phoneCheckUrl = phoneCheckUrl;
        this.authenticatorCallback = authenticatorCallback;
        this.sampleBaseUrl = sampleBaseUrl;
        this.objectMapper = objectMapper;
    }

    public Check createCheck(String phoneNumber, String reference) {
        CheckCreate cc = new CheckCreate(phoneNumber);
        cc.setRedirectUrl(sampleBaseUrl + "/bridge/check/callback");
        cc.setReferenceId(reference);

        LOG.debug("creating check for {}", cc);

        Check check = null;
        try {
            check = truClient.postForObject(phoneCheckUrl, cc, Check.class);
            check.setCheckUrl(phoneCheckUrl + "/" + check.getCheckId() + "/redirect");
        } catch (RestClientException e) {
            LOG.error("failed to create check for phoneNumber={} with reference={}", phoneNumber, reference, e);
            throw e;
        }
        return check;
    }

    public Check createCheckForPush(String phoneNumber, String reference) {
        CheckCreate cc = new CheckCreate(phoneNumber);
        cc.setCallbackUrl(authenticatorCallback);
        cc.setRedirectUrl(sampleBaseUrl + "/bridge/check/callback_push");
        cc.setReferenceId(reference);

        LOG.debug("creating check[push] for {}", cc);

        Check check = null;
        try {
            check = truClient.postForObject(phoneCheckUrl, cc, Check.class);
            check.setCheckUrl(phoneCheckUrl + "/" + check.getCheckId() + "/redirect");
        } catch (RestClientException e) {
            LOG.error("failed to create check[push] for phoneNumber={} with reference={}", phoneNumber, reference, e);
            throw e;
        }
        return check;
    }

    public Check fetchCheckResult(String checkId, String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json-patch+json");

        Map<String, Object> operation = new HashMap<>();
        operation.put("op", "add");
        operation.put("path", "/code");
        operation.put("value", code);

        String patchBody = null;
        try {
            patchBody = objectMapper.writeValueAsString(List.of(operation));
        } catch (JsonProcessingException e) {
            LOG.error("patchCheck: failed to create JSON patch body", e);
            throw new RuntimeException(e);
        }

        try {
            HttpEntity<String> request = new HttpEntity<String>(patchBody, headers);
            ResponseEntity<Check> response = truClient.exchange(phoneCheckUrl + "/" + checkId,
                    HttpMethod.PATCH,
                    request,
                    Check.class);
            if (!response.getStatusCode()
                         .is2xxSuccessful()) {
                LOG.error("failed to get check result for checkId={}: status={}", checkId, response.getStatusCode());
                throw new RuntimeException("failed to patch phonecheck result");
            }
            return response.getBody();
        } catch (RestClientException e) {
            LOG.error("failed to get check result for checkId={}", checkId, e);
            throw e;
        }
    }

    @JsonInclude(Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static class CheckCreate {
        private String phoneNumber;
        private String callbackUrl;
        private String redirectUrl;
        private String referenceId;

        public CheckCreate(String phone) {
            if (phone != null) {
                this.phoneNumber = phone.replaceAll("\\s+", "");
            }
        }

        @SuppressWarnings("unused")
        public String getCallbackUrl() {
            return callbackUrl;
        }

        public void setCallbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
        }

        @SuppressWarnings("unused")
        public String getPhoneNumber() {
            return phoneNumber;
        }

        @SuppressWarnings("unused")
        public String getReferenceId() {
            return referenceId;
        }

        public void setReferenceId(String referenceId) {
            this.referenceId = referenceId;
        }

        @SuppressWarnings("unused")
        public String getRedirectUrl() {
            return redirectUrl;
        }

        public void setRedirectUrl(String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }

        @Override
        public String toString() {
            return "CheckCreate[phoneNumber=" + phoneNumber + ", callbackUrl=" + callbackUrl + ", redirectUrl="
                    + redirectUrl + "]";
        }
    }
}
