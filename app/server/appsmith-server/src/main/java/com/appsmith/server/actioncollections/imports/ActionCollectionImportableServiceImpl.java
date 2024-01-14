package com.appsmith.server.actioncollections.imports;

import com.appsmith.server.actioncollections.base.ActionCollectionService;
import com.appsmith.server.defaultresources.DefaultResourcesService;
import com.appsmith.server.domains.ActionCollection;
import com.appsmith.server.dtos.ActionCollectionDTO;
import com.appsmith.server.imports.importable.ImportableService;
import com.appsmith.server.repositories.cakes.ActionCollectionRepositoryCake;
import org.springframework.stereotype.Service;

@Service
public class ActionCollectionImportableServiceImpl extends ActionCollectionImportableServiceCEImpl
        implements ImportableService<ActionCollection> {
    public ActionCollectionImportableServiceImpl(
            ActionCollectionService actionCollectionService,
            ActionCollectionRepositoryCake repository,
            DefaultResourcesService<ActionCollection> defaultResourcesService,
            DefaultResourcesService<ActionCollectionDTO> dtoDefaultResourcesService) {
        super(actionCollectionService, repository, defaultResourcesService, dtoDefaultResourcesService);
    }
}
