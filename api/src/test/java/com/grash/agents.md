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

## 4. JaCoCo Coverage (Lines & Branches)
The `api/pom.xml` is configured with `jacoco-maven-plugin` (0.8.12):
- `prepare-agent` attaches the agent to every surefire run (wired via `@{argLine}` in the surefire config).
- `report` runs at the `prepare-package` phase and writes the HTML report to `api/htmlReport/`.

### Generate the report for a single service test
```bash
mvn -o prepare-package -Dtest=AssetServiceTest
```
This compiles, runs only the matching test(s), and regenerates `api/htmlReport/`. Any report from a previous run is overwritten, so always re-run before quoting coverage numbers.

To run multiple classes or a specific nested group:
```bash
mvn -o prepare-package "-Dtest=AssetServiceTest,AssetDowntimeServiceTest#Create+Update"
```

### Reading the report
- Open `api/htmlReport/index.html` for the package summary.
- Open `api/htmlReport/com.grash.service/AssetService.java.html` for the source view.
- Per-line markers: `fc` (fully covered), `pc` (partially covered), `nc` (not covered).
- Branch coverage is in the line `title` attribute, e.g. `title="1 of 4 branches missed."`. A green `bfc` span means all branches on that line were taken.
- Column meanings in the class summary (`AssetService.html`): `Missed Instructions / Cov. %`, `Missed Branches / Cov. %`, `Cxty` (complexity), `Lines`, `Methods`.

### Rules of thumb
- With Mockito unit tests only the class under test (plus real static helpers such as `Helper`, `Consts`) shows meaningful coverage; mocked collaborators show ~0%. Do not chase coverage inside mocked dependencies.
- Aim for `fc` on every executable line and `bfc` (all branches) on every `if`/`switch`/ternary. `pc` lines indicate a missed branch: add a test for that branch (e.g., the negative path, an empty collection, a null guard, or an alternative `else`).
- If a line shows `nc` despite an existing test, verify the test actually exercises that path (stubbed return values, `never()`-style branches) before adding a new one.
- Report data lives in `target/jacoco.exec`; only generate it from tests that ran with the agent attached (the `prepare-agent` execution is bound to the default `initialize` phase).