package id.tru.oidc.sample.service.pingid;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import id.tru.oidc.sample.service.IdpUserResolver;
import id.tru.oidc.sample.service.context.SampleContext;
import id.tru.oidc.sample.service.context.user.IdpUser;

public class PingIdUserResolver implements IdpUserResolver {

    private Map<String, PingIdUser> testUsers;

    public PingIdUserResolver() {
        this.testUsers = new HashMap<>();

        String testEmail = "test@example.com";
        String testPhone = "447580505540";
        testUsers.put(testEmail, PingIdUser.of(testEmail, testPhone));
    }

    @Override
    public Optional<IdpUser> findUserForContext(SampleContext ctx) {
        String loginHint = ctx.getLoginHint();

        // FIXME this is just to test
        return Optional.ofNullable(testUsers.get(loginHint));
    }

    @Override
    public Optional<IdpUser> findUserById(String userId) {
        // FIXME this is just to test
        return Optional.ofNullable(testUsers.get(userId));
    }

}
