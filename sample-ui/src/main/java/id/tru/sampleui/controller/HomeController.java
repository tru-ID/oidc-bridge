package id.tru.sampleui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/sample-ui")
@Controller
public class HomeController {

    @GetMapping
    String index() {
        return "index";
    }

    @GetMapping("/profile")
    String profile() {
        return "profile";
    }

    @GetMapping("/authenticator/onboard")
    String authenticatorOnboard() {
        return "authenticator-onboard";
    }
}
