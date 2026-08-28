# EcoBridge

Full-stack environmental action platform built with Java 21 and Spring Boot 3.5.4.

## Authentication

EcoBridge uses Spring Security OAuth2 Client with Google OpenID Connect. Google identities are linked to local rows in the `users` table by Google subject ID and unique email. Browser authentication is held in an HTTP-only SameSite session cookie; no OAuth token or client secret is stored in frontend JavaScript.

Create a Google OAuth 2.0 Web application and set:

```powershell
$env:GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
```

Authorized JavaScript origins:

- `http://localhost:8080`
- `https://ecobridge-2nfp.onrender.com`

Authorized redirect URIs:

- `http://localhost:8080/login/oauth2/code/google`
- `https://ecobridge-2nfp.onrender.com/login/oauth2/code/google`

The client secret belongs only in `.env.local` or Render environment variables. Never commit it.

## Run

Install JDK 21 and Maven 3.9+, set the Google variables, run `mvn spring-boot:run`, and open `http://localhost:8080`.

Local development uses persistent H2 data in `./data`. Production uses PostgreSQL. Existing production databases can apply `database/google-oauth-migration.sql`; Hibernate `ddl-auto=update` also creates the mapped columns.

## Firebase Storage

Firebase Authentication is not used. The Firebase Admin dependency remains only for image uploads to Firebase Storage. Configure `FIREBASE_PROJECT_ID`, `FIREBASE_STORAGE_BUCKET`, and either `GOOGLE_APPLICATION_CREDENTIALS` locally or `FIREBASE_SERVICE_ACCOUNT_JSON` on Render.

## Deploy to Render

`render.yaml` configures PostgreSQL and declares `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and the Firebase Storage variables as secrets. Production trusts forwarded HTTPS headers, uses secure cookies, and derives the standard callback from `{baseUrl}/login/oauth2/code/google`.
