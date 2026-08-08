# EcoBridge

Full-stack environmental action platform built with Java 21 and Spring Boot.

- **Eco Pickup:** doorstep recycling requests and lifecycle status.
- **Eco Events:** discover, host, join and leave local activities.
- **Eco Feed:** publish environmental actions and like community posts.

## Run

Install JDK 21 and Maven 3.9+, run `mvn spring-boot:run`, then open `http://localhost:8080`.

The application uses a persistent embedded H2 database stored in `./data`, Spring Data JPA, BCrypt password hashing and server-side sessions. Data survives restarts.

## Firebase Authentication

Create a Firebase project and Web App. In Firebase Authentication, enable **Email/Password** and **Google** providers. Download a service-account JSON file from **Project settings → Service accounts** and set:

```powershell
$env:FIREBASE_API_KEY="your-web-api-key"
$env:FIREBASE_AUTH_DOMAIN="your-project.firebaseapp.com"
$env:FIREBASE_PROJECT_ID="your-project-id"
$env:FIREBASE_STORAGE_BUCKET="your-project.firebasestorage.app"
$env:FIREBASE_MESSAGING_SENDER_ID="your-sender-id"
$env:FIREBASE_APP_ID="your-web-app-id"
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\secure\firebase-service-account.json"
mvn spring-boot:run
```

Never place the service-account JSON in the repository. The browser signs in with Firebase, sends the Firebase ID token to Spring Boot, and the Java Firebase Admin SDK verifies it before creating the EcoBridge session.

## REST API

- `GET/POST /api/pickups`
- `GET/POST /api/events`
- `POST/DELETE /api/events/{id}/join`
- `GET/POST /api/posts`
- `POST /api/posts/{id}/like`
- `DELETE /api/posts/{id}`

For public production deployment, migrate H2 to PostgreSQL and add object storage for uploads, collector/admin roles, notifications, maps and integration tests.

## Deploy to Render

The repository includes a production `Dockerfile` and `render.yaml`. The Blueprint creates the Java web service and a PostgreSQL database in Singapore.

1. Push this project to a GitHub or GitLab repository.
2. In Render, select **New → Blueprint** and connect the repository.
3. Render reads `render.yaml` and prompts for the Firebase values marked `sync: false`.
4. Paste the Firebase Web App values and the complete service-account JSON into `FIREBASE_SERVICE_ACCOUNT_JSON`.
5. Deploy, then add the resulting `https://<service>.onrender.com` domain to Firebase Authentication → Settings → Authorized domains.

The production profile uses PostgreSQL, secure session cookies, forwarded HTTPS headers, and `/actuator/health` for platform health checks. Local development continues to use the file-backed H2 database.
