package com.gmall.foundation.interfaces.http;

import com.gmall.foundation.application.FoundationGovernanceService;
import com.gmall.foundation.application.FoundationAdmissionService;
import com.gmall.foundation.application.FoundationOperationCommand;
import com.gmall.foundation.application.FoundationQueryModels.AdmissionPendingApplicationView;
import com.gmall.foundation.application.FoundationQueryModels.AdmissionReviewTaskQueueItemView;
import com.gmall.foundation.application.FoundationQueryModels.AdmissionWorkbenchOverviewView;
import com.gmall.foundation.application.FoundationQueryModels.OrganizationContextView;
import com.gmall.foundation.application.FoundationQueryModels.OrganizationEligibilityView;
import com.gmall.foundation.application.FoundationQueryModels.OrganizationView;
import com.gmall.foundation.application.FoundationQueryModels.SellerView;
import com.gmall.foundation.application.FoundationQueryModels.StorefrontView;
import com.gmall.foundation.application.FoundationQueryModels.DirectQualificationView;
import com.gmall.foundation.application.FoundationQueryModels.StorefrontOperabilityView;
import com.gmall.foundation.application.FoundationQueryModels.SupplyRelationResolutionView;
import com.gmall.foundation.application.FoundationQueryModels.SupplyRelationView;
import com.gmall.foundation.application.FoundationQueryModels.StorefrontTerminalView;
import com.gmall.foundation.application.AdmissionApplicationCommand;
import com.gmall.foundation.application.AdmissionResubmitCommand;
import com.gmall.foundation.application.AdmissionReviewCommand;
import com.gmall.foundation.application.AdmissionReviewAssignmentCommand;
import com.gmall.foundation.application.AdmissionReviewTaskSupport;
import com.gmall.foundation.application.DirectSupplierQualificationCommand;
import com.gmall.foundation.application.MerchantProvisionCommand;
import com.gmall.foundation.application.MerchantProvisionResult;
import com.gmall.foundation.application.MerchantProvisionSagaService;
import com.gmall.foundation.application.FoundationRelationshipService;
import com.gmall.foundation.application.StorefrontGovernanceService;
import com.gmall.foundation.application.StorefrontHomePageBindingCommand;
import com.gmall.foundation.application.StorefrontPermissionBindingCommand;
import com.gmall.foundation.application.StorefrontTerminalCommand;
import com.gmall.foundation.application.SupplyRelationCommand;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/foundation")
public class FoundationController {

    private final MerchantProvisionSagaService merchantProvisionSagaService;
    private final FoundationAdmissionService foundationAdmissionService;
    private final AdmissionReviewTaskSupport admissionReviewTaskSupport;
    private final FoundationGovernanceService foundationGovernanceService;
    private final FoundationRelationshipService foundationRelationshipService;
    private final StorefrontGovernanceService storefrontGovernanceService;

    public FoundationController(MerchantProvisionSagaService merchantProvisionSagaService,
                                FoundationAdmissionService foundationAdmissionService,
                                AdmissionReviewTaskSupport admissionReviewTaskSupport,
                                FoundationGovernanceService foundationGovernanceService,
                                FoundationRelationshipService foundationRelationshipService,
                                StorefrontGovernanceService storefrontGovernanceService) {
        this.merchantProvisionSagaService = merchantProvisionSagaService;
        this.foundationAdmissionService = foundationAdmissionService;
        this.admissionReviewTaskSupport = admissionReviewTaskSupport;
        this.foundationGovernanceService = foundationGovernanceService;
        this.foundationRelationshipService = foundationRelationshipService;
        this.storefrontGovernanceService = storefrontGovernanceService;
    }

    @PostMapping("/admission-applications")
    public AdmissionApplicationEntity submitAdmission(@Valid @RequestBody AdmissionApplicationCommand command) {
        return foundationAdmissionService.submit(command);
    }

    @PostMapping("/admission-applications/{applicationId}/review")
    public OrganizationView reviewAdmission(@PathVariable String applicationId,
                                            @RequestBody AdmissionReviewCommand command) {
        return foundationAdmissionService.review(applicationId, command);
    }

    @PostMapping("/admission-applications/{applicationId}/start-review")
    public AdmissionApplicationEntity startReviewAdmission(@PathVariable String applicationId,
                                                           @RequestBody FoundationOperationCommand command) {
        return foundationAdmissionService.startReview(applicationId, command);
    }

    @PostMapping("/admission-applications/{applicationId}/reject")
    public AdmissionApplicationEntity rejectAdmission(@PathVariable String applicationId,
                                                      @RequestBody FoundationOperationCommand command) {
        return foundationAdmissionService.reject(applicationId, command);
    }

    @PostMapping("/admission-applications/{applicationId}/withdraw")
    public AdmissionApplicationEntity withdrawAdmission(@PathVariable String applicationId,
                                                        @RequestBody FoundationOperationCommand command) {
        return foundationAdmissionService.withdraw(applicationId, command);
    }

    @PostMapping("/admission-applications/{applicationId}/resubmit")
    public AdmissionApplicationEntity resubmitAdmission(@PathVariable String applicationId,
                                                        @RequestBody AdmissionResubmitCommand command) {
        return foundationAdmissionService.resubmit(applicationId, command);
    }

    @PostMapping("/admission-applications/{applicationId}/reassign-reviewer")
    public AdmissionApplicationEntity reassignReviewer(@PathVariable String applicationId,
                                                       @RequestBody AdmissionReviewAssignmentCommand command) {
        return foundationAdmissionService.reassignReview(applicationId, command);
    }

    @PostMapping("/admission-applications/{applicationId}/timeout-review")
    public AdmissionApplicationEntity timeoutReviewAdmission(@PathVariable String applicationId,
                                                             @RequestBody FoundationOperationCommand command) {
        return foundationAdmissionService.timeoutReview(applicationId, command);
    }

    @GetMapping("/internal/admission-applications/{applicationId}/review-task")
    public com.gmall.foundation.application.FoundationQueryModels.AdmissionReviewTaskView getAdmissionReviewTask(@PathVariable String applicationId) {
        return foundationAdmissionService.getReviewTask(applicationId);
    }

    @GetMapping("/internal/admission-review-tasks")
    public List<AdmissionReviewTaskQueueItemView> listAdmissionReviewTasks(@RequestParam(defaultValue = "ACTIVE") String taskStatus,
                                                                           @RequestParam(required = false) String reviewerId) {
        return foundationAdmissionService.listReviewTasks(taskStatus, reviewerId);
    }

    @GetMapping("/internal/admission-pending-applications")
    public List<AdmissionPendingApplicationView> listAdmissionPendingApplications(@RequestParam(required = false) String organizationType) {
        return foundationAdmissionService.listPendingApplications(organizationType);
    }

    @PostMapping("/internal/admission-pending-applications/{applicationId}/claim")
    public AdmissionApplicationEntity claimAdmissionPendingApplication(@PathVariable String applicationId,
                                                                       @RequestBody FoundationOperationCommand command) {
        return foundationAdmissionService.startReview(applicationId, command);
    }

    @GetMapping("/internal/admission-workbench-overview")
    public AdmissionWorkbenchOverviewView getAdmissionWorkbenchOverview() {
        return admissionReviewTaskSupport.getWorkbenchOverview();
    }

    @PostMapping("/merchant-provisions")
    public MerchantProvisionResult provision(@Valid @RequestBody MerchantProvisionCommand command) {
        return merchantProvisionSagaService.provision(command);
    }

    @GetMapping("/organizations/{organizationId}")
    public OrganizationView getOrganization(@PathVariable String organizationId) {
        return foundationGovernanceService.getOrganization(organizationId);
    }

    @GetMapping("/internal/organizations/{organizationId}/eligibility")
    public OrganizationEligibilityView getOrganizationEligibility(@PathVariable String organizationId) {
        return foundationGovernanceService.getOrganizationEligibility(organizationId);
    }

    @GetMapping("/internal/organizations/{organizationId}/context")
    public OrganizationContextView getOrganizationContext(@PathVariable String organizationId) {
        return foundationGovernanceService.getOrganizationContext(organizationId);
    }

    @PostMapping("/organizations/{organizationId}/freeze")
    public OrganizationView freezeOrganization(@PathVariable String organizationId,
                                               @RequestBody FoundationOperationCommand command) {
        return foundationGovernanceService.freezeOrganization(organizationId, command);
    }

    @PostMapping("/organizations/{organizationId}/restore")
    public OrganizationView restoreOrganization(@PathVariable String organizationId,
                                                @RequestBody FoundationOperationCommand command) {
        return foundationGovernanceService.restoreOrganization(organizationId, command);
    }

    @GetMapping("/sellers/{sellerId}")
    public SellerView getSeller(@PathVariable String sellerId) {
        return foundationGovernanceService.getSeller(sellerId);
    }

    @PostMapping("/sellers/{sellerId}/suspend")
    public SellerView suspendSeller(@PathVariable String sellerId,
                                    @RequestBody FoundationOperationCommand command) {
        return foundationGovernanceService.suspendSeller(sellerId, command);
    }

    @PostMapping("/sellers/{sellerId}/restore")
    public SellerView restoreSeller(@PathVariable String sellerId,
                                    @RequestBody FoundationOperationCommand command) {
        return foundationGovernanceService.restoreSeller(sellerId, command);
    }

    @GetMapping("/storefronts/{storefrontId}")
    public StorefrontView getStorefront(@PathVariable String storefrontId) {
        return foundationGovernanceService.getStorefront(storefrontId);
    }

    @PostMapping("/storefronts/{storefrontId}/freeze")
    public StorefrontView freezeStorefront(@PathVariable String storefrontId,
                                           @RequestBody FoundationOperationCommand command) {
        return foundationGovernanceService.freezeStorefront(storefrontId, command);
    }

    @PostMapping("/storefronts/{storefrontId}/restore")
    public StorefrontView restoreStorefront(@PathVariable String storefrontId,
                                            @RequestBody FoundationOperationCommand command) {
        return foundationGovernanceService.restoreStorefront(storefrontId, command);
    }

    @PatchMapping("/storefronts/{storefrontId}/terminals/{terminalType}")
    public StorefrontTerminalView updateTerminal(@PathVariable String storefrontId,
                                                 @PathVariable String terminalType,
                                                 @RequestBody StorefrontTerminalCommand command) {
        return storefrontGovernanceService.updateTerminal(storefrontId, terminalType, command);
    }

    @PatchMapping("/storefronts/{storefrontId}/terminals/{terminalType}/home-page")
    public StorefrontTerminalView bindHomePage(@PathVariable String storefrontId,
                                               @PathVariable String terminalType,
                                               @RequestBody StorefrontHomePageBindingCommand command) {
        return storefrontGovernanceService.bindHomePage(storefrontId, terminalType, command);
    }

    @PatchMapping("/storefronts/{storefrontId}/permission-binding")
    public void updatePermissionBinding(@PathVariable String storefrontId,
                                        @RequestBody StorefrontPermissionBindingCommand command) {
        storefrontGovernanceService.updatePermissionBinding(storefrontId, command);
    }

    @PostMapping("/qualifications")
    public DirectQualificationView createQualification(@RequestBody DirectSupplierQualificationCommand command) {
        return foundationRelationshipService.createQualification(command);
    }

    @PostMapping("/qualifications/{qualificationId}/approve")
    public DirectQualificationView approveQualification(@PathVariable String qualificationId,
                                                        @RequestBody FoundationOperationCommand command) {
        return foundationRelationshipService.approveQualification(qualificationId, command);
    }

    @PostMapping("/qualifications/{qualificationId}/restore")
    public DirectQualificationView restoreQualification(@PathVariable String qualificationId,
                                                        @RequestBody FoundationOperationCommand command) {
        return foundationRelationshipService.restoreQualification(qualificationId, command);
    }

    @PostMapping("/qualifications/{qualificationId}/suspend")
    public DirectQualificationView suspendQualification(@PathVariable String qualificationId,
                                                        @RequestBody FoundationOperationCommand command) {
        return foundationRelationshipService.suspendQualification(qualificationId, command);
    }

    @PostMapping("/supply-relations")
    public SupplyRelationView createSupplyRelation(@RequestBody SupplyRelationCommand command) {
        return foundationRelationshipService.createSupplyRelation(command);
    }

    @PostMapping("/supply-relations/{relationId}/approve")
    public SupplyRelationView approveSupplyRelation(@PathVariable String relationId,
                                                    @RequestBody FoundationOperationCommand command) {
        return foundationRelationshipService.approveSupplyRelation(relationId, command);
    }

    @PostMapping("/supply-relations/{relationId}/restore")
    public SupplyRelationView restoreSupplyRelation(@PathVariable String relationId,
                                                    @RequestBody FoundationOperationCommand command) {
        return foundationRelationshipService.restoreSupplyRelation(relationId, command);
    }

    @PostMapping("/supply-relations/{relationId}/suspend")
    public SupplyRelationView suspendSupplyRelation(@PathVariable String relationId,
                                                    @RequestBody FoundationOperationCommand command) {
        return foundationRelationshipService.suspendSupplyRelation(relationId, command);
    }

    @GetMapping("/internal/supply-relations/resolve")
    public SupplyRelationResolutionView resolveSupplyRelation(@RequestParam String supplierOrganizationId,
                                                              @RequestParam String merchantOrganizationId,
                                                              @RequestParam(required = false) String categoryId,
                                                              @RequestParam(required = false) String productId) {
        return foundationRelationshipService.resolveSupplyRelation(
                supplierOrganizationId,
                merchantOrganizationId,
                categoryId,
                productId
        );
    }

    @GetMapping("/internal/storefronts/{storefrontId}/operability")
    public StorefrontOperabilityView getStorefrontOperability(@PathVariable String storefrontId,
                                                              @RequestParam String terminalType,
                                                              @RequestParam(required = false) String operation) {
        return storefrontGovernanceService.getOperability(storefrontId, terminalType, operation);
    }
}
