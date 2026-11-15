-- A18: the owner also has an ACTIVE team row, so member lists and "my memberships" read one table.
-- provider_business.owner_id stays the immutable owner used for the root lock and permission checks.
ALTER TABLE provider_business_member DROP CONSTRAINT provider_business_member_role_check;
ALTER TABLE provider_business_member ADD CONSTRAINT ck_provider_business_member_role
    CHECK (role IN ('OWNER', 'MANAGER', 'FINANCE'));
ALTER TABLE provider_business_member ADD CONSTRAINT ck_provider_business_member_owner_active
    CHECK (role <> 'OWNER' OR status = 'ACTIVE');
CREATE UNIQUE INDEX uq_provider_business_member_owner ON provider_business_member(provider_business_id)
    WHERE role = 'OWNER';

-- Backfill existing businesses. A stray team row for the owner fails uq_provider_business_member loudly.
INSERT INTO provider_business_member (id, provider_business_id, user_id, role, status, created_at, updated_at, version)
SELECT uuidv7(), b.id, b.owner_id, 'OWNER', 'ACTIVE', b.created_at, b.created_at, 0
FROM provider_business b
WHERE NOT EXISTS (
    SELECT 1 FROM provider_business_member m WHERE m.provider_business_id = b.id AND m.role = 'OWNER');
