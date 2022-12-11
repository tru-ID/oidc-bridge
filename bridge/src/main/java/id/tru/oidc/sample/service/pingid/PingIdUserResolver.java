package id.tru.oidc.sample.service.pingid;

import java.util.Optional;

import id.tru.oidc.sample.service.IdpUserResolver;
import id.tru.oidc.sample.service.context.VerificationContext;
import id.tru.oidc.sample.service.context.user.IdpUser;

public class PingIdUserResolver implements IdpUserResolver {

    @Override
    public Optional<IdpUser> findUserForContext(VerificationContext ctx) {
        // We are assuming the tru.ID connector runs after another factor
        // e.g. knowledge factor a.k.a. user/pass authentication
        //
        // As such, the phone number can be extracted from the user profile
        // and fed to the connector as the login_hint parameter.
        //
        // This avoids having to access whatever user directory the DaVinci
        // instance is using
        String phoneNumber = ctx.getLoginHint();

        return Optional.ofNullable(PingIdUser.of(phoneNumber));
    }

    @Override
    public Optional<IdpUser> findUserById(String userId) {
        // Again, the authenticator external user ID in DaVinci's case will
        // have to be a phone number, as explained above
        //
        // Here we just highlight this for clarity
        String phoneNumber = userId;

        return Optional.ofNullable(PingIdUser.of(phoneNumber));
    }

}
