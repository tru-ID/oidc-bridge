package id.tru.sampleui.controller;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@RequestMapping("/sample-ui/authenticator")
@Controller
public class AuthenticatorController {
    private static Logger LOG = LoggerFactory.getLogger(AuthenticatorController.class);

    @Value("${tru.id.bridge.api.base-url}")
    private String bridgeApiBaseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/onboard")
    ModelAndView authenticatorOnboard(@AuthenticationPrincipal OAuth2User oAuth2User) {
        String userId = oAuth2User.getName();

        URI uri = UriComponentsBuilder.fromUriString(bridgeApiBaseUrl)
                                      .pathSegment("user", "{id}", "factors")
                                      .build(userId);

        RegistrationIntent ri = restTemplate.postForObject(uri, null, RegistrationIntent.class);

        LOG.info("Onboarding authenticator for user {} with phone number {}", ri.userId, ri.phoneNumber);

        ModelAndView mv = new ModelAndView("authenticator-onboard");
        mv.addObject("registration", ri);
        return mv;
    }

    @PostMapping("/onboard/submit")
    String authenticatorValidate(@AuthenticationPrincipal OAuth2User oAuth2User, TotpSubmitForm totpSubmitForm) {
        String userId = oAuth2User.getName();

        URI uri = UriComponentsBuilder.fromUriString(bridgeApiBaseUrl)
                                      .pathSegment("user", "{userId}", "factors", "{factorId}", "enable")
                                      .build(userId, totpSubmitForm.factorId);

        var body = Map.of("code", totpSubmitForm.getCode());

        LOG.info("Submitting TOTP code {} to register factor {} for user {}", totpSubmitForm.getCode(),
                totpSubmitForm.getFactorId(), userId);

        restTemplate.postForObject(uri, body, Void.class);

        return "redirect:/sample-ui/profile";
    }

    @PostMapping("/remove")
    String authenticatorRemove(@AuthenticationPrincipal OAuth2User oAuth2User) {
        String userId = oAuth2User.getName();

        URI uri = UriComponentsBuilder.fromUriString(bridgeApiBaseUrl)
                                      .pathSegment("user", "{userId}", "factors", "disable")
                                      .build(userId);

        LOG.info("Disabling authenticator for user {}", userId);

        restTemplate.postForObject(uri, null, Void.class);

        return "redirect:/sample-ui/profile";
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class RegistrationIntent {
        private String factorId;
        private String userId;
        private String phoneNumber;
        private String dataUrl;

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

    private static class TotpSubmitForm {
        private String code;
        private String factorId;

        @SuppressWarnings("unused")
        public TotpSubmitForm(String code, String factor_id) {
            this.code = code;
            this.factorId = factor_id;
        }

        public String getCode() {
            return code;
        }

        public String getFactorId() {
            return factorId;
        }
    }
}
