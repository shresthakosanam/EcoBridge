ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(2048);
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE users SET role = 'ROLE_USER' WHERE role IS NULL OR role = '';
UPDATE users SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP);

ALTER TABLE users ALTER COLUMN role SET DEFAULT 'ROLE_USER';
ALTER TABLE users ALTER COLUMN role SET NOT NULL;
ALTER TABLE users ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ALTER COLUMN updated_at SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email ON users (LOWER(email));
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_google_id ON users (google_id) WHERE google_id IS NOT NULL;

-- Run the following only after every retained user has signed in with Google and been linked.
-- ALTER TABLE users DROP COLUMN IF EXISTS firebase_uid;
-- ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
-- ALTER TABLE users DROP COLUMN IF EXISTS provider;
-- ALTER TABLE users DROP COLUMN IF EXISTS avatar_url;
