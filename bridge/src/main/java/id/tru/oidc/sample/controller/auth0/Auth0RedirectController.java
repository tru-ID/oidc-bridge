package id.tru.oidc.sample.controller.auth0;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import id.tru.oidc.sample.service.auth0.Auth0User;
import id.tru.oidc.sample.service.context.SampleContext;
import id.tru.oidc.sample.service.context.SampleContextRepository;

@RequestMapping("/auth0")
@Controller
public class Auth0RedirectController {
    private static final Logger LOG = LoggerFactory.getLogger(Auth0RedirectController.class);

    private final ObjectMapper objectMapper;
    private final HttpClient client;
    private final SampleContextRepository contextRepository;

    private final String servicePublicBaseUrl;
    private final String truApiBaseUrl;
    private final String oidcClientId;
    private final String oidcClientSecret;
    private final String auth0Domain;
    private final String auth0SigSecret;

    public Auth0RedirectController(
            ObjectMapper objectMapper,
            HttpClient client,
            SampleContextRepository contextRepository,
            @Value("${sample.url}") String servicePublicBaseUrl,
            @Value("${tru.api}") String truApiBaseUrl,
            @Value("${tru.oidc.clientId}") String oidcClientId,
            @Value("${tru.oidc.clientSecret}") String oidcClientSecret,
            @Value("${sample.auth0.domain}") String auth0Domain,
            @Value("${sample.auth0.sig.secret}") String auth0SigSecret) {
        this.objectMapper = objectMapper;
        this.client = client;
        this.contextRepository = contextRepository;
        this.servicePublicBaseUrl = servicePublicBaseUrl;
        this.truApiBaseUrl = truApiBaseUrl;
        this.oidcClientId = oidcClientId;
        this.oidcClientSecret = oidcClientSecret;
        this.auth0Domain = auth0Domain;
        this.auth0SigSecret = auth0SigSecret;
    }

    @GetMapping("/action/redirect")
    public ModelAndView handleActionRedirect(@RequestParam("state") String state,
            @RequestParam("session_token") String jwtToken) {
        Map<String, Object> payload = decodeJwt(jwtToken);

        var user = Auth0User.ofJwtClaims(payload);

        var ctx = SampleContext.ofLoginHint(user.getId());
        ctx.setUser(user);
        ctx.setState(state);

        contextRepository.save(ctx);

        var baseAuthUri = URI.create(truApiBaseUrl + "/oauth2/v1/auth");

        String authUri = UriComponentsBuilder.fromUri(baseAuthUri)
                                             .queryParam("client_id", oidcClientId)
                                             .queryParam("scope", "openid profile")
                                             .queryParam("redirect_uri", servicePublicBaseUrl + "/auth0/oidc/redirect")
                                             .queryParam("response_type", "code")
                                             .queryParam("login_hint", ctx.getUser()
                                                                          .getId())
                                             .queryParam("state", state)
                                             .encode()
                                             .build()
                                             .toString();

        return new ModelAndView("redirect:" + authUri);
    }

    @GetMapping("/oidc/redirect")
    public ModelAndView handleOidcRedirect(CallbackParams params) {
        String code = params.getCode();
        if (code != null) {
            return handleCode(code, params.getState());
        }
        return handleError(params);
    }

    private ModelAndView handleCode(String code, String state) {
        SampleContext ctx = contextRepository.findByState(state)
                                             .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE,
                                                     "failed to find context for this state"));

        URI tokenUri = URI.create(truApiBaseUrl + "/oauth2/v1/token");

        String formBody = UriComponentsBuilder.fromUriString("")
                                              .queryParam("code", code)
                                              .queryParam("grant_type", "authorization_code")
                                              .queryParam("redirect_uri", servicePublicBaseUrl + "/auth0/oidc/redirect")
                                              .queryParam("client_id", oidcClientId)
                                              .queryParam("client_secret", oidcClientSecret)
                                              .encode()
                                              .build()
                                              .toString()
                                              // hack: remove heading "?"
                                              .substring(1);
        var request = HttpRequest.newBuilder(tokenUri)
                                 .header("Content-Type", "application/x-www-form-urlencoded")
                                 .POST(BodyPublishers.ofString(formBody))
                                 .build();

        // Tokens tokens = null;
        boolean match = false;
        try {
            HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
            if (response.statusCode() > 299) {
                try (var is = response.body()) {
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    LOG.error("failed to exchange code flowId={} userId={} status={} body={}", ctx.getFlowId(),
                            ctx.getUser()
                               .getId(),
                            response.statusCode(), body);
                    // return newErrorView("exchange code status=" + response.statusCode() + "
                    // body=" + body, state);
                }
            } else {
                objectMapper.readValue(response.body(), Tokens.class);
                match = true;
            }
        } catch (IOException e) {
            LOG.error("failed to exchange code flowId={} userId={}", ctx.getFlowId(), ctx.getUser()
                                                                                         .getId(),
                    e);
        } catch (InterruptedException e) {
            LOG.error("operation was cancelled flowId={} userId={}", ctx.getFlowId(), ctx.getUser()
                                                                                         .getId(),
                    e);
            Thread.currentThread()
                  .interrupt();
        } finally {
            contextRepository.delete(ctx);
        }

        String auth0Redirect = "https://" + auth0Domain + "/continue";
        String jwtToken = encodeJwt(ctx.getUser()
                                       .getId(),
                state,
                Map.of("match", match));

        String redirectUri = UriComponentsBuilder.fromUriString(auth0Redirect)
                                                 .queryParam("state", state)
                                                 .queryParam("truid_token", jwtToken)
                                                 .encode()
                                                 .build()
                                                 .toString();
        return new ModelAndView("redirect:" + redirectUri);
    }

    private Map<String, Object> decodeJwt(String jwtToken) {
        try {
            var signedJwt = SignedJWT.parse(jwtToken);
            var verifier = new MACVerifier(auth0SigSecret.getBytes());

            if (!signedJwt.verify(verifier)) {
                throw new RuntimeException("jwtToken signature does not match");
            }

            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();

            if (!new Date().before(claims.getExpirationTime())) {
                throw new RuntimeException("jwtToken is expired");
            }

            return Map.of("sub", claims.getSubject(), "phone_number", claims.getClaim("phone_number"));
        } catch (JOSEException | ParseException e) {
            LOG.error("failed to decode jwt", e);
            throw new RuntimeException(e);
        }
    }

    private String encodeJwt(String subject, String state, Map<String, Object> payload) {
        String issuer = URI.create(servicePublicBaseUrl)
                           .getHost();

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Date issueTime = Date.from(now.toInstant(ZoneOffset.UTC));
        Date expirationTime = Date.from(now.plusSeconds(60)
                                           .toInstant(ZoneOffset.UTC));

        var claimsBuilder = new JWTClaimsSet.Builder().subject(subject)
                                                      .issuer(issuer)
                                                      .issueTime(issueTime)
                                                      .expirationTime(expirationTime);

        // auth0 needs state to be a claim in the JWT
        claimsBuilder.claim("state", state);

        for (var entry : payload.entrySet()) {
            claimsBuilder.claim(entry.getKey(), entry.getValue());
        }

        var header = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT)
                                                              .build();
        var jwt = new SignedJWT(header, claimsBuilder.build());

        try {
            var signer = new MACSigner(auth0SigSecret.getBytes());
            jwt.sign(signer);
        } catch (JOSEException e) {
            LOG.error("failed to sign JWT for subject={}", subject, e);
            throw new RuntimeException(e);
        }

        return jwt.serialize();
    }

    private ModelAndView handleError(CallbackParams params) {
        SampleContext ctx = contextRepository.findByState(params.getState())
                                             .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE,
                                                     "failed to find context for this state"));
        String error = params.getError();
        String errorDescription = params.getErrorDescription();

        LOG.error("login failed error={} errorDescription={} flowId={} userId={}", error, errorDescription,
                ctx.getFlowId(), ctx.getUser()
                                    .getId());

        return newErrorView(error, params.getState());
    }

    private ModelAndView newErrorView(String reason, String state) {
        String auth0Redirect = "https://" + auth0Domain + "/continue";
        String redirectUri = UriComponentsBuilder.fromUriString(auth0Redirect)
                                                 .queryParam("state", state)
                                                 .encode()
                                                 .build()
                                                 .toString();
        return new ModelAndView("redirect:" + redirectUri);
    }

    static class CallbackParams {
        private String code;
        private String error;
        private String errorDescription;
        private String state;

        CallbackParams(String code, String error, String error_description, String state) {
            this.code = code;
            this.error = error;
            this.errorDescription = error_description;
            this.state = state;
        }

        public String getCode() {
            return code;
        }

        public String getError() {
            return error;
        }

        public String getErrorDescription() {
            return errorDescription;
        }

        public String getState() {
            return state;
        }
    }

    @JsonInclude(Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class Tokens {
        private String accessToken;
        private String idToken;

        public String getAccessToken() {
            return accessToken;
        }

        public String getIdToken() {
            return idToken;
        }
    }
}
