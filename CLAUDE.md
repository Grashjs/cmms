# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Structure

This is a monorepo for **Atlas CMMS**, a maintenance management system consisting of four sub-projects:

| Directory | Technology | Purpose | Port |
|-----------|------------|---------|------|
| `api/` | Java 17 + Spring Boot 3.2 | REST API backend | 8080 |
| `frontend/` | React 17 + TypeScript + MUI 5 | Main web application | 3000 |
| `home/` | Next.js 16 + Cloudflare | Marketing/landing website | 4000 |
| `mobile/` | React Native + Expo 53 | Mobile app (Android/iOS) | - |

---

## Development Commands

### API (Java/Spring Boot)

```bash
cd api/

# Run with local PostgreSQL (requires DB in application-dev.yml)
mvn spring-boot:run

# Run tests
mvn test

# Build JAR
mvn clean package

# Liquibase database migrations
mvn liquibase:update
```

**Local development database config:** Edit `api/src/main/resources/application-dev.yml` with your PostgreSQL credentials.

### Frontend (React)

```bash
cd frontend/

# Install dependencies
npm i

# Run locally (requires API running on localhost:8080)
set REACT_APP_API_URL=http://localhost:8080  # Windows
export REACT_APP_API_URL=http://localhost:8080  # Unix
npm start

# Build for production
npm run build

# Lint code
npm run lint
npm run lint:fix

# Format code
npm run format
```

**Custom pages location:** Application-specific pages are in `frontend/src/content/own/`. The `src/content/pages/` directory contains template pages.

### Home (Next.js)

```bash
cd home/

# Install dependencies
npm i

# Run development server
npm run dev

# Build
npm run build

# Deploy to Cloudflare
npm run deploy
```

### Mobile (React Native/Expo)

```bash
cd mobile/

# Install dependencies
npm i

# Run on Android
npm run android

# Run on iOS
npm run ios

# Run tests
npm test

# Format code
npm run format

# Build APK via EAS
npm run prebuild
eas build --profile previewAndroid --platform android
```

### Full Stack (Docker)

```bash
# Quick start - requires docker-compose.yml and .env
docker-compose up -d

# Access points:
# - Frontend: http://localhost:3000
# - API: http://localhost:8080
# - MinIO Console: http://localhost:9001
# - PostgreSQL: localhost:5432
```

---

## Architecture Overview

### Backend (api/)

Standard Spring Boot layered architecture:

- **Controllers** (`controller/`): REST endpoints, handle HTTP requests/responses
- **Services** (`service/`): Business logic, transactional operations
- **Repositories** (`repository/`): JPA/Hibernate data access layer
- **Models** (`model/`): JPA entities with Hibernate Envers for auditing
- **DTOs** (`dto/`): Data transfer objects for API contracts
- **Mappers** (`mapper/`): MapStruct interfaces for entity/DTO conversion
- **Security** (`security/`): JWT authentication, filters, OAuth2 configuration
- **Jobs** (`job/`): Quartz scheduled jobs

**Key patterns:**
- Services use constructor injection via Lombok `@RequiredArgsConstructor`
- MapStruct for mapping (not manual mappers)
- Entities use Hibernate Envers annotations for audit trails
- Configuration is environment-driven via `application.yml`

### Frontend (frontend/)

React application with Redux Toolkit state management:

- **State Management** (`slices/`): Redux Toolkit slices (asset.ts, workOrder.ts, etc.)
- **Routing** (`router/`): React Router configuration
- **API Integration** (`slices/`): Uses axios via async thunks in slices
- **UI Components** (`components/`): Reusable MUI-based components
- **Pages** (`content/own/`): Application-specific page components
- **Configuration** (`config.ts`): Runtime config via `window.__RUNTIME_CONFIG__`

**Key patterns:**
- Redux slices handle API calls via `createAsyncThunk`
- Runtime configuration supports Docker deployments
- i18n with translation files in `src/i18n/translations/`

### Mobile (mobile/)

React Native with Expo:

- **State Management** (`models/`): Redux slices similar to frontend
- **Navigation** (`navigation/`): React Navigation (bottom tabs + stack)
- **Storage**: AsyncStorage for local data including custom API URL
- **Configuration** (`config.ts`): Supports dynamic backend URL via AsyncStorage

**Build process:**
- Requires Firebase project with `google-services.json` in `android/app/`
- Uses EAS (Expo Application Services) for builds

---

## Environment Configuration

### Required Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
# Database
POSTGRES_USER=rootUser
POSTGRES_PWD=mypassword

# MinIO (file storage)
MINIO_USER=minio
MINIO_PASSWORD=minio123

# Security
JWT_SECRET_KEY=$(openssl rand -base64 32)

# URLs (update for production)
PUBLIC_API_URL=http://localhost:8080
PUBLIC_FRONT_URL=http://localhost:3000
PUBLIC_MINIO_ENDPOINT=http://localhost:9000
```

### Storage Backends

File storage supports two backends via `STORAGE_TYPE`:
- `minio` (default): Local S3-compatible storage
- `gcp`: Google Cloud Storage (requires `GCP_JSON`, `GCP_PROJECT_ID`, `GCP_BUCKET_NAME`)

---

## Database

- **PostgreSQL 16** for primary data
- **Liquibase** for schema migrations (see `api/src/main/resources/db/`)
- **Hibernate Envers** for entity auditing
- **Quartz** JDBC job store for scheduled jobs

---

## Code Style & Guidelines

### Frontend
- ESLint with Airbnb TypeScript config
- Prettier for formatting
- Husky pre-commit hooks run linting/formatting

### API
- Java 17 with Lombok for boilerplate reduction
- MapStruct for DTO/entity mapping (configured in `pom.xml` annotation processors)

---

## Testing

- **API**: JUnit 5 tests in `api/src/test/java/` (currently minimal)
- **Frontend**: React Testing Library (configure as needed)
- **Mobile**: Jest with Expo preset (`npm test`)

---

## Contributing

Each sub-project has its own `CONTRIBUTING.md`:
- Frontend: Custom pages go in `src/content/own/`
- API: Fork and submit PR with feature branches
- Mobile: Test on both Android and iOS before submitting

---

## Licensing

Dual-licensed:
- **AGPLv3**: Free/open source
- **Commercial License**: Required for white-labeling, custom branding, advanced features

Enable commercial features by setting `LICENSE_KEY` environment variable.
