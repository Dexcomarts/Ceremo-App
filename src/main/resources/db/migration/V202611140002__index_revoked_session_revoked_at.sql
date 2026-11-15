-- A1: the scheduled purge deletes by revoked_at. Spring Session JDBC indexes its cleanup column the same way
-- (SPRING_SESSION_IX2 on EXPIRY_TIME, spring-session 4.1.1 schema-postgresql.sql).
CREATE INDEX idx_revoked_session_revoked_at ON revoked_session(revoked_at);
