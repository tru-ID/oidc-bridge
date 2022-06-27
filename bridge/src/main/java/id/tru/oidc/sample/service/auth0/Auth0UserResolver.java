package id.tru.oidc.sample.service.auth0;

import java.util.Optional;

import id.tru.oidc.sample.service.IdpUserResolver;
import id.tru.oidc.sample.service.context.SampleContext;
import id.tru.oidc.sample.service.context.user.IdpUser;

public class Auth0UserResolver implements IdpUserResolver {

    @Override
    public Optional<IdpUser> findUserForContext(SampleContext ctx) {
        if (ctx.getUser() == null) {
            // auth0 users should've been resolved by the action code
            // and stored in the existing context
            // if this is not the case, then it's likely the login flow
            // did not start through the action
            throw new IllegalStateException(
                    "unresolved Auth0 user - login flow could have not started through the action");
        }
        return Optional.of(ctx.getUser());
    }

}
