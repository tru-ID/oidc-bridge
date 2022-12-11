package id.tru.oidc.sample.service.context;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationContextRepository extends CrudRepository<VerificationContext, String> {

    Optional<VerificationContext> findByLoginHint(String loginHint);

    Optional<VerificationContext> findByFlowId(String flowId);

    Optional<VerificationContext> findByCheckId(String checkId);

    Optional<VerificationContext> findByChallengeId(String checkId);

    Optional<VerificationContext> findByState(String state);
}
