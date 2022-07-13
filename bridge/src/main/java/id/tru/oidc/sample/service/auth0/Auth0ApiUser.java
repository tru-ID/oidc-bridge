package id.tru.oidc.sample.service.auth0;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class Auth0ApiUser {
    private String userId;
    private String email;
    private String username;
    private Map<String, Object> appMetadata;

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public Map<String, Object> getAppMetadata() {
        return appMetadata;
    }

    @JsonIgnore
    public String getPhoneNumber() {
        // this is how we are actually storing phone numbers in auth0 since some
        // providers do not have the actual openid "phone_number" attribute populated
        //
        // we assume the number is provisioned through other means and stored in the
        // "app_metadata" field
        return (String) appMetadata.get("phone_number");
    }
}
