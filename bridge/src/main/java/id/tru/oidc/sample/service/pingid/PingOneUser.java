package id.tru.oidc.sample.service.pingid;

import java.util.Optional;

import id.tru.oidc.sample.service.context.user.IdpUser;

public class PingOneUser implements IdpUser {

    private String id;
    private String username;
    private String phoneNumber;

    PingOneUser(String id, String username, String phoneNumber) {
        this.id = id;
        this.username = username;
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Optional<String> getPhoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }
}
