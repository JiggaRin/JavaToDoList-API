ALTER TABLE users ADD COLUMN first_name VARCHAR(50) NULL after username;
ALTER TABLE users ADD COLUMN last_name VARCHAR(50) NULL after first_name;