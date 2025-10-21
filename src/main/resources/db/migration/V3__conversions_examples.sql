-- ILIKE example: SELECT * FROM users WHERE username ILIKE '%admin%';
-- Oracle: SELECT * FROM users WHERE UPPER(username) LIKE UPPER('%admin%');

CREATE INDEX idx_users_active_username ON users (CASE WHEN active = 1 THEN username ELSE NULL END);
