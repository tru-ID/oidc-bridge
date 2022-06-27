package id.tru.oidc.sample.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChallengeResult {
    private String challengeId;
    private String status;

    public String getChallengeId() {
        return challengeId;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "ChallengeResult[challengeId=" + challengeId + ", status=" + status + "]";
    }
}