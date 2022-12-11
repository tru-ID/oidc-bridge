package id.tru.oidc.sample.service;

import java.util.Optional;

import id.tru.oidc.sample.service.context.VerificationContext;
import id.tru.oidc.sample.service.context.user.IdpUser;

public interface IdpUserResolver {
    Optional<IdpUser> findUserForContext(VerificationContext ctx);

    Optional<IdpUser> findUserById(String userId);
}
