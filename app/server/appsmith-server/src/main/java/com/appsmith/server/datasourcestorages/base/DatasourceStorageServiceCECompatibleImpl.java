package com.appsmith.server.datasourcestorages.base;

import com.appsmith.server.helpers.PluginExecutorHelper;
import com.appsmith.server.plugins.base.PluginService;
import com.appsmith.server.repositories.DatasourceStorageRepository;
import com.appsmith.server.repositories.cakes.DatasourceStorageRepositoryCake;
import com.appsmith.server.services.AnalyticsService;
import com.appsmith.server.solutions.DatasourcePermission;
import org.springframework.stereotype.Service;

@Service
public class DatasourceStorageServiceCECompatibleImpl extends DatasourceStorageServiceCEImpl
        implements DatasourceStorageServiceCECompatible {

    public DatasourceStorageServiceCECompatibleImpl(
            DatasourceStorageRepository repositoryDirect,
            DatasourceStorageRepositoryCake repository,
            DatasourcePermission datasourcePermission,
            PluginService pluginService,
            PluginExecutorHelper pluginExecutorHelper,
            AnalyticsService analyticsService) {
        super(
                repositoryDirect,
                repository,
                datasourcePermission,
                pluginService,
                pluginExecutorHelper,
                analyticsService);
    }
}
