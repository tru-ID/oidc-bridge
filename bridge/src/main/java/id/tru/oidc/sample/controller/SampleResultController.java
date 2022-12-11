package id.tru.oidc.sample.controller;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import id.tru.oidc.sample.dto.ChallengeResult;
import id.tru.oidc.sample.service.SampleService;
import id.tru.oidc.sample.service.auth0.Auth0User;
import id.tru.oidc.sample.service.context.VerificationContext;
import id.tru.oidc.sample.service.context.VerificationContextRepository;
import id.tru.oidc.sample.service.phonecheck.Check;
import id.tru.oidc.sample.service.phonecheck.PhoneCheckService;

@Controller
@RequestMapping("/bridge")
public class SampleResultController {
    private static final Logger LOG = LoggerFactory.getLogger(SampleResultController.class);

    @Value("${tru.api}")
    private String truApiBaseUrl;

    @Autowired
    private SampleService service;
    @Autowired
    private PhoneCheckService phoneCheckService;
    @Autowired
    private VerificationContextRepository contextRepository;

    @GetMapping(value = "/check/callback")
    public ModelAndView checkCallback(@RequestParam(value = "check_id", required = true) String checkId,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            @RequestParam(value = "error", required = false) String error) {
        LOG.info("check callback: check_id {} code {} error {}", checkId, code, error);
        VerificationContext ctx = contextRepository.findByCheckId(checkId)
                                                   .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE,
                                                           "failed to find context for this checkId"));

        if (code != null) {
            LOG.debug("redeeming check result for checkId={}", checkId);
            Check check = phoneCheckService.fetchCheckResult(checkId, code);

            ctx.setMatch(check.getMatch());
            ctx.setCheckStatus(check.getStatus());
        } else {
            LOG.error("PhoneCheck factor verification failed with checkId={} error={} errorDescription={}",
                    checkId,
                    error,
                    errorDescription);
        }

        ModelAndView mv = null;
        try {
            service.patchToOIDC(ctx);

            if (!ctx.isMobileFlow()) {
                mv = new ModelAndView("callback");
                mv.addObject("message", "Verification completed");
            } else {
                String mobileUrl = UriComponentsBuilder.fromHttpUrl(truApiBaseUrl)
                                                       .path("/oidc/callback")
                                                       .queryParam("flow_id", ctx.getFlowId())
                                                       .encode()
                                                       .build()
                                                       .toString();
                RedirectView redirectView = new RedirectView(mobileUrl, false);
                redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
                mv = new ModelAndView(redirectView);
            }
        } catch (Exception e) {
            LOG.error("failed to patchToOIDC for flowId={}", ctx.getFlowId(), e);
            mv = new ModelAndView("callback");
            mv.addObject("message", "Verification failed");
        } finally {
            if (!(ctx.getUser() instanceof Auth0User)) {
                contextRepository.delete(ctx);
            }
        }

        return mv;
    }

    @GetMapping(value = "/check/callback_push")
    public ResponseEntity<?> checkCallbackPush(@RequestParam(value = "check_id", required = true) String checkId,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            @RequestParam(value = "error", required = false) String error) {
        LOG.info("check callback: check_id {} code {} error {}", checkId, code, error);
        if (checkId != null && code != null) {
            LOG.debug("redeeming check[push] result for checkId={}", checkId);
            Check check = phoneCheckService.fetchCheckResult(checkId, code);
            VerificationContext ctx = contextRepository.findByCheckId(checkId)
                                                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE,
                                                               "failed to find context for this checkId"));
            ctx.setMatch(check.getMatch());
            ctx.setCheckStatus(check.getStatus());
            contextRepository.save(ctx);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                                 .build();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    @PostMapping(value = "/challenge/callback")
    @ResponseBody
    public void challengeCallback(@RequestHeader HttpHeaders headers, @RequestBody ChallengeResult body) {
        LOG.info("challenge callback: {} {}", headers, body);
        if (verifySignature(headers) && body != null) {
            VerificationContext ctx = contextRepository.findByChallengeId(body.getChallengeId())
                                                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE,
                                                               "failed to find context for this challengeId"));

            ctx.setChallengeStatus(body.getStatus());

            try {
                service.patchToOIDC(ctx);
            } catch (Exception e) {
                LOG.error("failed to update challenge status: abandoning challenge with id={}", body.getChallengeId());
            } finally {
                if (!(ctx.getUser() instanceof Auth0User)) {
                    contextRepository.delete(ctx);
                }
            }
        }
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
}
