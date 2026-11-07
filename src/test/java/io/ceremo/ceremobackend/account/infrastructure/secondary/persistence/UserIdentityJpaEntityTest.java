package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.transaction.AfterTransaction;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.jpa.properties.hibernate.connection.provider_disables_autocommit=false",
    "spring.jpa.properties.hibernate.cache.use_second_level_cache=false"
})
@Import(UserIdentityJpaEntityTest.AuditingConfiguration.class)
class UserIdentityJpaEntityTest {

    private static final Instant REGISTERED_AT =
        Instant.parse("2026-10-21T12:00:00Z");

    private static final UserIdentity IDENTITY = new UserIdentity(
        "https://identity.example/realms/Ceremo",
        "Person-123");

    @PersistenceContext
    private EntityManager entityManager;

    @AfterTransaction
    void leavesNoAccountOrIdentityRowsAfterRollback() {
        assertAll(
            () -> assertEquals(0L, entityManager.createQuery(
                "select count(u) from UserJpaEntity u", Long.class).getSingleResult()),
            () -> assertEquals(0L, entityManager.createQuery(
                "select count(i) from UserIdentityJpaEntity i", Long.class).getSingleResult()));
    }

    @Test
    void reloadsIdentityWithItsAccountAndAuditFields() {
        UserIdentityJpaEntity entity = persistIdentity();
        UUID identityId = entity.getId();
        UUID userId = entity.getUser().getId();

        entityManager.flush();
        entityManager.clear();

        UserIdentityJpaEntity saved = entityManager.find(
            UserIdentityJpaEntity.class,
            identityId);

        assertNotNull(saved);

        assertAll(
            () -> assertEquals(userId, saved.getUser().getId()),
            () -> assertEquals(IDENTITY.issuer(), saved.getIssuer()),
            () -> assertEquals(IDENTITY.subject(), saved.getSubject()),
            () -> assertEquals(REGISTERED_AT, saved.getLastSeenAt()),
            () -> assertNotNull(saved.getCreatedAt()),
            () -> assertNotNull(saved.getUpdatedAt()),
            () -> assertEquals(0L, saved.getVersion()));
    }

    @Test
    void removingIdentityDoesNotRemoveItsAccount() {
        UserIdentityJpaEntity entity = persistIdentity();
        UUID identityId = entity.getId();
        UUID userId = entity.getUser().getId();

        entityManager.flush();

        entityManager.remove(entity);
        entityManager.flush();
        entityManager.clear();

        assertAll(
            () -> assertNull(entityManager.find(
                UserIdentityJpaEntity.class, identityId)),
            () -> assertNotNull(entityManager.find(
                UserJpaEntity.class, userId)));
    }

    private UserIdentityJpaEntity persistIdentity() {
        User user = User.register(
            CeremoUUID.generate(),
            "Ama Mensah",
            "terms-v1",
            REGISTERED_AT);

        UserJpaEntity account = UserMapper.toNewEntity(user);
        entityManager.persist(account);

        UserIdentityJpaEntity identity = new UserIdentityJpaEntity(
            CeremoUUID.generate(),
            account,
            IDENTITY,
            REGISTERED_AT);

        assertNull(identity.getVersion());
        entityManager.persist(identity);

        return identity;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableJpaAuditing
    static class AuditingConfiguration {}
}
