package com.appsmith.server.actioncollections.importable;

import com.appsmith.server.actioncollections.base.ActionCollectionService;
import com.appsmith.server.domains.ActionCollection;
import com.appsmith.server.domains.Application;
import com.appsmith.server.imports.importable.ImportableService;
import com.appsmith.server.imports.importable.artifactbased.ArtifactBasedImportableService;
import com.appsmith.server.repositories.cakes.ActionCollectionRepositoryCake;
import org.springframework.stereotype.Service;

@Service
public class ActionCollectionImportableServiceImpl extends ActionCollectionImportableServiceCEImpl
        implements ImportableService<ActionCollection> {

    public ActionCollectionImportableServiceImpl(
            ActionCollectionService actionCollectionService,
            ActionCollectionRepositoryCake repository,
            ArtifactBasedImportableService<ActionCollection, Application> applicationImportableService) {
        super(actionCollectionService, repository, applicationImportableService);
    }
}
