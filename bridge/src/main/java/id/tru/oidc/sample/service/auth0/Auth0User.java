package id.tru.oidc.sample.service.auth0;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.util.StringUtils;

import id.tru.oidc.sample.service.context.user.IdpUser;

public class Auth0User implements IdpUser {
    private final String userId;
    private final String phoneNumber;

    public static Auth0User ofJwtClaims(Map<String, Object> claims) {
        Objects.requireNonNull(claims, "claims cannot be null");
        String userId = (String) claims.get("sub");
        String phoneNumber = (String) claims.get("phone_number");

        if (!StringUtils.hasLength(userId)) {
            throw new IllegalArgumentException("sub claim must not be blank");
        }
        if (!StringUtils.hasLength(phoneNumber)) {
            throw new IllegalArgumentException("phone_number claim must not be blank");
        }

        return new Auth0User(userId, phoneNumber);
    }

    private Auth0User(String userId, String phoneNumber) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String getId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public Optional<String> getPhoneNumber() {
        return Optional.of(phoneNumber);
    }

}
