package id.tru.oidc.sample.service.authenticator;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Factor {
    private String factorId;
    private String status;
    private String type;
    private String externalUserId;
    private String url;
    private String dataUrl;

    public String getFactorId() {
        return factorId;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getUrl() {
        return url;
    }

    public String getDataUrl() {
        return dataUrl;
    }
}
