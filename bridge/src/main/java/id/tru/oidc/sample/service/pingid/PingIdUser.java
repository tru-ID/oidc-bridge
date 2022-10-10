package id.tru.oidc.sample.service.pingid;

import java.util.Optional;

import id.tru.oidc.sample.service.context.user.IdpUser;

public class PingIdUser implements IdpUser {

    private String phoneNumber;

    public static PingIdUser of(String phoneNumber) {
        var u = new PingIdUser();
        u.phoneNumber = phoneNumber;
        return u;
    }

    @Override
    public String getId() {
        return phoneNumber;
    }

    @Override
    public String getUsername() {
        return phoneNumber;
    }

    @Override
    public Optional<String> getPhoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

}
