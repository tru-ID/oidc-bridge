package id.tru.oidc.sample.service.context;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import id.tru.oidc.sample.service.context.user.IdpUser;

@RedisHash(value = "verificationContexts", timeToLive = 360L) // 6 min TTL
public class VerificationContext {
    @Id
    private String contextId;

    @Indexed
    private String loginHint;
    @Indexed
    private String state;
    @Indexed
    private String flowId;

    private boolean mobileFlow;

    private IdpUser user;
    private VerificationType verificationType;
    @Indexed
    private String checkId;
    @Indexed
    private String challengeId;

    private boolean match;
    private String checkStatus;
    private String challengeStatus;

    public static VerificationContext ofLoginHint(String loginHint) {
        var contextId = UUID.randomUUID()
                            .toString();
        return new VerificationContext(contextId, loginHint, null, null, false, null, null, null, null, false, null,
                null);
    }

    public static VerificationContext ofFlow(String flowId, String loginHint, String state) {
        var contextId = UUID.randomUUID()
                            .toString();
        return new VerificationContext(contextId, loginHint, state, flowId, false, null, null, null, null, false,
                null,
                null);
    }

    // used by spring data redis to avoid reflecting on setters
    VerificationContext(String contextId, String loginHint, String state, String flowId, boolean mobileFlow,
            IdpUser user, VerificationType verificationType, String checkId, String challengeId, boolean match,
            String checkStatus, String challengeStatus) {
        this.contextId = contextId;
        this.loginHint = loginHint;
        this.state = state;
        this.flowId = flowId;
        this.mobileFlow = mobileFlow;
        this.user = user;
        this.verificationType = verificationType;
        this.checkId = checkId;
        this.challengeId = challengeId;
        this.match = match;
        this.checkStatus = checkStatus;
        this.challengeStatus = challengeStatus;
    }

    public String getContextId() {
        return contextId;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public boolean isMobileFlow() {
        return mobileFlow;
    }

    public void setMobileFlow(boolean mobileFlow) {
        this.mobileFlow = mobileFlow;
    }

    public IdpUser getUser() {
        return user;
    }

    public void setUser(IdpUser user) {
        this.user = user;
    }

    public VerificationType getVerificationType() {
        return verificationType;
    }

    public void setVerificationType(VerificationType verificationType) {
        this.verificationType = verificationType;
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public String getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(String checkStatus) {
        this.checkStatus = checkStatus;
    }

    public String getChallengeStatus() {
        return challengeStatus;
    }

    public void setChallengeStatus(String challengeStatus) {
        this.challengeStatus = challengeStatus;
    }

    @Override
    public String toString() {
        return "VerificationContext[contextId=" + contextId
                + ", flowId=" + flowId
                + ", loginHint=" + loginHint
                + ", verificationType=" + verificationType + "]";
    }
}
