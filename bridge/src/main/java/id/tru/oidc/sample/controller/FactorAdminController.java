package id.tru.oidc.sample.controller;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import id.tru.oidc.sample.service.authenticator.AuthenticatorService;
import id.tru.oidc.sample.service.authenticator.Factor;

@Controller
@RequestMapping("/admin/factors")
public class FactorAdminController {
    @Autowired
    AuthenticatorService authenticatorService;

    @GetMapping
    public ModelAndView index() {
        Collection<Factor> factors = authenticatorService.findAll();
        var mav = new ModelAndView("factors");
        mav.addObject("factors", factors);
        return mav;
    }

    @PostMapping(path = "/create")
    public ModelAndView create(@Valid FactorCreateForm form) {
        Factor factor = authenticatorService.createFactor(form.getUsername(), form.getPhoneNumber());
        var mav = new ModelAndView("factor-onboard");
        mav.addObject("factor", factor);
        return mav;
    }

    @PostMapping(path = "/enable")
    public String enable(@Valid FactorEnableForm form) {
        Factor factor = authenticatorService.findById(form.getFactorId())
                                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                    "cannot enable non-existing factor"));
        authenticatorService.enableFactor(factor, form.getCode());

        return "redirect:/admin/factors";
    }

    @PostMapping(path = "/{id}/disable")
    public String disable(@PathVariable("id") String factorId) {
        Factor factor = authenticatorService.findById(factorId)
                                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                    "cannot disable non-existing factor"));
        authenticatorService.disableFactor(factor);

        return "redirect:/admin/factors";
    }

    private static class FactorCreateForm {
        @NotBlank
        private String phoneNumber;
        @NotBlank
        private String username;

        @SuppressWarnings("unused")
        public FactorCreateForm(String phone_number, String username) {
            this.phoneNumber = phone_number;
            this.username = username;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getUsername() {
            return username;
        }
    }

    private static class FactorEnableForm {
        @NotBlank
        private String code;
        @NotBlank
        private String factorId;

        @SuppressWarnings("unused")
        public FactorEnableForm(String code, String factor_id) {
            this.code = code;
            this.factorId = factor_id;
        }

        public String getCode() {
            return code;
        }

        public String getFactorId() {
            return factorId;
        }
    }
}
