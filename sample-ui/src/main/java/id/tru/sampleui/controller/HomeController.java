package id.tru.sampleui.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@RequestMapping("/sample-ui")
@Controller
public class HomeController {

    @Value("${tru.id.bridge.api.base-url}")
    private String bridgeApiBaseUrl;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping
    String index() {
        return "index";
    }

    @GetMapping("/error")
    String error() {
        return "error";
    }

    @GetMapping("/profile")
    ModelAndView profile(@AuthenticationPrincipal OAuth2User oauth2User) {
        var mv = new ModelAndView("profile");
        mv.addObject("username", oauth2User.getAttribute("name"));

        Map<String, Object> userAttributes = oauth2User.getAttributes();

        mv.addObject("profileImageSource", userAttributes.get("picture"));

        String claims = null;
        try {
            claims = objectMapper.writerWithDefaultPrettyPrinter()
                                 .writeValueAsString(oauth2User.getAttributes());
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to serialize id token claims");
        }

        mv.addObject("tokenClaims", claims);

        String url = bridgeApiBaseUrl + "/user/" + oauth2User.getName() + "/factors";

        var response = restTemplate.getForEntity(url, PublicFactor[].class);
        List<PublicFactor> factors = Arrays.asList(response.getBody());

        mv.addObject("factors", factors);

        return mv;
    }


    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class PublicFactor {
        String factorId;
        String type;
        String status;
    }
}
