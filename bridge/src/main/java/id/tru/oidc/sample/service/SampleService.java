package id.tru.oidc.sample.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import id.tru.oidc.sample.service.context.SampleContext;
import id.tru.oidc.sample.service.okta.OktaUser;

@Service
public class SampleService {
    private static final Logger LOG = LoggerFactory.getLogger(SampleService.class);

    private final OidcService oidcService;

    public SampleService(OidcService oidcService) {
        this.oidcService = oidcService;
    }

    public void patchToOIDC(SampleContext ctx) {
        LOG.info("patching context {}", ctx);

        boolean verificationSuccessful = false;
        switch (ctx.getVerificationType()) {
        case PHONECHECK:
            // looking at the match field is enough
            verificationSuccessful = ctx.getMatch();
            break;
        case PUSH:
            // here it's mix of both
            verificationSuccessful = ctx.getMatch() && "VERIFIED".equals(ctx.getChallengeStatus());
            break;
        case TOTP:
            // looking at the challenge status is enought
            verificationSuccessful = "VERIFIED".equals(ctx.getChallengeStatus());
            break;
        default:
            LOG.error("could not determine the verification result: unknown verification type {}",
                    ctx.getVerificationType());
            break;
        }

        if (verificationSuccessful) {
            LOG.info("resolving login flow flowId={} {}", ctx.getFlowId(), ctx);
            String userId = ctx.getUser()
                               .getUsername();
            Map<String, Object> userinfo = new HashMap<>();
            // we can add more claims here but the "sub" is required
            // these claims will be included in the ID token
            userinfo.put("sub", userId);
            userinfo.put("phone_number_verified", true);

            if (ctx.getUser() instanceof OktaUser) {
                // workaround to make Okta's MFA enrollment through custom IDP work, see:
                // https://help.okta.com/en-us/Content/Topics/Security/MFA_Custom_Factor.htm
                //
                // "For the OIDC response, the preferred_username claim is mapped to the Okta
                // username."
                userinfo.put("preferred_username", ctx.getUser()
                                                      .getUsername());
            }

            try {
                oidcService.resolveFlow(ctx.getFlowId(), userinfo);
            } catch (RestClientException e) {
                LOG.error("failed to resolve the login flow match={} flowId={}", ctx.getMatch(), ctx.getFlowId(), e);
                throw new RuntimeException(e);
            }
        } else {
            LOG.warn("failing login flow flowId={} {}", ctx.getFlowId(), ctx);
            try {
                oidcService.rejectFlow(ctx.getFlowId());
            } catch (RestClientException e) {
                LOG.error("failed to reject the login flow match={} flowId={}", ctx.getMatch(), ctx.getFlowId(), e);
                throw new RuntimeException(e);
            }
        }
    }
}
