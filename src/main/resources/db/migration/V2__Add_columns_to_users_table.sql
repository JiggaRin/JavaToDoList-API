ALTER TABLE users
ADD COLUMN email VARCHAR(255) NOT NULL UNIQUE after username;

ALTER TABLE users
ADD COLUMN role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' AFTER password;

ALTER TABLE users
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER role;

ALTER TABLE users
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

INSERT INTO users (username, password, email, role, created_at, updated_at)
SELECT 'admin', SHA2('admin', 256), 'admin@admin.com', 'ADMIN', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin' AND role = 'ADMIN'
);
