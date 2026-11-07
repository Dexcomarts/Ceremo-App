package io.ceremo.ceremobackend.account.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class UserIdentityTest {
    public static final String ISSUER =
        "https://identity.example/realms/ceremo";

    @Test
    void  sameIssuerAndSubjectRepresentTheSameIdentity(){
        UserIdentity first = new UserIdentity(ISSUER, "person-123");
        UserIdentity second = new UserIdentity(ISSUER, "person-123");
        assertEquals(first, second);
    }


    @Test
    void sameSubjectFromDifferentIssuersIsDifferentIdentity() {
        UserIdentity first = new UserIdentity(ISSUER, "person-123");
        UserIdentity second = new UserIdentity(
            "https://identity.example/realms/another",
            "person-123");

        assertNotEquals(first, second);
    }


    @Test
    void preservesSubjectCase() {
        UserIdentity first = new UserIdentity(ISSUER, "Person-123");
        UserIdentity second = new UserIdentity(ISSUER, "person-123");

        assertEquals("Person-123", first.subject());
        assertNotEquals(first, second);
    }

    @Test
    void rejectsMissingOrBlankIdentityValues() {
        assertThrows(
            AssertionException.class,
            () -> new UserIdentity(null, "person-123"));

        assertThrows(
            AssertionException.class,
            () -> new UserIdentity(" ", "person-123"));

        assertThrows(
            AssertionException.class,
            () -> new UserIdentity(ISSUER, null));

        assertThrows(
            AssertionException.class,
            () -> new UserIdentity(ISSUER, " "));
    }

}
