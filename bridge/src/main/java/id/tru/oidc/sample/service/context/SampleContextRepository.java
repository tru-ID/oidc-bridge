package id.tru.oidc.sample.service.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SampleContextRepository {

    private final Map<String, SampleContext> contextByCheck;
    private final Map<String, SampleContext> contextByChallenge;
    private final Map<String, SampleContext> contextByLoginHint;
    private final Map<String, SampleContext> contextByFlowId;
    private final Map<String, SampleContext> contextByState;
    private final ReadWriteLock rwLock;

    public SampleContextRepository() {
        this.contextByChallenge = new HashMap<>();
        this.contextByCheck = new HashMap<>();
        this.contextByLoginHint = new HashMap<>();
        this.contextByFlowId = new HashMap<>();
        this.contextByState = new HashMap<>();
        this.rwLock = new ReentrantReadWriteLock();
    }

    public SampleContext save(SampleContext context) {
        Lock writeLock = rwLock.writeLock();
        writeLock.lock();
        try {
            if (context.getLoginHint() != null) {
                contextByLoginHint.put(context.getLoginHint(), context);
            }
            if (context.getCheckId() != null) {
                contextByCheck.put(context.getCheckId(), context);
            }
            if (context.getChallengeId() != null) {
                contextByChallenge.put(context.getChallengeId(), context);
            }
            if (context.getFlowId() != null) {
                contextByFlowId.put(context.getFlowId(), context);
            }
            if (context.getState() != null) {
                contextByState.put(context.getState(), context);
            }
        } finally {
            writeLock.unlock();
        }
        return context;
    }

    public SampleContext saveUniqueState(SampleContext context) {
        Lock writeLock = rwLock.writeLock();
        writeLock.lock();
        try {
            String state = context.getState();
            if (!StringUtils.hasLength(state)) {
                throw new IllegalStateException("context state field cannot be blank");
            }

            SampleContext existing = contextByState.get(state);
            if (existing != null) {
                throw new IllegalStateException("context state field must be unique: existing flowId="
                        + existing.getFlowId() + " new flowId=" + context.getFlowId());
            }

            contextByState.put(state, context);

            if (context.getLoginHint() != null) {
                contextByLoginHint.put(context.getLoginHint(), context);
            }
            if (context.getCheckId() != null) {
                contextByCheck.put(context.getCheckId(), context);
            }
            if (context.getChallengeId() != null) {
                contextByChallenge.put(context.getChallengeId(), context);
            }
            if (context.getFlowId() != null) {
                contextByFlowId.put(context.getFlowId(), context);
            }
        } finally {
            writeLock.unlock();
        }
        return context;
    }

    public Optional<SampleContext> findByCheckId(String checkId) {
        Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            return Optional.ofNullable(contextByCheck.get(checkId));
        } finally {
            readLock.unlock();
        }
    }

    public Optional<SampleContext> findByChallengeId(String challengeId) {
        Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            return Optional.ofNullable(contextByChallenge.get(challengeId));
        } finally {
            readLock.unlock();
        }
    }

    public Optional<SampleContext> findByLoginHint(String loginHint) {
        Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            return Optional.ofNullable(contextByLoginHint.get(loginHint));
        } finally {
            readLock.unlock();
        }
    }

    public Optional<SampleContext> findByFlowId(String flowId) {
        Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            return Optional.ofNullable(contextByFlowId.get(flowId));
        } finally {
            readLock.unlock();
        }
    }

    public Optional<SampleContext> findByState(String state) {
        Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            return Optional.ofNullable(contextByState.get(state));
        } finally {
            readLock.unlock();
        }
    }

    public void delete(SampleContext context) {
        Lock writeLock = rwLock.writeLock();
        writeLock.lock();
        try {
            if (context.getCheckId() != null) {
                contextByCheck.remove(context.getCheckId());
            }
            if (context.getChallengeId() != null) {
                contextByChallenge.remove(context.getChallengeId());
            }
            if (context.getLoginHint() != null) {
                contextByLoginHint.remove(context.getLoginHint());
            }
            if (context.getFlowId() != null) {
                contextByFlowId.remove(context.getFlowId());
            }
            if (context.getState() != null) {
                contextByState.remove(context.getState());
            }
        } finally {
            writeLock.unlock();
        }
    }
}
