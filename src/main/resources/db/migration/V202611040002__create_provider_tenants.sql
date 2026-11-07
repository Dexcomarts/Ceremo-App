CREATE TABLE tenant (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(160) NOT NULL CHECK (length(btrim(name)) > 0),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);
CREATE INDEX idx_tenant_owner ON tenant(owner_id);

-- The immutable tenant.owner_id is the owner; this table holds invited team members only.
CREATE TABLE tenant_member (
    id UUID NOT NULL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    role VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER', 'FINANCE')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('INVITED', 'ACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255), updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uq_tenant_member UNIQUE (tenant_id, user_id)
);
CREATE INDEX idx_tenant_member_user ON tenant_member(user_id, status, tenant_id);

CREATE TABLE tenant_audit (
    id UUID NOT NULL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    actor_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    action VARCHAR(40) NOT NULL CHECK (action IN (
        'CREATED', 'MEMBER_INVITED', 'INVITATION_ACCEPTED', 'MEMBER_REVOKED', 'SUSPENDED', 'REACTIVATED')),
    reason VARCHAR(500) NOT NULL CHECK (length(btrim(reason)) > 0),
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_tenant_audit_history ON tenant_audit(tenant_id, occurred_at, id);
