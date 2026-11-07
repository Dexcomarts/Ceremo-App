package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@DataJpaTest(showSql = false, properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.jpa.properties.hibernate.connection.provider_disables_autocommit=false",
    "spring.jpa.properties.hibernate.cache.use_second_level_cache=false"
})
@Import(UserJpaEntityTest.AuditingConfiguration.class)
class UserJpaEntityTest {

    @PersistenceContext
    private EntityManager entityManager;

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    void reloadsAccountWithSavedStatusAndInheritedAuditFields(UserStatus status) {
        User original = User.restore(
            CeremoUUID.generate(),
            "Ama Mensah",
            "terms-v1",
            Instant.parse("2026-10-20T12:00:00Z"),
            status);
        UserJpaEntity entity = UserMapper.toNewEntity(original);
        assertNull(entity.getVersion());

        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        UserJpaEntity saved = entityManager.find(UserJpaEntity.class, original.id());
        assertNotNull(saved);
        User restored = UserMapper.toDomain(saved);
        Object storedStatus = entityManager.createNativeQuery(
                "select status from app_user where id = :id")
            .setParameter("id", original.id())
            .getSingleResult();

        assertAll(
            () -> assertEquals(original.id(), restored.id()),
            () -> assertEquals(original.displayName(), restored.displayName()),
            () -> assertEquals(original.termsVersion(), restored.termsVersion()),
            () -> assertEquals(original.termsAcceptedAt(), restored.termsAcceptedAt()),
            () -> assertEquals(status, restored.status()),
            () -> assertEquals(status.name(), storedStatus),
            () -> assertNotNull(saved.getCreatedAt()),
            () -> assertNotNull(saved.getUpdatedAt()),
            () -> assertEquals(0L, saved.getVersion()));
    }

    @Test
    void updatesOnlyStatusAndAdvancesTheOptimisticVersion() {
        User original = User.register(CeremoUUID.generate(), "Ama Mensah", "terms-v1",
            Instant.parse("2026-10-20T12:00:00Z"));
        entityManager.persist(UserMapper.toNewEntity(original));
        entityManager.flush();

        new JpaUserRepository(entityManager, CeremoUUID::generate).updateStatus(original.activate());
        entityManager.clear();

        UserJpaEntity saved = entityManager.find(UserJpaEntity.class, original.id());
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertEquals(original.displayName(), saved.getDisplayName());
        assertEquals(original.termsVersion(), saved.getTermsVersion());
        assertEquals(original.termsAcceptedAt(), saved.getTermsAcceptedAt());
        assertEquals(1L, saved.getVersion());
    }

    @Test
    void cannotUpdateAnAbsentAccountOrANullSnapshot() {
        JpaUserRepository repository = new JpaUserRepository(entityManager, CeremoUUID::generate);
        User missing = User.register(CeremoUUID.generate(), "Ama Mensah", "terms-v1",
            Instant.parse("2026-10-20T12:00:00Z"));

        CeremoException error = assertThrows(CeremoException.class, () -> repository.updateStatus(missing));
        assertEquals(UserErrorKey.USER_NOT_FOUND, error.key());
        assertThrows(AssertionException.class, () -> repository.updateStatus(null));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableJpaAuditing
    static class AuditingConfiguration {}
}
