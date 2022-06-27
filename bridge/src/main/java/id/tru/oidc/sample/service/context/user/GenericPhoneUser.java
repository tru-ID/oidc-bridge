package id.tru.oidc.sample.service.context.user;

import java.util.Optional;

import org.springframework.util.StringUtils;

public class GenericPhoneUser implements IdpUser {

    private final String phoneNumber;

    public static GenericPhoneUser ofPhoneNumber(String phoneNumber) {
        if (!StringUtils.hasLength(phoneNumber)) {
            throw new IllegalArgumentException("phoneNumber cannot be empty or null");
        }

        return new GenericPhoneUser(phoneNumber);
    }

    private GenericPhoneUser(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String getId() {
        return phoneNumber;
    }

    @Override
    public String getUsername() {
        return phoneNumber;
    }

    @Override
    public Optional<String> getPhoneNumber() {
        return Optional.of(phoneNumber);
    }

    @Override
    public String toString() {
        return "GenericPhoneUser[phoneNumber=" + phoneNumber + "]";
    }
}
