package com.gmall.foundation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "storefront_permission_binding")
public class StorefrontPermissionBindingEntity {

    @Id
    private String storefrontId;

    @Column(nullable = false)
    private String editorRoleIds;

    @Column(nullable = false)
    private String submitterRoleIds;

    @Column(nullable = false)
    private Long bindingVersion;

    @Column(nullable = false)
    private String operatorId;

    @Column(nullable = false)
    private String operationReason;

    @Column(nullable = false)
    private OffsetDateTime operationAt;

    protected StorefrontPermissionBindingEntity() {
    }

    public StorefrontPermissionBindingEntity(String storefrontId,
                                             String editorRoleIds,
                                             String submitterRoleIds,
                                             Long bindingVersion,
                                             String operatorId,
                                             String operationReason,
                                             OffsetDateTime operationAt) {
        this.storefrontId = storefrontId;
        this.editorRoleIds = editorRoleIds;
        this.submitterRoleIds = submitterRoleIds;
        this.bindingVersion = bindingVersion;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
    }

    public String getStorefrontId() {
        return storefrontId;
    }

    public String getEditorRoleIds() {
        return editorRoleIds;
    }

    public String getSubmitterRoleIds() {
        return submitterRoleIds;
    }

    public Long getBindingVersion() {
        return bindingVersion;
    }

    public void update(String editorRoleIds,
                       String submitterRoleIds,
                       String operatorId,
                       String operationReason,
                       OffsetDateTime operationAt) {
        this.editorRoleIds = editorRoleIds;
        this.submitterRoleIds = submitterRoleIds;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.bindingVersion = bindingVersion + 1;
    }
}
