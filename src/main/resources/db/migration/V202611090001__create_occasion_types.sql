CREATE TABLE occasion_type (
    id UUID NOT NULL,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_occasion_type PRIMARY KEY (id),
    CONSTRAINT uq_occasion_type_code UNIQUE (code),
    CONSTRAINT ck_occasion_type_code
        CHECK (code ~ '[^[:space:]]'),
    CONSTRAINT ck_occasion_type_name
        CHECK (name ~ '[^[:space:]]'),
    CONSTRAINT ck_occasion_type_version
        CHECK (version >= 0)
);