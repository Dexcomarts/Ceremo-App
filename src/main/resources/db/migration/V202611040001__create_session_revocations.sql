CREATE TABLE revoked_session (
    id UUID NOT NULL PRIMARY KEY,
    issuer VARCHAR(512) COLLATE "C" NOT NULL CHECK (length(btrim(issuer)) > 0),
    subject VARCHAR(255) COLLATE "C" NOT NULL CHECK (length(btrim(subject)) > 0),
    session_id VARCHAR(255) COLLATE "C" NOT NULL CHECK (length(btrim(session_id)) > 0),
    revoked_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_revoked_session_issuer_session UNIQUE (issuer, session_id)
);
