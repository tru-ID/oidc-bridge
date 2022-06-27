package id.tru.oidc.sample.service.context.user;

import java.util.Optional;

public interface IdpUser {
    String getId();

    String getUsername();

    Optional<String> getPhoneNumber();
}
