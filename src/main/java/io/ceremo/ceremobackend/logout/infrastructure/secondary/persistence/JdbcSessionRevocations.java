package io.ceremo.ceremobackend.logout.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.logout.domain.model.UserSession;
import io.ceremo.ceremobackend.logout.domain.repository.SessionRevocations;
import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Shared database storage makes logout effective across API instances. No raw tokens are stored. */
public class JdbcSessionRevocations implements SessionRevocations {
  private final JdbcTemplate jdbc;

  public JdbcSessionRevocations(JdbcTemplate jdbc) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isRevoked(UserSession session) {
    return Boolean.TRUE.equals(jdbc.queryForObject("""
        select exists(select 1 from revoked_session where issuer = ? and session_id = ?)
        """, Boolean.class, session.identity().issuer(), session.id()));
  }

  @Override
  @Transactional
  public void revoke(UserSession session, Instant revokedAt) {
    jdbc.update("""
        insert into revoked_session(id, issuer, subject, session_id, revoked_at)
        values (?, ?, ?, ?, ?) on conflict (issuer, session_id) do nothing
        """, CeremoUUID.generate(), session.identity().issuer(), session.identity().subject(),
        session.id(), Timestamp.from(revokedAt));
  }
}
