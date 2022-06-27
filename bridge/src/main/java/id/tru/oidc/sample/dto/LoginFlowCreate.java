package id.tru.oidc.sample.dto;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LoginFlowCreate {
    @NotBlank
    private String loginHint;
    @NotBlank
    private String flowId;
    @NotBlank
    private String flowPatchUrl;

    private String state;

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

    public String getFlowPatchUrl() {
        return flowPatchUrl;
    }

    void setFlowPatchUrl(String flowPatchUrl) {
        this.flowPatchUrl = flowPatchUrl;
    }

    public String getState() {
        return state;
    }

    void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "LoginFlowCreate [flowId=" + flowId + ", flowPatchUrl=" + flowPatchUrl + ", loginHint=" + loginHint
                + "state=" + state + "]";
    }

}
