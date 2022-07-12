package id.tru.oidc.sample.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import id.tru.oidc.sample.service.authenticator.AuthenticatorService;
import id.tru.oidc.sample.service.authenticator.Factor;

@RequestMapping("/bridge/api")
@RestController
public class ApiController {

    @Autowired
    private AuthenticatorService authenticatorService;

    @GetMapping("/user/{id}/factors")
    public List<PublicFactor> factorForUserId(@PathVariable("id") String userId) {
        return authenticatorService.findFactorsByUserId(userId)
                                   .stream()
                                   .map(PublicFactor::ofFactor)
                                   .collect(Collectors.toList());
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class PublicFactor {
        String factorId;
        String type;
        String status;

        static PublicFactor ofFactor(Factor factor) {
            var pf = new PublicFactor();
            pf.factorId = factor.getFactorId();
            pf.type = factor.getType();
            pf.status = factor.getStatus();

            return pf;
        }
    }
}
