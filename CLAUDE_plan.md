Context
Multi-tenant CMMS SaaS: Java 17 + Spring Boot 3.2.3 backend, React 17 + TypeScript + MUI5 frontend.
Two features:

Superadmin Panel — List/inspect all companies, impersonate any user via JWT swap.
QR Checklist Layer — Scan QR on an asset → fill a checklist (no auth) → PDF + email notification + optional WO creation.


Architecture Diagram
QR Scan Flow:
  /checklist/:qrUuid  →  GET /checklist-submissions/public/asset/{qrUuid}
                          → returns asset info + company's checklists
                      →  POST /checklist-submissions/public/asset/{assetId}/submit
                          → saves ChecklistSubmission + answers
                          → generates PDF (Thymeleaf + iText, same as WorkOrderController:395-470)
                          → emails PDF (asset.checklistNotificationEmail or company email)
                          → optionally creates WorkOrder

Superadmin Flow:
  /app/superadmin/companies  →  GET /superadmin/companies
  /app/superadmin/companies/:id  →  GET /superadmin/companies/{id}
  Invite user  →  POST /superadmin/companies/{companyId}/users/invite
  Switch user  →  POST /superadmin/switch/{userId}  →  JWT for that user  →  loginInternal()

Feature 1: Superadmin Panel
Critical findings

ROLE_SUPER_ADMIN exists in RoleType.java
CompanyAudit.afterLoad() (line 48) already bypasses tenant checks for ROLE_SUPER_ADMIN — superadmin can load any entity freely
CompanyService.getAll() already returns all companies — reuse it
UserRepository.findByCompany_Id(Long id) already exists — reuse; use .size() for user count
UserService.invite(String email, Role role, OwnUser inviter, Boolean disableSendingMails) exists — needs an inviter from the target company (fetch the owner via findByCompany_Id and filter isOwnsCompany())
Current /auth/switch-account (line 173-195) requires ROLE_CLIENT and a SuperAccountRelation — superadmin needs a different endpoint that directly calls jwtTokenProvider.createToken(user.getEmail(), ...)

Backend changes
New file: api/src/main/java/com/grash/controller/SuperAdminController.java
java@RestController
@RequestMapping("/superadmin")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
Endpoints:

GET /superadmin/companies → companyService.getAll() filtered to exclude superadmin's own company → map to SuperAdminCompanyDTO
GET /superadmin/companies/{id} → companyService.findById(id) + userRepository.findByCompany_Id(id) → map to SuperAdminCompanyDetailDTO
POST /superadmin/companies/{companyId}/users/invite — Body: { email, roleId } → fetch role via roleService.findById(roleId) → fetch inviter (company owner via findByCompany_Id(companyId).stream().filter(u -> u.isOwnsCompany()).findFirst()) → userService.invite(email, role, inviter, false)
POST /superadmin/switch/{userId} → userService.findById(userId) → jwtTokenProvider.createToken(user.getEmail(), singletonList(user.getRole().getRoleType())) → return AuthResponse

New DTOs (in api/src/main/java/com/grash/dto/):

SuperAdminCompanyDTO — id, name, email, createdAt, subscriptionPlanName, userCount
SuperAdminCompanyDetailDTO — extends above + List<UserResponseDTO> users
SuperAdminInviteUserDTO — email, roleId

WebSecurityConfig: No changes needed — all /superadmin/** are protected by @PreAuthorize, default authenticated rule covers them.
Frontend changes
New: frontend/src/components/SuperAdminGuard.tsx

Reads user.role.roleType from auth state; redirects to /app/work-orders if not ROLE_SUPER_ADMIN

New slice: frontend/src/slices/superAdmin.ts

State: companies: Company[], singleCompanyDetail: SuperAdminCompanyDetail | null, loadingGet: boolean
Thunks: getCompanies(), getCompanyDetail(id), inviteToCompany(companyId, dto), switchToUser(userId) → on success calls loginInternal(token) + navigate to /app/work-orders

New pages:

frontend/src/content/superadmin/Companies/index.tsx — MUI DataGrid: Name, Plan, Users, Created, Detail button
frontend/src/content/superadmin/Companies/Detail/index.tsx — Company info + user table + "Invite User" button
frontend/src/content/superadmin/Companies/Detail/InviteUserModal.tsx — email + role dropdown

Router updates:

frontend/src/router/app.tsx — add:

tsx  {
    path: 'superadmin',
    element: <SuperAdminGuard />,
    children: [
      { path: 'companies', element: <SuperAdminCompanies /> },
      { path: 'companies/:companyId', element: <SuperAdminCompanyDetail /> }
    ]
  }
Sidebar (ExtendedSidebarLayout/ sidebar nav config) — add a nav item visible only when user.role.roleType === 'ROLE_SUPER_ADMIN' linking to /app/superadmin/companies.

Feature 2: QR Checklist Layer
Critical findings

Asset.barCode and Asset.nfcId exist; qrUuid and checklistNotificationEmail fields need to be added
ChecklistSubmission cannot extend CompanyAudit — its @PrePersist requires an authenticated user, but public submissions have no auth. Use a simpler base (DateAudit or just @Entity with explicit @ManyToOne Company company + manual set in service)
ChecklistService.findByCompanySettings(Long companySettingsId) exists — use it with asset's company.companySettings.id to get checklists
PDF generation pattern: WorkOrderController.java:407-467 (Thymeleaf + HtmlConverter.convertToPdf + iText)
Email with attachment: EmailService2.sendHtmlMessage() with List<EmailAttachmentDTO>
Public endpoint pattern: RequestPortalController.java:54-58 (/request-portals/public/{uuid}) + WebSecurityConfig permit rules

DB changes
Changeset 1: api/src/main/resources/db/changelog/2026_04_04_add_asset_qr_fields.xml
xml<addColumn tableName="asset">
  <column name="qr_uuid" type="VARCHAR(255)"/>
  <column name="checklist_notification_email" type="VARCHAR(255)"/>
</addColumn>
Changeset 2: api/src/main/resources/db/changelog/2026_04_04_create_checklist_submission.xml
xml<!-- checklist_submission table -->
<!-- checklist_submission_answer table (FK to checklist_submission) -->
Include both in api/src/main/resources/db/master.xml.
New entities
api/src/main/java/com/grash/model/ChecklistSubmission.java
java@Entity
public class ChecklistSubmission extends DateAudit {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @ManyToOne @NotNull
    private Company company;          // set manually in service
    
    @ManyToOne @NotNull
    private Asset asset;
    
    @ManyToOne @NotNull
    private Checklist checklist;
    
    private String submitterName;
    private String submitterEmail;
    
    private boolean workOrderCreated;
    private Long generatedWorkOrderId;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChecklistSubmissionAnswer> answers = new ArrayList<>();
}
api/src/main/java/com/grash/model/ChecklistSubmissionAnswer.java
java@Entity
public class ChecklistSubmissionAnswer extends DateAudit {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String taskLabel;
    private String taskType;
    private String value;
    private String notes;
}
Asset changes
api/src/main/java/com/grash/model/Asset.java — add:
javaprivate String qrUuid;
private String checklistNotificationEmail;
api/src/main/java/com/grash/service/AssetService.java — in create(), generate UUID if absent:
javaif (asset.getQrUuid() == null) asset.setQrUuid(UUID.randomUUID().toString());
api/src/main/java/com/grash/repository/AssetRepository.java — add:
javaOptional<Asset> findByQrUuid(String qrUuid);
New repositories

ChecklistSubmissionRepository extends JpaRepository<ChecklistSubmission, Long>

List<ChecklistSubmission> findByCompany_Id(Long companyId)


ChecklistSubmissionAnswerRepository extends JpaRepository<ChecklistSubmissionAnswer, Long>

Backend: new controller + service
ChecklistSubmissionController.java
java@RestController
@RequestMapping("/checklist-submissions")
Endpoints:

GET /checklist-submissions/public/asset/{qrUuid} — permitAll()

Finds asset via assetRepository.findByQrUuid(qrUuid) (no auth context needed — does NOT call CompanyAudit.afterLoad() because we use a plain repository query, not findById)
Returns { asset: AssetMiniDTO, checklists: List<ChecklistMiniDTO> } fetched via checklistService.findByCompanySettings(asset.getCompany().getCompanySettings().getId())


POST /checklist-submissions/public/asset/{assetId}/submit — permitAll()

Body: ChecklistSubmissionPublicDTO { checklistId, submitterName, submitterEmail, answers: [{taskLabel, taskType, value, notes}], createWorkOrder }
Fetches asset by plain assetRepository.findById(assetId) — but since there's no auth context, CompanyAudit.afterLoad() will skip the check (auth is null → returns early at line 32). Safe.
Creates and saves ChecklistSubmission with company set manually: submission.setCompany(asset.getCompany())
Calls checklistSubmissionService.generatePdf(submission) → Thymeleaf template → iText → stored PDF URL
Calls checklistSubmissionService.sendNotification(submission, pdfUrl) → email to asset.checklistNotificationEmail (or asset.getCompany().getEmail())
If createWorkOrder == true, creates a minimal WorkOrder with asset reference


POST /checklist-submissions/search — hasRole('ROLE_CLIENT')

Lists submissions for current user's company



ChecklistSubmissionService.java — contains orchestration logic above.
PDF template: api/src/main/resources/templates/checklist-submission-report.html
Based on work-order-report.html structure; variables: companyName, companyLogo, assetName, checklistName, submitterName, submittedAt, answers.
WebSecurityConfig.java — add before existing permitAll() rules:
java.requestMatchers("/checklist-submissions/public/**").permitAll()
Frontend changes
frontend/src/models/owns/asset.ts — add:
typescriptqrUuid?: string;
checklistNotificationEmail?: string;
New slice: frontend/src/slices/checklistSubmission.ts

Thunks: getChecklistPagePublic(qrUuid), submitChecklist(assetId, dto), getChecklistSubmissions(criteria)

New public page: frontend/src/content/own/Checklist/PublicPage/ChecklistPublicPage.tsx

Pattern: mirrors RequestPortalPublicPage.tsx
Uses useParams<{ qrUuid }> → dispatches getChecklistPagePublic(qrUuid) → renders asset info + checklist selector + task form + submitter name/email fields + submit button → shows success screen on done

New QR modal: frontend/src/content/own/Assets/components/QrChecklistModal.tsx

Mirrors SharePortalModal.tsx exactly; URL: `${window.location.origin}/checklist/${asset.qrUuid}`
Reuses QRCodeSVG from qrcode.react (already installed)

Asset detail page (frontend/src/content/own/Assets/Show/index.tsx or AssetDetails.tsx) — add "QR Checklist" button that opens QrChecklistModal.
Router (frontend/src/router/index.tsx) — add alongside the request-portal route:
tsxconst ChecklistPublicPage = Loader(lazy(() => import('../content/own/Checklist/PublicPage/ChecklistPublicPage')));

{ path: 'checklist/:qrUuid', element: <ChecklistPublicPage /> }

Implementation Order
1. DB: asset qr columns changeset → Asset.java fields + AssetService UUID init + AssetRepository.findByQrUuid
2. DB: checklist_submission + answer tables changeset → entities + repositories
3. Backend: ChecklistSubmissionService (PDF + email + submit orchestration)
4. Backend: ChecklistSubmissionController (public GET + POST + search) + WebSecurityConfig permitAll
5. Backend: SuperAdminController DTOs
6. Backend: SuperAdminController endpoints (getCompanies, getCompanyDetail, inviteUser, switchUser)
7. Frontend: asset.ts model update + checklistSubmission slice
8. Frontend: ChecklistPublicPage + QrChecklistModal + Asset detail "QR" button + router entry
9. Frontend: superAdmin slice + SuperAdminGuard
10. Frontend: SuperAdminCompanies + Detail + InviteUserModal + app.tsx routes + sidebar nav

Critical Files
FileChangeapi/.../model/Asset.javaAdd qrUuid, checklistNotificationEmailapi/.../service/AssetService.javaAuto-assign UUID on createapi/.../repository/AssetRepository.javaAdd findByQrUuid(String)api/.../model/ChecklistSubmission.javaNew entity (DateAudit base, manual company set)api/.../model/ChecklistSubmissionAnswer.javaNew entityapi/.../controller/ChecklistSubmissionController.javaNew — public GET/POST + authenticated searchapi/.../service/ChecklistSubmissionService.javaNew — orchestration, PDF (mirrors WorkOrderController:407-467), emailapi/.../resources/templates/checklist-submission-report.htmlNew PDF templateapi/.../configuration/WebSecurityConfig.javaAdd permitAll for /checklist-submissions/public/**api/.../controller/SuperAdminController.javaNew — company list/detail, invite, switchapi/.../dto/SuperAdminCompanyDTO.javaNew DTOapi/.../dto/SuperAdminCompanyDetailDTO.javaNew DTOapi/.../dto/SuperAdminInviteUserDTO.javaNew DTOapi/src/main/resources/db/master.xmlInclude 2 new changesetsfrontend/src/models/owns/asset.tsAdd qrUuid, checklistNotificationEmailfrontend/src/router/index.tsxAdd checklist/:qrUuid public routefrontend/src/router/app.tsxAdd superadmin/* routesfrontend/src/content/own/Checklist/PublicPage/ChecklistPublicPage.tsxNew public pagefrontend/src/content/own/Assets/components/QrChecklistModal.tsxNew QR modalfrontend/src/content/own/Assets/Show/ (index or Details)"QR Checklist" buttonfrontend/src/content/superadmin/Companies/index.tsxNew company list pagefrontend/src/content/superadmin/Companies/Detail/index.tsxNew company detail pagefrontend/src/content/superadmin/Companies/Detail/InviteUserModal.tsxNew modalfrontend/src/slices/checklistSubmission.tsNew slicefrontend/src/slices/superAdmin.tsNew slicefrontend/src/components/SuperAdminGuard.tsxNew guard componentSidebar nav config in ExtendedSidebarLayout/Conditional superadmin nav item

Verification

Asset QR UUID: Create a new asset → confirm qrUuid is populated in DB; existing assets need a migration or lazy assignment on first access.
QR Checklist public flow:

Open http://localhost:3000/checklist/{qrUuid} without logging in → asset info + checklist dropdown visible
Fill form → submit → success screen shown
Check DB for new checklist_submission row
Check email inbox for notification with PDF attachment


Superadmin panel:

Login as superadmin → "Superadmin" appears in sidebar
/app/superadmin/companies loads company list
Drill into a company → users table visible; invite a new email → invitation sent
Click "Switch" on a user → redirected as that user, token reflects their role


Guard: Access /app/superadmin/companies as non-superadmin → redirected to /app/work-orders