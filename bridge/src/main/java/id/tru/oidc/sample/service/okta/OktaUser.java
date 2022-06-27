package id.tru.oidc.sample.service.okta;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import id.tru.oidc.sample.service.context.user.IdpUser;

public class OktaUser implements IdpUser {

    private String id;
    private String firstName;
    private String lastName;
    private String mobilePhone;
    private String primaryPhone;
    private String email;
    private String login;

    @Override
    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public String getPrimaryPhone() {
        return primaryPhone;
    }

    @Override
    @JsonIgnore
    public Optional<String> getPhoneNumber() {
        return Optional.ofNullable(mobilePhone)
                       .or(() -> Optional.ofNullable(primaryPhone));
    }

    public String getLogin() {
        return login;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public String toString() {
        return "OktaUser[id=" + id + ", email=" + email + ", login=" + login + ", phoneNumber="
                + getPhoneNumber().orElse("null") + "]";
    }
}
