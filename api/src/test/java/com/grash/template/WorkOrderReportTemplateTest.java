package com.grash.template;

import com.grash.dto.ReportConfig;
import com.grash.model.*;
import com.grash.model.enums.DateFormat;
import com.grash.model.enums.Priority;
import com.grash.model.enums.RelationTypeInternal;
import com.grash.model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkOrderReportTemplateTest extends AbstractTemplateTest {

    private static final String TEMPLATE = "work-order-report.html";

    @BeforeEach
    void setUp() {
        variables = createVariables(createWorkOrder(), new ReportConfig());
    }

    @Test
    void rendersFullReport_withAllSectionsAndValues() {
        String html = render(variables, Locale.ENGLISH);

        assertTrue(html.contains("Grash Test Co."));
        assertTrue(html.contains("+1-555-0100"));
        assertTrue(html.contains("data-storage-path=\"logos/logo.png\""));
        assertTrue(html.contains("Work Order"));
        assertTrue(html.contains("WO-0001"));

        assertTrue(html.contains("Created On"));
        assertTrue(html.contains("01/05/24"));
        assertTrue(html.contains("Last Updated"));
        assertTrue(html.contains("01/06/24"));
        assertTrue(html.contains("Assigned To"));
        assertTrue(html.contains("John Doe"));
        assertTrue(html.contains("Due Date"));
        assertTrue(html.contains("Status"));
        assertTrue(html.contains("In Progress"));
        assertTrue(html.contains("Priority"));
        assertTrue(html.contains("High"));

        assertTrue(html.contains("Replace conveyor belt"));
        assertTrue(html.contains("The main conveyor belt is worn and needs to be replaced."));

        assertTrue(html.contains(panelTitle("Details")));
        assertTrue(html.contains("Warehouse A"));
        assertTrue(html.contains("Conveyor Belt #3 (CB-003)"));
        assertTrue(html.contains("Jan 07"));
        assertTrue(html.contains("4.0"));
        assertTrue(html.contains("Maintenance Crew"));
        assertTrue(html.contains("Mechanical"));
        assertTrue(html.contains("Alice Johnson, Bob Brown"));
        assertTrue(html.contains("Acme Corp"));
        assertTrue(html.contains("Jane Smith"));
        assertTrue(html.contains("01/08/24"));
        assertTrue(html.contains("Great job, parts were swapped quickly."));
        assertTrue(html.contains("Safety Note"));
        assertTrue(html.contains("High voltage area"));

        assertTrue(html.contains(panelTitle("Tasks")));
        assertTrue(html.contains("Inspect motor"));
        assertTrue(html.contains("Complete"));
        assertTrue(html.contains("Checked for wear"));
        assertTrue(html.contains("data-storage-path=\"images/task1.png\""));

        assertTrue(html.contains(panelTitle("Labors")));
        assertTrue(html.contains("0 days 2 hours 0 minutes"));
        assertTrue(html.contains("200 USD"));

        assertTrue(html.contains(panelTitle("Parts")));
        assertTrue(html.contains("Conveyor Belt"));
        assertTrue(html.contains("2.0"));
        assertTrue(html.contains("300.0 USD"));

        assertTrue(html.contains(panelTitle("Additional Costs")));
        assertTrue(html.contains("Parking fee"));
        assertTrue(html.contains("50.0 USD"));

        assertTrue(html.contains(panelTitle("Relations")));
        assertTrue(html.contains("Replace gearbox"));
        assertTrue(html.contains("is related to"));
        assertTrue(html.contains("Fix the conveyor"));

        assertTrue(html.contains(panelTitle("History")));
        assertTrue(html.contains("Maintenance history v1"));

        assertTrue(html.contains(panelTitle("Comments")));
        assertTrue(html.contains("@Jane Smith"));
        assertFalse(html.contains("user:3"));
        assertTrue(html.contains("Please confirm the replacement parts."));
        assertTrue(html.contains("data-storage-path=\"images/comment1.png\""));

        assertTrue(html.contains(panelTitle("Files")));
        assertTrue(html.contains("data-storage-path=\"files/wo-file1.png\""));

        assertTrue(html.contains(panelTitle("Signature")));
        assertTrue(html.contains("https://s3.example.com/signature.png"));

        assertTrue(html.contains("data-storage-path=\"images/wo-image.png\""));
        assertTrue(html.contains("This Work Order was created with"));
        assertTrue(html.contains("https://api.example.com/images/logo.png"));
    }

    @Test
    void hidesSections_whenReportConfigDisablesFeatures() {
        ReportConfig config = new ReportConfig();
        config.setCost(false);
        config.setComments(false);
        config.setTasks(false);
        config.setWorkOrderHistory(false);
        config.setEstimatedTime(false);
        config.setLocationAddress(false);
        config.setPriority(false);
        config.setWorkOrderInformation(false);
        config.setRelations(false);
        config.setFiles(false);
        config.setSignature(false);
        variables.put("reportConfig", config);

        String html = render(variables, Locale.ENGLISH);

        assertTrue(html.contains("Grash Test Co."));
        assertTrue(html.contains("WO-0001"));
        assertTrue(html.contains("Replace conveyor belt"));

        assertFalse(html.contains("Created On"));
        assertFalse(html.contains("Last Updated"));
        assertFalse(html.contains("Due Date"));
        assertFalse(html.contains("Status"));
        assertFalse(html.contains("Priority"));
        assertFalse(html.contains(panelTitle("Details")));
        assertFalse(html.contains("Warehouse A"));
        assertFalse(html.contains("The main conveyor belt is worn"));
        assertFalse(html.contains("In Progress"));
        assertFalse(html.contains(panelTitle("Tasks")));
        assertFalse(html.contains(panelTitle("Labors")));
        assertFalse(html.contains(panelTitle("Parts")));
        assertFalse(html.contains(panelTitle("Additional Costs")));
        assertFalse(html.contains(panelTitle("Relations")));
        assertFalse(html.contains(panelTitle("History")));
        assertFalse(html.contains(panelTitle("Comments")));
        assertFalse(html.contains(panelTitle("Files")));
        assertFalse(html.contains(panelTitle("Signature")));
        assertFalse(html.contains("High"));
    }

    @Test
    void hidesPanels_whenListsAreEmpty() {
        variables.put("tasks", Collections.emptyList());
        variables.put("labors", Collections.emptyList());
        variables.put("partQuantities", Collections.emptyList());
        variables.put("additionalCosts", Collections.emptyList());
        variables.put("relations", Collections.emptyList());
        variables.put("workOrderHistories", Collections.emptyList());
        variables.put("comments", Collections.emptyList());
        variables.put("workOrderFilesPaths", new String[0]);

        String html = render(variables, Locale.ENGLISH);

        assertTrue(html.contains(panelTitle("Details")));
        assertTrue(html.contains(panelTitle("Signature")));
        assertFalse(html.contains(panelTitle("Tasks")));
        assertFalse(html.contains(panelTitle("Labors")));
        assertFalse(html.contains(panelTitle("Parts")));
        assertFalse(html.contains(panelTitle("Additional Costs")));
        assertFalse(html.contains(panelTitle("Relations")));
        assertFalse(html.contains(panelTitle("History")));
        assertFalse(html.contains(panelTitle("Comments")));
        assertFalse(html.contains(panelTitle("Files")));
    }

    @Test
    void hidesOptionalMetaCells_whenFieldsAreNull() {
        variables.put("primaryUserName", null);
        variables.put("comments", Collections.emptyList());
        variables.put("workOrderHistories", Collections.emptyList());
        WorkOrder workOrder = (WorkOrder) variables.get("workOrder");
        workOrder.setDueDate(null);

        String html = render(variables, Locale.ENGLISH);

        assertFalse(html.contains("Due Date"));
        assertFalse(html.contains("John Doe"));
        assertTrue(html.contains("Alice Johnson, Bob Brown"));
    }

    @Test
    void escapesUserProvidedContent() {
        WorkOrder workOrder = (WorkOrder) variables.get("workOrder");
        workOrder.setTitle("<script>alert(1)</script>");
        workOrder.setDescription("Check <b>bearings</b> for wear");
        workOrder.setFeedback("Cost exceeded $100 > budget");

        String html = render(variables, Locale.ENGLISH);

        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("Check &lt;b&gt;bearings&lt;/b&gt; for wear"));
        assertTrue(html.contains("Cost exceeded $100 &gt; budget"));
    }

    @Test
    void rendersDates_withConfiguredDateFormatAndTimeZone() {
        variables.put("dateFormat", DateFormat.DDMMYY);
        variables.put("timeZone", "Europe/Paris");

        String html = render(variables, Locale.ENGLISH);

        assertTrue(html.contains("05/01/24"));
        assertTrue(html.contains("08/01/24"));
    }

    @Test
    void rendersTranslatedMessages_forFrenchLocale() {
        variables.put("locale", Locale.FRANCE);

        String html = render(variables, Locale.FRANCE);

        assertTrue(html.contains("Bon de travail"));
        assertTrue(html.contains("Statut"));
        assertTrue(html.contains("En Cours"));
        assertTrue(html.contains("Pièces"));
        assertTrue(html.contains("Historique"));
        assertTrue(html.contains("Terminé par"));
    }

    @Test
    void rendersCustomFooterLogo_whenWhiteLabelingConfigured() {
        StandardEnvironment environment = environment();
        environment.getPropertySources().addFirst(new MapPropertySource("whiteLabeling",
                Collections.singletonMap("white-labeling.logo-paths", "/images/custom.png")));
        variables.put("environment", environment);

        String html = render(variables, Locale.ENGLISH);

        assertTrue(html.contains("https://api.example.com/images/custom-logo.png"));
        assertFalse(html.contains("/images/logo.png"));
    }

    @Override
    protected String templateName() {
        return TEMPLATE;
    }

    private static String panelTitle(String title) {
        return "<span class=\"panel-title\">" + title + "</span>";
    }

    private Map<String, Object> createVariables(WorkOrder workOrder, ReportConfig config) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("companyName", "Grash Test Co.");
        vars.put("companyPhone", "+1-555-0100");
        vars.put("companyLogo", "logos/logo.png");
        vars.put("currency", "USD");
        vars.put("dateFormat", DateFormat.MMDDYY);
        vars.put("timeZone", "UTC");
        vars.put("assignedTo", "Alice Johnson, Bob Brown");
        vars.put("customers", "Acme Corp");
        vars.put("workOrder", workOrder);
        vars.put("primaryUserName", "John Doe");
        vars.put("tasks", createTasks());
        vars.put("labors", createLabors());
        vars.put("relations", createRelations());
        vars.put("additionalCosts", createAdditionalCosts());
        vars.put("workOrderHistories", createHistories());
        vars.put("partQuantities", createPartQuantities());
        vars.put("environment", environment());
        vars.put("tasksImagesPaths", Collections.singletonMap(1L,
                new String[]{"images/task1.png"}));
        vars.put("messageSource", messageSource());
        vars.put("locale", Locale.ENGLISH);
        vars.put("backgroundColor", "#2563EB");
        vars.put("reportConfig", config);
        vars.put("comments", createComments());
        vars.put("commentFilesPaths", Collections.singletonMap(1L,
                new String[]{"images/comment1.png"}));
        vars.put("workOrderFilesPaths", new String[]{"files/wo-file1.png"});
        vars.put("workOrderImagePath", "images/wo-image.png");
        return vars;
    }

    private WorkOrder createWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1L);
        workOrder.setCustomId("WO-0001");
        workOrder.setTitle("Replace conveyor belt");
        workOrder.setDescription("The main conveyor belt is worn and needs to be replaced.");
        workOrder.setCreatedAt(utc(5, 10, 0));
        workOrder.setUpdatedAt(utc(6, 11, 30));
        workOrder.setDueDate(utc(10, 8, 0));
        workOrder.setStatus(Status.IN_PROGRESS);
        workOrder.setPriority(Priority.HIGH);
        workOrder.setEstimatedDuration(4.0);
        workOrder.setEstimatedStartDate(utc(7, 9, 0));

        Location location = new Location();
        location.setId(1L);
        location.setName("Warehouse A");
        workOrder.setLocation(location);

        Asset asset = new Asset();
        asset.setId(1L);
        asset.setName("Conveyor Belt #3");
        asset.setSerialNumber("CB-003");
        workOrder.setAsset(asset);

        Team team = new Team();
        team.setId(1L);
        team.setName("Maintenance Crew");
        workOrder.setTeam(team);

        WorkOrderCategory category = new WorkOrderCategory();
        category.setId(1L);
        category.setName("Mechanical");
        workOrder.setCategory(category);

        workOrder.setPrimaryUser(user(2L, "John", "Doe"));
        workOrder.setCompletedBy(user(3L, "Jane", "Smith"));
        workOrder.setCompletedOn(utc(8, 16, 0));
        workOrder.setFeedback("Great job, parts were swapped quickly.");
        workOrder.setSignature("https://s3.example.com/signature.png");

        CustomField customField = new CustomField();
        customField.setId(1L);
        customField.setLabel("Safety Note");

        CustomFieldValue customFieldValue = new CustomFieldValue();
        customFieldValue.setId(1L);
        customFieldValue.setCustomField(customField);
        customFieldValue.setValue("High voltage area");

        workOrder.setCustomFieldValues(new ArrayList<>(Collections.singletonList(customFieldValue)));
        return workOrder;
    }

    private List<Task> createTasks() {
        TaskBase motorBase = TaskBase.builder().label("Inspect motor").build();
        Task inspectMotor = Task.builder().taskBase(motorBase).value("COMPLETE").notes("Checked for wear").build();
        inspectMotor.setId(1L);

        TaskBase lubeBase = TaskBase.builder().label("Lubricate bearings").build();
        Task lubricate = Task.builder().taskBase(lubeBase).value("Completed").build();
        lubricate.setId(2L);

        return List.of(inspectMotor, lubricate);
    }

    private List<Labor> createLabors() {
        Labor labor = new Labor();
        labor.setId(1L);
        labor.setAssignedTo(user(4L, "Alice", "Johnson"));
        labor.setDuration(7200);
        labor.setHourlyRate(100);
        labor.setStartedAt(utc(8, 8, 0));
        return List.of(labor);
    }

    private List<Relation> createRelations() {
        WorkOrder parent = new WorkOrder();
        parent.setId(10L);
        parent.setTitle("Replace gearbox");

        WorkOrder child = new WorkOrder();
        child.setId(11L);
        child.setTitle("Fix the conveyor");

        Relation relation = Relation.builder()
                .relationType(RelationTypeInternal.RELATED_TO)
                .parent(parent)
                .child(child)
                .build();
        relation.setId(1L);
        return List.of(relation);
    }

    private List<AdditionalCost> createAdditionalCosts() {
        AdditionalCost cost = new AdditionalCost();
        cost.setId(1L);
        cost.setDescription("Parking fee");
        cost.setCost(50.0);
        return List.of(cost);
    }

    private List<PartQuantity> createPartQuantities() {
        Part part = new Part();
        part.setId(1L);
        part.setName("Conveyor Belt");
        part.setCost(150.0);

        PartQuantity quantity = new PartQuantity();
        quantity.setId(1L);
        quantity.setPart(part);
        quantity.setQuantity(2.0);
        return List.of(quantity);
    }

    private List<WorkOrderHistory> createHistories() {
        WorkOrderHistory history = WorkOrderHistory.builder()
                .name("Maintenance history v1")
                .user(user(2L, "John", "Doe"))
                .build();
        history.setId(1L);
        history.setCreatedAt(utc(6, 9, 0));
        return List.of(history);
    }

    private List<Comment> createComments() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUser(user(2L, "John", "Doe"));
        comment.setContent("@[Jane Smith](user:3) Please confirm the replacement parts.");
        comment.setCreatedAt(utc(8, 10, 0));
        comment.setFiles(new ArrayList<>());
        return List.of(comment);
    }

    private User user(long id, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }
}
