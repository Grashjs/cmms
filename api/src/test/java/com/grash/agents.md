# Atlas CMMS - AI Agent Testing Guidelines

This document serves as the system instruction set for any AI agents (e.g., OpenCode, Copilot, Cursor) generating or refactoring tests within the Atlas CMMS test root.

## 1. Test Layer Boundaries
Atlas CMMS maintains a strict separation between Unit (Service) tests, Controller tests, and Integration tests to ensure pipeline efficiency and reliable builds across the dual-licensing model.

### Unit Tests (Service Layer)
- **Scope:** Pure business logic (e.g., work order state transitions, logic checks).
- **Tools:** Use `MockitoExtension`, `@Mock`, `@InjectMocks`.
- **Rules:**
  - DO NOT load the Spring Context (`@SpringBootTest` is strictly forbidden here).
  - Mock all database interactions (`Repository`).
  - Focus heavily on edge cases, exception handling, and core module logic.

### Controller Tests (Web Layer)
- **Scope:** HTTP request/response routing, DTO serialization, `@PreAuthorize` security enforcement, and exception-to-HTTP-status mapping.
- **Tools:** Use `@WebMvcTest`, `MockMvc`, `@MockBean`.
- **Rules:**
  - DO NOT load the full Spring Context (`@SpringBootTest` is strictly forbidden here).
  - Mock all underlying service dependencies (`@MockBean`). Do not test service business logic in this layer.
  - Assert HTTP status codes, JSON response structures (using `jsonPath`), and header passthroughs.
  - Exhaustively test role-based access control (RBAC) and unauthorized edge cases.

### Integration Tests
- **Scope:** Infrastructure, database queries, transactions, and Spring wiring.
- **Tools:** `@SpringBootTest`, `@DataJpaTest`, `@Autowired`.
- **Rules:**
  - Rely on the real or embedded test database context.
  - DO NOT exhaustively test business logic here (leave that to the unit tests).
  - Always verify data persistence, cascading constraints, and custom JPA query accuracy.
  - Use `EntityManager.clear()` after saves to ensure the cache flushes properly.

## 2. Naming Conventions
- Unit Tests: `<ClassName>Test.java` (e.g., `WorkOrderServiceTest.java`)
- Controller Tests: `<ClassName>Test.java` (e.g., `WorkOrderControllerTest.java`)
- Integration Tests: `<ClassName>IntegrationTest.java` (e.g., `WorkOrderIntegrationTest.java`)

## 3. Execution & Context
- Ensure all tests are independent and stateless.
- Rely on `@Transactional` for rollback, but explicitly clean up external state if necessary.
- When generating tests for commercial vs. open-source features, ensure proper interface mocking so tests remain isolated.