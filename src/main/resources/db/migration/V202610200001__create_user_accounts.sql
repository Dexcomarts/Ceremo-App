CREATE TABLE app_user (
    id UUID NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    terms_version VARCHAR(40) NOT NULL,
    terms_accepted_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT ck_app_user_display_name
        CHECK (length(btrim(display_name)) > 0),
    CONSTRAINT ck_app_user_terms_version
        CHECK (length(btrim(terms_version)) > 0),
    CONSTRAINT ck_app_user_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'ANONYMISED')),
    CONSTRAINT ck_app_user_version CHECK (version >= 0)
);

CREATE TABLE user_identity (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    issuer VARCHAR(512) COLLATE "C" NOT NULL,
    subject VARCHAR(255) COLLATE "C" NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_user_identity PRIMARY KEY (id),
    CONSTRAINT fk_user_identity_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT uq_user_identity_issuer_subject UNIQUE (issuer, subject),
    CONSTRAINT ck_user_identity_issuer CHECK (length(btrim(issuer)) > 0),
    CONSTRAINT ck_user_identity_subject CHECK (length(btrim(subject)) > 0),
    CONSTRAINT ck_user_identity_version CHECK (version >= 0)
);

CREATE INDEX idx_user_identity_user_id ON user_identity (user_id);
