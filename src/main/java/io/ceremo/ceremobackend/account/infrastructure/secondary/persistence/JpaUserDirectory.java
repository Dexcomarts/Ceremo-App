package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.account.application.port.in.UserDirectory;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.MANDATORY)
public class JpaUserDirectory implements UserDirectory {
  private final EntityManager entityManager;

  public JpaUserDirectory(EntityManager entityManager) {
    this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
  }

  @Override
  public boolean isRegistered(UUID userId) {
    return entityManager.find(UserJpaEntity.class, Objects.requireNonNull(userId, "userId")) != null;
  }
}
