# CMMS Oracle Spring Boot Starter (Enhanced)

This repository is a Spring Boot starter that demonstrates connecting a Java application to:

- Oracle Autonomous Database (wallet-based or direct host/service)
- Oracle Object Storage using the AWS S3-compatible API
- Liquibase migrations + Flyway SQL migration equivalents
- JWT authentication and OpenAPI (Swagger) UI
- Streaming download of large objects

Endpoints:
- GET /health
- GET /db-test
- POST /upload (multipart, field name "file")
- GET /download/{key}

Security:
- JWT-based authentication is enabled. Provide JWT token via Authorization: Bearer <token>.
- Public endpoints: /health, OpenAPI endpoints (swagger UI).

OpenAPI:
- Swagger UI available at /swagger-ui/index.html

Run:
1. Copy .env.example to .env and set credentials.
2. If using Autonomous DB wallet: download and unzip wallet into ./oracle_wallet and set ORACLE_USE_WALLET=true.
3. docker compose up --build
