-- Review only, after migration 2026_10_20_invitation_authorization.
-- Current enum ordinals: ROLE_SUPER_ADMIN=0, ROLE_CLIENT=1;
-- ADMIN=0, LIMITED_ADMIN=1, USER_CREATED=6. No mutation or invitation secret is returned.
BEGIN TRANSACTION READ ONLY;

-- Privileged accounts: verify each account's legitimacy and the owner of each company.
SELECT u.id AS user_id, u.email, u.company_id, u.owns_company, u.enabled,
       r.id AS role_id, r.name AS role_name, r.role_type, r.code,
       i.invited_by_id, i.consumed_at
FROM own_user u
JOIN role r ON r.id = u.role_id
LEFT JOIN user_invitation i ON i.accepted_user_id = u.id
WHERE u.owns_company OR r.role_type = 0 OR r.code IN (0, 1)
ORDER BY u.company_id, u.id;

-- Invalid company-role associations; platform accounts are reviewed separately above.
SELECT u.id AS user_id, u.email, u.company_id, r.id AS role_id, r.name, r.code,
       r.company_settings_id AS role_settings_id, c.company_settings_id AS user_settings_id
FROM own_user u
JOIN role r ON r.id = u.role_id
LEFT JOIN company c ON c.id = u.company_id
WHERE r.role_type = 1 AND (
    c.id IS NULL
    OR (r.code = 6 AND (r.company_settings_id IS NULL OR r.company_settings_id <> c.company_settings_id))
    OR (r.code <> 6 AND r.company_settings_id IS NOT NULL)
)
ORDER BY u.company_id, u.id;

-- Pre-upgrade invitations needing renewal; do not backfill snapshots from today's permissions.
SELECT id AS invitation_id, email, company_id, invited_by_id, invited_role_id, expires_at
FROM user_invitation
WHERE consumed_at IS NULL AND revoked_at IS NULL
  AND (role_authorization_hash IS NULL OR issuer_authorization_hash IS NULL)
ORDER BY company_id, id;

-- Custom roles: compare every permission set and the implicit capabilities described in the runbook.
SELECT id AS role_id, name, company_settings_id
FROM role WHERE code = 6 ORDER BY company_settings_id, id;

ROLLBACK;
