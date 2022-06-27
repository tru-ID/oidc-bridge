package id.tru.oidc.sample.service.context;

import id.tru.oidc.sample.service.context.user.IdpUser;

public class SampleContext {
    private String checkId;
    private String checkUrl;
    private String checkStatus;
    private boolean match;
    private String phoneNumber;
    private String flowId;
    private String flowCallbackUrl;
    private String state;
    private String challengeId;
    private String challengeStatus;
    private boolean mobileFlow;

    private VerificationType verificationType;

    private String loginHint;

    private IdpUser user;

    public static SampleContext ofFlow(String flowId, String flowPatchUrl, String state) {
        var ctx = new SampleContext();
        ctx.setFlowId(flowId);
        ctx.setFlowCallbackUrl(flowPatchUrl);
        ctx.setState(state);
        return ctx;
    }

    public static SampleContext ofLoginHint(String loginHint) {
        var ctx = new SampleContext();
        ctx.setLoginHint(loginHint);
        return ctx;
    }

    SampleContext() {
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public String getCheckUrl() {
        return checkUrl;
    }

    public void setCheckUrl(String checkUrl) {
        this.checkUrl = checkUrl;
    }

    public String getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(String status) {
        this.checkStatus = status;
    }

    public boolean getMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getFlowCallbackUrl() {
        return flowCallbackUrl;
    }

    public void setFlowCallbackUrl(String flowCallbackUrl) {
        this.flowCallbackUrl = flowCallbackUrl;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getChallengeStatus() {
        return challengeStatus;
    }

    public void setChallengeStatus(String challengeStatus) {
        this.challengeStatus = challengeStatus;
    }

    public IdpUser getUser() {
        return user;
    }

    public void setUser(IdpUser user) {
        this.user = user;
    }

    public void setMobileFlow(boolean mobileFlow) {
        this.mobileFlow = mobileFlow;
    }

    public boolean isMobileFlow() {
        return mobileFlow;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public void setVerificationType(VerificationType verificationType) {
        this.verificationType = verificationType;
    }

    public VerificationType getVerificationType() {
        return verificationType;
    }

    @Override
    public String toString() {
        return "SampleContext[flowId=" + flowId
                + ", loginHint=" + loginHint
                + ", state=" + state
                + ", phoneNumber=" + phoneNumber
                + ", mobileFlow=" + mobileFlow
                + ", verificationType=" + verificationType
                + ", checkId=" + checkId
                + ", checkStatus=" + checkStatus
                + ", challengeId=" + challengeId
                + ", challengeStatus=" + challengeStatus
                + ", match=" + match
                + "]";
    }

}