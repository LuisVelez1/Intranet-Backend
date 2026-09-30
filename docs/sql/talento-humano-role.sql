INSERT IGNORE INTO roles (name)
VALUES ('TALENTO_HUMANO');

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT
    u.id,
    r.id
FROM users u
JOIN roles r
    ON r.name = 'TALENTO_HUMANO'
WHERE u.email = 'talentohumano@licodistribuciones.com';
