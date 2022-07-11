package id.tru.sampleui.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RequestMapping("/sample-ui")
@Controller
public class HomeController {

    @Autowired
    private ObjectMapper objectMapper;

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
        return mv;
    }

    @GetMapping("/authenticator/onboard")
    String authenticatorOnboard() {
        return "authenticator-onboard";
    }
}
