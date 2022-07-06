package id.tru.oidc.sample.dto;

import java.util.Map;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LoginFlowCreate {
    @NotBlank
    private String loginHint;
    @NotBlank
    private String flowId;

    private String state;

    @JsonProperty("_links")
    private Map<String, Object> links;

    public String getLoginHint() {
        return loginHint;
    }

    void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getFlowId() {
        return flowId;
    }

    void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    void setLinks(Map<String, Object> links) {
        this.links = links;
    }

    public String getFlowPatchUrl() {
        return (String) ((Map<String, Object>) links.get("self")).get("href");
    }

    public String getQrCodeDelegationUrl() {
        return (String) ((Map<String, Object>) links.get("qr_code_delegation_url")).get("href");
    }

    public String getTOTPDelegationUrl() {
        return (String) ((Map<String, Object>) links.get("totp_delegation_url")).get("href");
    }

    public String getPushDelegationUrl() {
        return (String) ((Map<String, Object>) links.get("push_delegation_url")).get("href");
    }

    public String getState() {
        return state;
    }

    void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "LoginFlowCreate [flowId=" + flowId + ", loginHint=" + loginHint + "state=" + state + "]";
    }

}
