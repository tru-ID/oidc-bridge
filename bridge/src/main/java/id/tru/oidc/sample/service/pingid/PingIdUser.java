package id.tru.oidc.sample.service.pingid;

import java.util.Optional;

import id.tru.oidc.sample.service.context.user.IdpUser;

public class PingIdUser implements IdpUser {

    private String username;
    private String phoneNumber;

    public static PingIdUser of(String username, String phoneNumber) {
        var u = new PingIdUser();
        u.username = username;
        u.phoneNumber = phoneNumber;
        return u;
    }

    @Override
    public String getId() {
        return username;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public Optional<String> getPhoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }
    
}
