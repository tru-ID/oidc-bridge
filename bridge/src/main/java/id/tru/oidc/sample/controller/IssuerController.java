package id.tru.oidc.sample.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bridge")
public class IssuerController {

    @GetMapping("/.well-known/openid-configuration")
    public Map<String, Object> openIDConfiguration() {
        var config = new HashMap<String, Object>();

        config.put("issuer", "https://eu-dev.api.tru.qa/");
        config.put("authorization_endpoint", "https://eu-dev.api.tru.qa/oauth2/v1/auth");
        config.put("token_endpoint", "https://eu-dev.api.tru.qa/oauth2/v1/token");
        config.put("jwks_uri", "https://eu-dev.api.tru.qa/oidc/.well-known/jwks.json");
        config.put("subject_types_supported", List.of("public"));
        config.put("response_types_supported",
                List.of("code", "code id_token", "id_token", "token id_token", "token", "token id_token code"));
        config.put("claims_supported", List.of("sub"));
        config.put("grant_types_supported", List.of("authorization_code"));
        config.put("response_modes_supported", List.of("query", "fragment"));
        config.put("scopes_supported", List.of("openid", "profile"));
        config.put("token_endpoint_auth_methods_supported", List.of("client_secret_post", "client_secret_basic"));
        config.put("id_token_signing_alg_values_supported", List.of("RS256"));

        return config;
    }
}
