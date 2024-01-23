package com.appsmith.server.domains.ce;

import com.appsmith.external.models.ActionDTO;
import com.appsmith.external.models.BranchAwareDomain;
import com.appsmith.external.models.Documentation;
import com.appsmith.external.models.PluginType;
import com.appsmith.external.views.Views;
import com.appsmith.server.domains.Application;
import com.fasterxml.jackson.annotation.JsonView;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

@Getter
@Setter
@ToString
@MappedSuperclass
public class NewActionCE extends BranchAwareDomain {

    // Fields in action that are not allowed to change between published and unpublished versions

    @JoinColumn(name = "application_id", referencedColumnName = "id")
    @JsonView(Views.Public.class)
    private Application application;

    @Column(name = "application_id", insertable = false, updatable = false)
    @JsonView(Views.Public.class)
    private String applicationId;

    @JsonView(Views.Public.class)
    String workspaceId;

    @JsonView(Views.Public.class)
    PluginType pluginType;

    @JsonView(Views.Public.class)
    String pluginId;

    @JsonView(Views.Public.class)
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    Documentation documentation; // Documentation for the template using which this action was created

    // Action specific fields that are allowed to change between published and unpublished versions
    @JsonView(Views.Public.class)
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private ActionDTO unpublishedAction;

    @JsonView(Views.Public.class)
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private ActionDTO publishedAction;

    @Override
    public void sanitiseToExportDBObject() {
        this.setApplicationId(null);
        this.setWorkspaceId(null);
        this.setDocumentation(null);
        ActionDTO unpublishedAction = this.getUnpublishedAction();
        if (unpublishedAction != null) {
            unpublishedAction.sanitiseToExportDBObject();
        }
        ActionDTO publishedAction = this.getPublishedAction();
        if (publishedAction != null) {
            publishedAction.sanitiseToExportDBObject();
        }
        super.sanitiseToExportDBObject();
    }
}
