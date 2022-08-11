package id.tru.oidc.sample.service.gluu;

import com.fasterxml.jackson.annotation.JsonProperty;

class GluuScimUser {
    private String id;
    private String userName;
    private String displayName;
    private boolean active;
    private GluuUserExtension extension;

    @SuppressWarnings("unused")
    public String getId() {
        return id;
    }

    @SuppressWarnings("unused")
    public String getUserName() {
        return userName;
    }

    @SuppressWarnings("unused")
    public String getDisplayName() {
        return displayName;
    }

    @SuppressWarnings("unused")
    public boolean isActive() {
        return active;
    }

    @JsonProperty("urn:ietf:params:scim:schemas:extension:gluu:2.0:User")
    public GluuUserExtension getExtension() {
        return extension;
    }
}
