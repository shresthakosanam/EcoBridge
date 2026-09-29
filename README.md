# EcoBridge

EcoBridge is a Java 21 / Spring Boot 3.5.4 application. MySQL 8 is the production source of truth; Google OpenID Connect provides authentication; Firebase is retained only for image storage.

## Local MySQL

1. In MySQL Workbench, connect to your local server as an administrator and run `database/mysql-setup.sql` after replacing its example password locally.
2. Copy `.env.example` to the ignored `.env.local` and set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`.
3. Run `mvn clean package`, then `./start-ecobridge-mysql.ps1`.

The `mysql` profile runs Flyway migrations from `src/main/resources/db/migration` and Hibernate validates the resulting schema. Production never uses `ddl-auto=create`, `create-drop`, or `update`.

## Railway setup

1. Create a Railway project, add the EcoBridge repository as a service, and add a MySQL service in the same project.
2. Map Railway's private MySQL values to `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`. Do not use `localhost` in Railway.
3. Set `SPRING_PROFILES_ACTIVE=production`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `FIREBASE_PROJECT_ID`, `FIREBASE_STORAGE_BUCKET`, and `FIREBASE_SERVICE_ACCOUNT_JSON` (or the supported credentials path variable).
4. Deploy only after the build and MySQL integration tests pass, then generate a public domain for the web service.
5. In Google Cloud Console, add the generated domain—without guessing it—as:
   - Authorized JavaScript origin: `https://FINAL-RAILWAY-DOMAIN`
   - Authorized redirect URI: `https://FINAL-RAILWAY-DOMAIN/login/oauth2/code/google`

For local Google OAuth use:

- Authorized JavaScript origin: `http://localhost:8080`
- Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`

Never put `GOOGLE_CLIENT_SECRET`, database credentials, or Firebase service-account JSON in frontend files or committed configuration.

## MySQL Workbench and Railway

Enable Railway's public TCP endpoint for the MySQL service only when external Workbench access is needed. In Workbench create a Standard TCP/IP connection using the Railway-provided public host, public port, username, password, and database/schema. Use Railway's indicated SSL setting and test the connection. These are different from the private host/port used by the deployed application; do not print or commit their values.

## Authentication and storage

Google login uses `/oauth2/authorization/google` and the standard callback `{baseUrl}/login/oauth2/code/google`. Local email passwords, where enabled by the existing UI, are BCrypt hashes; Google passwords are never received or stored. Firebase Authentication is not used. Firebase Admin remains solely for image uploads, while MySQL stores the resulting URLs and metadata.
