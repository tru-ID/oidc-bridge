package id.tru.oidc.sample.service.gluu;

import java.util.Optional;

import id.tru.oidc.sample.service.context.user.IdpUser;

public class GluuUser implements IdpUser {

    private String id;
    private String username;
    private String displayName;
    private String phoneNumber;

    public static GluuUser ofScimUser(GluuScimUser scimUser) {
        String phoneNumber = scimUser.getExtension()
                                     .getTelephoneNumber();
        return new GluuUser(scimUser.getId(), scimUser.getUserName(), scimUser.getDisplayName(), phoneNumber);
    }

    private GluuUser(String id, String username, String displayName, String phoneNumber) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Optional<String> getPhoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    @Override
    public String toString() {
        return "GluuUser[id=" + id
                + ", username=" + username
                + ", displayName=" + displayName
                + ", phoneNumber=" + phoneNumber
                + "]";
    }
}
