package id.tru.oidc.sample.controller;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import id.tru.oidc.sample.service.IdpUserResolver;
import id.tru.oidc.sample.service.authenticator.AuthenticatorService;
import id.tru.oidc.sample.service.authenticator.Factor;
import id.tru.oidc.sample.service.context.user.IdpUser;

@RequestMapping("/bridge/api")
@RestController
public class ApiController {

    private static Logger LOG = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private IdpUserResolver userResolver;

    @Autowired
    private AuthenticatorService authenticatorService;

    @GetMapping("/user/{id}/factors")
    public List<PublicFactor> allFactors(@PathVariable("id") String userId) {
        return authenticatorService.findFactorsByUserId(userId)
                                   .stream()
                                   .map(PublicFactor::ofFactor)
                                   .collect(Collectors.toList());
    }

    @PostMapping("/user/{id}/factors")
    public RegistrationIntent createFactor(@PathVariable("id") String userId) {
        String phoneNumber = userResolver.findUserById(userId)
                                         .flatMap(IdpUser::getPhoneNumber)
                                         .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                 "could resolve phone number for user with id=" + userId));

        Factor factor = authenticatorService.createFactor(userId, phoneNumber);

        return RegistrationIntent.of(factor, phoneNumber);
    }

    @PostMapping("/user/{id}/factors/{factorId}/enable")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void enableFactor(@PathVariable("factorId") String factorId, @RequestBody EnableFactor enableFactor) {
        Factor factor = authenticatorService.findById(factorId)
                                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                    "could not find factor with id=" + factorId));

        LOG.info("Enabling factor {} with code {}", factor, enableFactor.getCode());
        authenticatorService.enableFactor(factor, enableFactor.getCode());
    }

    @PostMapping("/user/{id}/factors/disable")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void disableFactors(@PathVariable("id") String userId) {
        Collection<Factor> activeFactors = authenticatorService.findFactorsByUserId(userId);

        for (Factor f : activeFactors) {
            LOG.info("Disabling factor {} for user {}", f, userId);
            authenticatorService.disableFactor(f);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class PublicFactor {
        String factorId;
        String type;
        String status;

        static PublicFactor ofFactor(Factor factor) {
            var pf = new PublicFactor();
            pf.factorId = factor.getFactorId();
            pf.type = factor.getType();
            pf.status = factor.getStatus();

            return pf;
        }

        public String getFactorId() {
            return factorId;
        }

        public String getType() {
            return type;
        }

        public String getStatus() {
            return status;
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class RegistrationIntent {
        private String factorId;
        private String userId;
        private String phoneNumber;
        private String dataUrl;

        static RegistrationIntent of(Factor factor, String phoneNumber) {
            var ri = new RegistrationIntent();

            ri.factorId = factor.getFactorId();
            ri.userId = factor.getExternalUserId();
            ri.phoneNumber = phoneNumber;
            ri.dataUrl = factor.getDataUrl();

            return ri;
        }

        public String getFactorId() {
            return factorId;
        }

        public String getUserId() {
            return userId;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getDataUrl() {
            return dataUrl;
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class EnableFactor {
        private String code;

        public String getCode() {
            return code;
        }
    }
}
