package id.tru.oidc.sample.controller;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import id.tru.oidc.sample.dto.LoginFlowCreate;
import id.tru.oidc.sample.service.IdpUserResolver;
import id.tru.oidc.sample.service.OidcService;
import id.tru.oidc.sample.service.authenticator.AuthenticatorService;
import id.tru.oidc.sample.service.authenticator.Factor;
import id.tru.oidc.sample.service.context.SampleContext;
import id.tru.oidc.sample.service.context.SampleContextRepository;
import id.tru.oidc.sample.service.context.VerificationType;
import id.tru.oidc.sample.service.context.user.IdpUser;
import id.tru.oidc.sample.service.phonecheck.Check;
import id.tru.oidc.sample.service.phonecheck.PhoneCheckService;

@Controller
@RequestMapping("/bridge")
public class MobileAwareController {
    private static final Logger LOG = LoggerFactory.getLogger(MobileAwareController.class);

    @Value("${tru.oidc.qrCodeUrl}")
    private String qrCodeUrl;
    @Value("${tru.oidc.totpUrl}")
    private String totpUrl;
    @Value("${tru.oidc.pushUrl}")
    private String pushUrl;
    @Value("${sample.url}")
    private String samplePublicBaseUrl;
    @Autowired
    private SampleContextRepository contextRepository;
    @Autowired
    private IdpUserResolver idpUserResolver;
    @Autowired
    private AuthenticatorService authenticatorService;
    @Autowired
    private PhoneCheckService phoneCheckService;
    @Autowired
    private OidcService oidcService;

    @PostMapping("/oidc-login")
    public ModelAndView handleOidcLogin(
            @RequestHeader HttpHeaders headers,
            @Valid @RequestBody LoginFlowCreate uc) {
        if (!verifySignature(headers)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Restricted endpoint");
        }

        String loginHint = uc.getLoginHint();
        String flowId = uc.getFlowId();
        String flowPatchUrl = uc.getFlowPatchUrl();
        String state = uc.getState();

        // context with a login_hint might've been created before (e.g. auth0 action)
        // otherwise create brand new context
        SampleContext ctx = contextRepository.findByLoginHint(loginHint)
                                             .map(c -> {
                                                 c.setFlowId(flowId);
                                                 c.setFlowCallbackUrl(flowPatchUrl);
                                                 c.setState(state);
                                                 return c;
                                             })
                                             .orElseGet(() -> {
                                                 var c = SampleContext.ofFlow(flowId, flowPatchUrl, state);
                                                 c.setLoginHint(loginHint);
                                                 return c;
                                             });

        contextRepository.save(ctx);

        ModelAndView mav = new ModelAndView("redirect:" + samplePublicBaseUrl + "/bridge/oidc-login/oidc-login-check");
        mav.addObject("flow_id", ctx.getFlowId());
        return mav;
    }

    @GetMapping("/oidc-login-check")
    public ModelAndView mobileCheck(@RequestParam("flow_id") String flowId) {
        ModelAndView mav = new ModelAndView("mobile-check");
        mav.addObject("flow_id", flowId);
        return mav;
    }

    @PostMapping("/oidc-login-check")
    public ResponseEntity<?> handleMobileCheck(MobileCheckForm form) {
        SampleContext ctx = contextRepository.findByFlowId(form.getFlowId())
                                             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                     "login flow not found"));

        // let us know if this is a mobile flow so we can handle it appropriately
        ctx.setMobileFlow(form.isMobileFlow());

        // Step 1:
        // resolve login_hint to an IAM User or phone number
        IdpUser user = idpUserResolver.findUserForContext(ctx)
                                      .orElse(null);
        if (user == null) {
            LOG.warn("failed to resolve user for flowId={} loginHint={}", ctx.getFlowId(), ctx.getLoginHint());
            oidcService.rejectFlow(ctx.getFlowId());

            // reject flow by sending to any oidcUrl
            String redirectUrl = UriComponentsBuilder.fromUriString(qrCodeUrl)
                                                     .queryParam("flow_id", ctx.getFlowId())
                                                     .encode()
                                                     .build()
                                                     .toString();

            LOG.info("redirecting to url={} ctx={}", redirectUrl, ctx);
            return ResponseEntity.status(HttpStatus.FOUND)
                                 .header("Location", redirectUrl)
                                 .build();
        }

        ctx.setUser(user);

        String phoneNumber = user.getPhoneNumber()
                                 .orElseThrow();
        ctx.setPhoneNumber(phoneNumber);

        LOG.info("found user id={} with phone={}", user.getId(), phoneNumber);

        // Step 2:
        // figure out which verification factor we should use for this user
        Collection<Factor> factors = authenticatorService.findFactorsByUserId(user.getId());

        // consider PUSH more important than TOTP
        Comparator<Factor> byImportance = Comparator.comparingInt(this::getFactorImportance);
        Factor factor = factors.stream()
                               .filter(f -> "ACTIVE".equals(f.getStatus()))
                               .sorted(byImportance)
                               .findAny()
                               .orElse(null); // handled below

        // Step 3:
        // handle the verification factors
        if (factor != null) {
            return handleAuthenticatorFactor(factor, ctx);
        }

        // if there's no verification factor for this user i.e. they don't have an
        // authenticator application, then do a simple PhoneCheck instead
        return handlePhoneCheckFactor(ctx);
    }

    private ResponseEntity<?> handleAuthenticatorFactor(Factor factor, SampleContext ctx) {
        if ("TOTP".equals(factor.getType())) {
            LOG.info("handling TOTP factor factorId={} for userId={}", factor.getFactorId(), ctx.getUser()
                                                                                                .getId());
            try {
                String challengeId = authenticatorService.createTotpChallenge(factor.getFactorId());
                ctx.setChallengeId(challengeId);
                ctx.setVerificationType(VerificationType.TOTP);

                oidcService.updateFlowForChallenge(ctx.getFlowId(), ctx.getChallengeId());
            } catch (Exception e) {
                LOG.error("failed to handle TOTP login flow for flowId={} factorId={} userId={}", ctx.getFlowId(),
                        factor.getFactorId(), ctx.getUser()
                                                 .getId(),
                        e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Error");
            }

            contextRepository.save(ctx);
            String redirectUrl = UriComponentsBuilder.fromHttpUrl(totpUrl)
                                                     .queryParam("flow_id", ctx.getFlowId())
                                                     .encode()
                                                     .build()
                                                     .toString();

            LOG.info("redirecting TOTP url={} ctx={}", redirectUrl, ctx);
            return ResponseEntity.status(HttpStatus.FOUND)
                                 .header("Location", redirectUrl)
                                 .build();
        } else if ("PUSH".equals(factor.getType())) {
            LOG.info("handling PUSH factor factorId={} for user_id={}", factor.getFactorId(), ctx.getUser()
                                                                                                 .getId());
            try {
                Check check = phoneCheckService.createCheckForPush(ctx.getPhoneNumber(), ctx.getFlowId());
                ctx.setCheckId(check.getCheckId());
                ctx.setCheckUrl(check.getCheckUrl());

                String challengeId = authenticatorService.createPushChallenge(factor.getFactorId(), check.getCheckId(),
                        check.getCheckUrl(),
                        "Please confirm you auth0 login");
                ctx.setChallengeId(challengeId);
                ctx.setVerificationType(VerificationType.PUSH);

                oidcService.updateFlowForChallenge(ctx.getFlowId(), ctx.getChallengeId());
            } catch (Exception e) {
                LOG.error("failed to handle PUSH login flow for flowId={} factorId={} userId={}", ctx.getFlowId(),
                        factor.getFactorId(), ctx.getUser()
                                                 .getId(),
                        e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Error");
            }

            contextRepository.save(ctx);
            String redirectUrl = UriComponentsBuilder.fromHttpUrl(pushUrl)
                                                     .queryParam("flow_id", ctx.getFlowId())
                                                     .encode()
                                                     .build()
                                                     .toString();
            LOG.info("redirecting PUSH url={} ctx={}", redirectUrl, ctx);
            return ResponseEntity.status(HttpStatus.FOUND)
                                 .header("Location", redirectUrl)
                                 .build();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported verification factor");
        }
    }

    private ResponseEntity<?> handlePhoneCheckFactor(SampleContext ctx) {
        try {
            Check check = phoneCheckService.createCheck(ctx.getPhoneNumber(), ctx.getFlowId());
            ctx.setCheckId(check.getCheckId());
            ctx.setCheckUrl(check.getCheckUrl());
            ctx.setVerificationType(VerificationType.PHONECHECK);

            oidcService.updateFlowForCheck(ctx.getFlowId(), ctx.getCheckUrl());
        } catch (Exception e) {
            LOG.error("failed to handle WEB login flow for flowId={} userId={}", ctx.getFlowId(), ctx.getUser()
                                                                                                     .getId(),
                    e);
            // FIXME this should fail the flow i.e. PATCH path=/user value=null
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Error");
        }

        contextRepository.save(ctx);
        // redirect to the OIDC QR code endpoint with the phone check url
        // the end-user can then scan this QR code on the browser, in order to verify
        // their phone number
        String redirectUrl = UriComponentsBuilder.fromUriString(qrCodeUrl)
                                                 .queryParam("flow_id", ctx.getFlowId())
                                                 .encode()
                                                 .build()
                                                 .toString();

        LOG.info("redirecting to url={} ctx={}", redirectUrl, ctx);
        return ResponseEntity.status(HttpStatus.FOUND)
                             .header("Location", redirectUrl)
                             .build();
    }

    // Since this endpoint is public it's strongly recommended to verify if a
    // request has been sent by the tru.ID platform,
    // and hasn't been tampered with, by validating a signature received in the
    // request.
    // https://developer.tru.id/docs/reference/authentication#verifying-the-signature
    // and alternate solution will be to limit this endpoint to tru.ID IP ranges
    private boolean verifySignature(HttpHeaders headers) {
        String digest = headers.getFirst("digest");
        // TODO verify digest
        return (digest != null);
    }

    private int getFactorImportance(Factor f) {
        var importanceTable = Map.of("PUSH", 1, "TOTP", 2);
        return importanceTable.getOrDefault(f.getType(), Integer.MAX_VALUE);
    }

    private static class MobileCheckForm {
        private String flowId;
        private boolean mobileFlow;

        // used by spring to bind the form
        @SuppressWarnings("unused")
        public MobileCheckForm(String flow_id, boolean mobile_flow) {
            this.flowId = flow_id;
            this.mobileFlow = mobile_flow;
        }

        public String getFlowId() {
            return flowId;
        }

        public boolean isMobileFlow() {
            return mobileFlow;
        }
    }
}
