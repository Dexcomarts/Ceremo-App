CREATE TABLE provider_business (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(160) NOT NULL CHECK (length(btrim(name)) > 0),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);
CREATE INDEX idx_provider_business_owner ON provider_business(owner_id);

-- The immutable provider_business.owner_id is the owner; this table holds invited and accepted team members only.
CREATE TABLE provider_business_member (
    id UUID NOT NULL PRIMARY KEY,
    provider_business_id UUID NOT NULL REFERENCES provider_business(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    role VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER', 'FINANCE')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('INVITED', 'ACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uq_provider_business_member UNIQUE (provider_business_id, user_id)
);
CREATE INDEX idx_provider_business_member_user ON provider_business_member(user_id, status, provider_business_id);

CREATE TABLE provider_business_audit (
    id UUID NOT NULL PRIMARY KEY,
    provider_business_id UUID NOT NULL REFERENCES provider_business(id) ON DELETE RESTRICT,
    actor_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    action VARCHAR(40) NOT NULL CHECK (action IN (
        'CREATED', 'MEMBER_INVITED', 'INVITATION_ACCEPTED', 'INVITATION_CANCELLED', 'MEMBER_REVOKED',
        'SUSPENDED', 'REACTIVATED')),
    reason VARCHAR(500) NOT NULL CHECK (length(btrim(reason)) > 0),
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_provider_business_audit_history ON provider_business_audit(provider_business_id, occurred_at, id);
