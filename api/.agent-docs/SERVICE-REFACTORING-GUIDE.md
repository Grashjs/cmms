# Service Refactoring Guide

Goal: move business logic out of controllers and into the service layer, following the
exact pattern used in the `Request` refactoring. The end-user/API behavior must remain
identical — this is a pure move + wiring change, never a behavior change.

Reference implementation (already completed, use it as the canonical example):

- `controller/RequestCon``troller.java`
- `service/RequestService.java`

## The two layouts

### Before (old controller-heavy style)``

```java
@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@Transactional            // <-- remove this
public class RequestController {
    private final RequestService requestService;
    private final UserService userService;            // <-- remove when unused
    private final NotificationService notificationService; // <- moved into service
    // ... many service dependencies injected in the controller

    @GetMapping("/{id}")
    public RequestShowDTO getById(@PathVariable Long id, HttpServletRequest req) {
        User user = userService.whoami(req);          // <-- replace with @CurrentUser
        Optional<Request> optionalRequest = requestService.findById(id);
        if (optionalRequest.isPresent()) {
            Request savedRequest = optionalRequest.get();
            if (savedRequest.canBeViewedBy(user)) {
                return requestMapper.toShowDto(savedRequest);
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }
}
```

### After (service-owns-logic style)

```java
// ---- Service ----
public Request getById(Long id, User user) {
    Optional<Request> optionalRequest = requestRepository.findById(id);
    if (optionalRequest.isPresent()) {
        Request savedRequest = optionalRequest.get();
        if (savedRequest.canBeViewedBy(user)) {
            return savedRequest;
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
}

// ---- Controller ----
@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {                       // NO @Transactional here
    private final RequestService requestService;
    private final RequestMapper requestMapper;

    @GetMapping("/{id}")
    public RequestShowDTO getById(@PathVariable("id") Long id,
                                  @Parameter(hidden = true) @CurrentUser User user) {
        return requestMapper.toShowDto(requestService.getById(id, user));
    }
}
```

## Step-by-step for a new entity (e.g. Meter, Part, PurchaseOrder)

1. **Determine every piece of non-mapping logic in the controller.** Anything that is
   not "call service + `mapper.toShowDto(...)`" belongs in the service:
   - `userService.whoami(req)` / user resolution
   - permission checks vs `PermissionEntity` / `RoleCode` / `RoleType`
   - `Optional<Entity>` lookups followed by `canBeViewedBy` / `canBeEditedBy` / `canBeDeletedBy`
   - sending notifications (`NotificationService`), emails (`MailServiceFactory` +
     `messageSource` `new-request.html`/`approved-request.html`/`rejected-request.html` templates)
   - webhook dispatch (`WebhookDispatchService`, `WebhookEvent.*`)
   - workflow triggering (`WorkflowService.findByMainConditionAndCompany` +
     `runRequest`/`runWorkOrder`...)
   - on-creation/on-status-change helpers (e.g. `onRequestCreation`)

2. **Move the methods.** Add the new methods to the service. Signatures become:
   `serviceMethod(DTO / entity id(s), User user)`. The controller keeps only
   `@Parameter(hidden = true) @CurrentUser User user` and returns mapper output.

3. **Move `@Transactional`.** Delete `@Transactional` from the controller class.
   Add `@Transactional` to each service *write* method (create/patch/approve/cancel/delete).
   Read methods (`getById`, `findBySearchCriteria`, `getSearchCriteria`) do NOT get
   `@Transactional` — lazy loading during DTO mapping in the controller is safe because
   `hibernate.enable_lazy_load_no_trans: true` is set (see `application.yml`).

4. **Move dependencies.** Remove the now-unused service/utility fields from the
   controller. Add them to the service constructor (its `@RequiredArgsConstructor`).
   If a circular dependency appears, keep the field in the service and inject with
   `@Autowired @Lazy` + a setter (see `WorkflowService` in `RequestService`).

5. **Keep the search pattern.** Do NOT inline filter logic in the controller. Follow:
   - service: `public SearchCriteria getSearchCriteria(User user, SearchCriteria criteria)`
     applies role-based filters (`filterCompany`, `filterCreatedBy`, super-account
     `"inm"` company filter, etc.).
   - service: `findBySearchCriteria` returns `Page<Entity>` (entities, not DTOs).
   - controller: wrap with `service.findBySearchCriteria(service.getSearchCriteria(user, criteria))
           .map(mapper::toShowDto)`.

6. **Return type of `findBySearchCriteria`.** If it changes from `Page<ShowDTO>` to
   `Page<Entity>`, update the controller to `.map(mapper::toShowDto)` and grep the
   codebase for other callers/tests that consume the old type.

7. **Delete/portal/utility endpoints.** Follow the same move:
   - `deleteByIdAndUser(id, user)` in service returns void; controller returns
     `ResponseEntity<SuccessResponse>` unchanged.

8. **Collapse single-call service methods.** After the move, look inside the service
   for methods that are called exactly once *and only from within that same service*,
   then inline their body at the call site and remove the method:
   - Grep the whole codebase (`src/main` **and** `src/test`) for external callers
     (`service.methodName(`) before removing — the requirement is *no* external callers.
   - Typical candidates found in `PreventiveMaintenanceService`:
     - `update(id, dto, user)` → only called from `patch(...)`; inline into `patch`.
     - `delete(id)` → only called from `deleteByIdAndUser(id, user)`; inline the
       repository call (`preventiveMaintenanceRepository.deleteById(id)`).
     - `findBySearchCriteria(...)` that had **no callers at all** (dead code) — delete
       outright. (Keep `findBySearchCriteriaWithEntityGraph`, which the controller uses.)
   - When inlining, reuse the entity already fetched by the outer method instead of
     re-running `existsById` + `findById` (see pitfall below).
   - Keep the permission-check / exception order identical: e.g. `patch` checks access
     (`canBeEditedBy` → "Access Denied" 403) before the feature/plan check, and only
     throws "PreventiveMaintenance not found" when the entity is absent.
   - Leave private helper methods alone when they have multiple callers
     (e.g. `checkAccessToPreventiveMaintenance`, `setPMCustomFields`,
     `checkUsageBasedLimit`) or when inlining hurts readability (`shouldFireOnDate`,
     `getDayOfWeekNumber`).
9. Remove unused service methods. Grep the whole codebase (`src/main` **and** `src/test`) for external callers
   (`service.methodName(`)
## Behavior-equivalence checklist (mandatory before finishing)

For EACH endpoint, confirm nothing changed for the client:

- [ ] Same permission check, `CustomException` message, and `HttpStatus` code — and in
      the **same order** (e.g. approve checks `Forbidden` before `Not found`).
- [ ] Same side effects and same relative order: workflow run, entity change, webhook,
      notifications, emails.
- [ ] Same response body / HTTP status. Watch for subtle swaps like webhook payloads
      reading an id from DTO vs entity — they must produce the same value.
- [ ] `@CurrentUser` behaves identically to the old `userService.whoami(req)` — it
      resolves through `CurrentUserResolver`, which calls `userService.whoami(req)`,
      so this is equivalent by construction.
- [ ] No other callers broke from signature / return-type changes. Run a grep
      (`findBySearchCriteria`, the service method names, mapper usage).

## Verify

- Build: `mvn -q compile` (or the project's build command, check README).
- Run tests: `mvn test` (there are `controller/*Test.java`,
  `integration/*IntegrationTest.java`, `service/*ServiceTest.java` suites).
- Spot-check the refactored suite for the entity you touched.

## Pitfalls observed in the Request refactoring

- Mapping to DTO must happen in the controller (after the service call), not in the
  service. Keep `Page<Entity>`/`Entity` returns so the controller owns the mapper.
- Do not re-add `@Transactional` to the new controller; the service methods own
  transactions now.
- Do not change exception messages/codes opportunistically while moving code.
- `findById` pass-throughs in services are fine to call; if the passed entity comes
  from a find in the same service, reuse that find directly.
- Before deleting/inlining a single-call method, grep `src/test` too — a mocked
  `service.method(...)` in a test is still a caller and will break compilation.