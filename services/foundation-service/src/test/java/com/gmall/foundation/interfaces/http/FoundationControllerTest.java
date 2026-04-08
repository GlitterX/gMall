package com.gmall.foundation.interfaces.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmall.foundation.application.FoundationAdmissionService;
import com.gmall.foundation.application.AdmissionReviewTaskSupport;
import com.gmall.foundation.application.FoundationGovernanceService;
import com.gmall.foundation.application.FoundationRelationshipService;
import com.gmall.foundation.application.MerchantProvisionResult;
import com.gmall.foundation.application.MerchantProvisionSagaService;
import com.gmall.foundation.application.StorefrontHomePageBindingCommand;
import com.gmall.foundation.application.StorefrontGovernanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FoundationControllerTest {

    private final MerchantProvisionSagaService merchantProvisionSagaService = mock(MerchantProvisionSagaService.class);
    private final FoundationAdmissionService foundationAdmissionService = mock(FoundationAdmissionService.class);
    private final AdmissionReviewTaskSupport admissionReviewTaskSupport = mock(AdmissionReviewTaskSupport.class);
    private final FoundationGovernanceService foundationGovernanceService = mock(FoundationGovernanceService.class);
    private final FoundationRelationshipService foundationRelationshipService = mock(FoundationRelationshipService.class);
    private final StorefrontGovernanceService storefrontGovernanceService = mock(StorefrontGovernanceService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FoundationController controller = new FoundationController(
                merchantProvisionSagaService,
                foundationAdmissionService,
                admissionReviewTaskSupport,
                foundationGovernanceService,
                foundationRelationshipService,
                storefrontGovernanceService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void provisionReturnsProvisionResult() throws Exception {
        when(merchantProvisionSagaService.provision(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MerchantProvisionResult("org-1", "seller-1", "store-1"));

        mockMvc.perform(post("/api/foundation/merchant-provisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "org-1",
                                  "sellerId": "seller-1",
                                  "storefrontId": "store-1",
                                  "organizationType": "MERCHANT",
                                  "sellerType": "MERCHANT",
                                  "defaultLocale": "zh-CN",
                                  "supportedLocales": "zh-CN,en-US",
                                  "operatorId": "tester",
                                  "operationReason": "provision"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value("org-1"))
                .andExpect(jsonPath("$.sellerId").value("seller-1"))
                .andExpect(jsonPath("$.storefrontId").value("store-1"));
    }

    @Test
    void getOrganizationEligibilityReturnsInternalReadModel() throws Exception {
        when(foundationGovernanceService.getOrganizationEligibility("org-1"))
                .thenReturn(new com.gmall.foundation.application.FoundationQueryModels.OrganizationEligibilityView(
                        "org-1",
                        "MERCHANT",
                        "ACTIVE",
                        true,
                        true,
                        5L
                ));

        mockMvc.perform(get("/api/foundation/internal/organizations/org-1/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value("org-1"))
                .andExpect(jsonPath("$.sellerActivationAllowed").value(true))
                .andExpect(jsonPath("$.storefrontProvisionAllowed").value(true));
    }

    @Test
    void getOrganizationContextReturnsInternalReadModel() throws Exception {
        when(foundationGovernanceService.getOrganizationContext("org-ctx-1"))
                .thenReturn(new com.gmall.foundation.application.FoundationQueryModels.OrganizationContextView(
                        "org-ctx-1",
                        "SUPPLIER",
                        "ACTIVE",
                        "zh-CN",
                        "zh-CN,en-US",
                        true,
                        "ACTIVE",
                        7L
                ));

        mockMvc.perform(get("/api/foundation/internal/organizations/org-ctx-1/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value("org-ctx-1"))
                .andExpect(jsonPath("$.defaultLocale").value("zh-CN"))
                .andExpect(jsonPath("$.supportedLocales").value("zh-CN,en-US"))
                .andExpect(jsonPath("$.directSupplierQualified").value(true))
                .andExpect(jsonPath("$.directSupplierQualificationStatus").value("ACTIVE"));
    }

    @Test
    void resolveSupplyRelationReturnsInternalReadModel() throws Exception {
        when(foundationRelationshipService.resolveSupplyRelation("org-supplier", "org-merchant", "cat-1", "prod-1"))
                .thenReturn(new com.gmall.foundation.application.FoundationQueryModels.SupplyRelationResolutionView(
                        "rel-1",
                        "org-supplier",
                        "org-merchant",
                        "CATALOG:ALL",
                        "CATALOG:ALL",
                        "PRICE_RULE:STANDARD",
                        "SETTLEMENT_RULE:MERCHANT",
                        "ACTIVE",
                        true,
                        true,
                        "CATALOG",
                        "ALL",
                        "CATALOG_ALL_AUTHORIZED",
                        4L
                ));

        mockMvc.perform(get("/api/foundation/internal/supply-relations/resolve")
                        .param("supplierOrganizationId", "org-supplier")
                        .param("merchantOrganizationId", "org-merchant")
                        .param("categoryId", "cat-1")
                        .param("productId", "prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relationId").value("rel-1"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.settlementRule").value("SETTLEMENT_RULE:MERCHANT"))
                .andExpect(jsonPath("$.catalogAuthorized").value(true))
                .andExpect(jsonPath("$.matchedScopeType").value("CATALOG"))
                .andExpect(jsonPath("$.authorizationReason").value("CATALOG_ALL_AUTHORIZED"));
    }

    @Test
    void getStorefrontOperabilityReturnsInternalReadModel() throws Exception {
        when(storefrontGovernanceService.getOperability("store-1", "MOBILE", "PUBLISH"))
                .thenReturn(new com.gmall.foundation.application.FoundationQueryModels.StorefrontOperabilityView(
                        "store-1",
                        "MOBILE",
                        "ACTIVE",
                        true,
                        true,
                        true,
                        true,
                        true,
                        "VALID",
                        true,
                        2L,
                        3L,
                        4L,
                        true
                ));

        mockMvc.perform(get("/api/foundation/internal/storefronts/store-1/operability")
                        .param("terminalType", "MOBILE")
                        .param("operation", "PUBLISH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storefrontId").value("store-1"))
                .andExpect(jsonPath("$.operable").value(true))
                .andExpect(jsonPath("$.terminalEnabled").value(true))
                .andExpect(jsonPath("$.homePageValidationStatus").value("VALID"))
                .andExpect(jsonPath("$.homePageValidationPassed").value(true))
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    void updateTerminalEndpointUsesStatusOnlyCommand() throws Exception {
        when(storefrontGovernanceService.updateTerminal("store-1", "MOBILE", new com.gmall.foundation.application.StorefrontTerminalCommand(
                true,
                "tester",
                "enable"
        ))).thenReturn(new com.gmall.foundation.application.FoundationQueryModels.StorefrontTerminalView(
                "store-1",
                "MOBILE",
                true,
                null,
                2L
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/foundation/storefronts/store-1/terminals/MOBILE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "operatorId": "tester",
                                  "operationReason": "enable"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.homePageId").doesNotExist());
    }

    @Test
    void bindHomePageEndpointExposesDedicatedCommand() throws Exception {
        when(storefrontGovernanceService.bindHomePage("store-1", "MOBILE", new StorefrontHomePageBindingCommand(
                "home-mobile",
                "tester",
                "bind-home"
        ))).thenReturn(new com.gmall.foundation.application.FoundationQueryModels.StorefrontTerminalView(
                "store-1",
                "MOBILE",
                true,
                "home-mobile",
                3L
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/foundation/storefronts/store-1/terminals/MOBILE/home-page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "homePageId": "home-mobile",
                                  "operatorId": "tester",
                                  "operationReason": "bind-home"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.homePageId").value("home-mobile"));
    }

    @Test
    void getAdmissionReviewTaskReturnsInternalReadModel() throws Exception {
        when(foundationAdmissionService.getReviewTask("app-11"))
                .thenReturn(new com.gmall.foundation.application.FoundationQueryModels.AdmissionReviewTaskView(
                        "app-11::2",
                        "app-11",
                        2L,
                        "ACTIVE",
                        "reviewer-b",
                        java.time.OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                        java.time.OffsetDateTime.parse("2026-04-02T10:30:00+08:00"),
                        null,
                        "ops-lead",
                        "转派给复审",
                        java.time.OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                        3L
                ));

        mockMvc.perform(get("/api/foundation/internal/admission-applications/app-11/review-task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("app-11"))
                .andExpect(jsonPath("$.taskSequence").value(2))
                .andExpect(jsonPath("$.taskStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.reviewerId").value("reviewer-b"));
    }

    @Test
    void listAdmissionReviewTasksReturnsInternalWorkbenchQueue() throws Exception {
        when(foundationAdmissionService.listReviewTasks("ACTIVE", "reviewer-b"))
                .thenReturn(java.util.List.of(new com.gmall.foundation.application.FoundationQueryModels.AdmissionReviewTaskQueueItemView(
                        "app-12::2",
                        "app-12",
                        2L,
                        "ACTIVE",
                        "reviewer-b",
                        java.time.OffsetDateTime.parse("2026-04-02T10:30:00+08:00"),
                        java.time.OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                        "MERCHANT",
                        "测试商家",
                        "UNDER_REVIEW",
                        "NEAR_DUE"
                )));

        mockMvc.perform(get("/api/foundation/internal/admission-review-tasks")
                        .param("reviewerId", "reviewer-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value("app-12::2"))
                .andExpect(jsonPath("$[0].reviewerId").value("reviewer-b"))
                .andExpect(jsonPath("$[0].organizationType").value("MERCHANT"))
                .andExpect(jsonPath("$[0].applicationStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$[0].slaStatus").value("NEAR_DUE"));
    }

    @Test
    void listAdmissionPendingApplicationsReturnsInternalPendingPool() throws Exception {
        when(foundationAdmissionService.listPendingApplications("SUPPLIER"))
                .thenReturn(java.util.List.of(new com.gmall.foundation.application.FoundationQueryModels.AdmissionPendingApplicationView(
                        "app-13",
                        "SUPPLIER",
                        "待审供应商",
                        "李七",
                        "13600136007",
                        java.time.OffsetDateTime.parse("2026-04-01T08:30:00+08:00"),
                        "SUBMITTED"
                )));

        mockMvc.perform(get("/api/foundation/internal/admission-pending-applications")
                        .param("organizationType", "SUPPLIER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value("app-13"))
                .andExpect(jsonPath("$[0].organizationType").value("SUPPLIER"))
                .andExpect(jsonPath("$[0].contactName").value("李七"))
                .andExpect(jsonPath("$[0].applicationStatus").value("SUBMITTED"));
    }

    @Test
    void getAdmissionWorkbenchOverviewReturnsInternalSummary() throws Exception {
        when(admissionReviewTaskSupport.getWorkbenchOverview())
                .thenReturn(new com.gmall.foundation.application.FoundationQueryModels.AdmissionWorkbenchOverviewView(
                        4L,
                        3L,
                        1L,
                        1L
                ));

        mockMvc.perform(get("/api/foundation/internal/admission-workbench-overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingApplicationCount").value(4))
                .andExpect(jsonPath("$.activeTaskCount").value(3))
                .andExpect(jsonPath("$.nearDueTaskCount").value(1))
                .andExpect(jsonPath("$.overdueTaskCount").value(1));
    }

    @Test
    void claimAdmissionPendingApplicationReturnsUnderReviewResult() throws Exception {
        when(foundationAdmissionService.startReview(org.mockito.ArgumentMatchers.eq("app-14"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity(
                        "app-14",
                        "MERCHANT",
                        com.gmall.foundation.domain.model.AdmissionApplicationStatus.UNDER_REVIEW,
                        "待审商家",
                        "LICENSE-14",
                        "张七",
                        "13800138014",
                        "工作台认领",
                        "reviewer-a",
                        java.time.OffsetDateTime.parse("2026-04-01T15:30:00+08:00"),
                        java.time.OffsetDateTime.parse("2026-04-01T09:10:00+08:00"),
                        2L
                ));

        mockMvc.perform(post("/api/foundation/internal/admission-pending-applications/app-14/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "reviewer-a",
                                  "operationReason": "工作台认领"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("app-14"))
                .andExpect(jsonPath("$.applicationStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.reviewedBy").value("reviewer-a"));
    }

    @Test
    void rejectWithdrawAndResubmitAdmissionExposeEndpoints() throws Exception {
        when(foundationAdmissionService.reject(org.mockito.ArgumentMatchers.eq("app-2"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity(
                        "app-2",
                        "MERCHANT",
                        com.gmall.foundation.domain.model.AdmissionApplicationStatus.REJECTED,
                        "测试商家",
                        "LICENSE-2",
                        "张三",
                        "13800138000",
                        "资料不完整",
                        "reviewer",
                        java.time.OffsetDateTime.parse("2026-04-01T10:00:00+08:00"),
                        java.time.OffsetDateTime.parse("2026-04-01T09:00:00+08:00"),
                        2L
                ));
        when(foundationAdmissionService.withdraw(org.mockito.ArgumentMatchers.eq("app-3"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity(
                        "app-3",
                        "MERCHANT",
                        com.gmall.foundation.domain.model.AdmissionApplicationStatus.WITHDRAWN,
                        "测试商家",
                        "LICENSE-3",
                        "李四",
                        "13900139000",
                        "主动撤回",
                        "applicant",
                        java.time.OffsetDateTime.parse("2026-04-01T10:10:00+08:00"),
                        java.time.OffsetDateTime.parse("2026-04-01T09:10:00+08:00"),
                        2L
                ));
        when(foundationAdmissionService.resubmit(org.mockito.ArgumentMatchers.eq("app-4"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity(
                        "app-4",
                        "MERCHANT",
                        com.gmall.foundation.domain.model.AdmissionApplicationStatus.SUBMITTED,
                        "新商家名称",
                        "LICENSE-4",
                        "王五",
                        "13700137000",
                        null,
                        null,
                        null,
                        java.time.OffsetDateTime.parse("2026-04-01T10:20:00+08:00"),
                        3L
                ));
        when(foundationAdmissionService.reassignReview(org.mockito.ArgumentMatchers.eq("app-5"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity(
                        "app-5",
                        "MERCHANT",
                        com.gmall.foundation.domain.model.AdmissionApplicationStatus.UNDER_REVIEW,
                        "测试商家",
                        "LICENSE-5",
                        "赵六",
                        "13600136002",
                        "转派给复审",
                        "reviewer-b",
                        java.time.OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                        java.time.OffsetDateTime.parse("2026-04-01T09:20:00+08:00"),
                        3L
                ));

        mockMvc.perform(post("/api/foundation/admission-applications/app-2/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "reviewer",
                                  "operationReason": "资料不完整"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("app-2"))
                .andExpect(jsonPath("$.applicationStatus").value("REJECTED"));

        mockMvc.perform(post("/api/foundation/admission-applications/app-3/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "applicant",
                                  "operationReason": "主动撤回"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("app-3"))
                .andExpect(jsonPath("$.applicationStatus").value("WITHDRAWN"));

        mockMvc.perform(post("/api/foundation/admission-applications/app-4/resubmit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationId": "app-4",
                                  "organizationType": "MERCHANT",
                                  "applicantName": "新商家名称",
                                  "businessLicenseNo": "LICENSE-4",
                                  "contactName": "王五",
                                  "contactMobile": "13700137000",
                                  "operatorId": "merchant-operator",
                                  "operationReason": "补充资料后重新提交"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("app-4"))
                .andExpect(jsonPath("$.applicationStatus").value("SUBMITTED"));

        mockMvc.perform(post("/api/foundation/admission-applications/app-5/reassign-reviewer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "ops-lead",
                                  "reviewerId": "reviewer-b",
                                  "operationReason": "转派给复审"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("app-5"))
                .andExpect(jsonPath("$.applicationStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.reviewedBy").value("reviewer-b"));
    }

    @Test
    void startReviewAdmissionExposeEndpoint() throws Exception {
        when(foundationAdmissionService.startReview(org.mockito.ArgumentMatchers.eq("app-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity(
                        "app-1",
                        "MERCHANT",
                        com.gmall.foundation.domain.model.AdmissionApplicationStatus.UNDER_REVIEW,
                        "测试商家",
                        "LICENSE-1",
                        "张三",
                        "13800138000",
                        "开始审核",
                        "reviewer",
                        java.time.OffsetDateTime.parse("2026-04-01T09:30:00+08:00"),
                        java.time.OffsetDateTime.parse("2026-04-01T09:00:00+08:00"),
                        2L
                ));

        mockMvc.perform(post("/api/foundation/admission-applications/app-1/start-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "reviewer",
                                  "operationReason": "开始审核"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("app-1"))
                .andExpect(jsonPath("$.applicationStatus").value("UNDER_REVIEW"));
    }

    @Test
    void timeoutReviewAdmissionExposeEndpoint() throws Exception {
        when(foundationAdmissionService.timeoutReview(org.mockito.ArgumentMatchers.eq("app-6"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity(
                        "app-6",
                        "MERCHANT",
                        com.gmall.foundation.domain.model.AdmissionApplicationStatus.SUBMITTED,
                        "测试商家",
                        "LICENSE-6",
                        "赵六",
                        "13600136002",
                        "审核超时退回待分配",
                        "ops-supervisor",
                        java.time.OffsetDateTime.parse("2026-04-01T11:00:00+08:00"),
                        java.time.OffsetDateTime.parse("2026-04-01T09:20:00+08:00"),
                        4L
                ));

        mockMvc.perform(post("/api/foundation/admission-applications/app-6/timeout-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "ops-supervisor",
                                  "operationReason": "审核超时退回待分配"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("app-6"))
                .andExpect(jsonPath("$.applicationStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.reviewedBy").value("ops-supervisor"));
    }
}
