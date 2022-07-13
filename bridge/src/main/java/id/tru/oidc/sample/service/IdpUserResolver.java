package id.tru.oidc.sample.service;

import java.util.Optional;

import id.tru.oidc.sample.service.context.SampleContext;
import id.tru.oidc.sample.service.context.user.IdpUser;

public interface IdpUserResolver {
    Optional<IdpUser> findUserForContext(SampleContext ctx);

    Optional<IdpUser> findUserById(String userId);
}
