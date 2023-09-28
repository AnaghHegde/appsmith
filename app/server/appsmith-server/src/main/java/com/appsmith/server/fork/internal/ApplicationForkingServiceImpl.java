package com.appsmith.server.fork.internal;

import com.appsmith.external.models.Datasource;
import com.appsmith.server.domains.Application;
import com.appsmith.server.fork.forkable.ForkableService;
import com.appsmith.server.helpers.ResponseUtils;
import com.appsmith.server.newactions.base.NewActionService;
import com.appsmith.server.repositories.ActionCollectionRepository;
import com.appsmith.server.repositories.NewActionRepository;
import com.appsmith.server.repositories.NewPageRepository;
import com.appsmith.server.repositories.WorkspaceRepository;
import com.appsmith.server.services.ActionCollectionService;
import com.appsmith.server.services.AnalyticsService;
import com.appsmith.server.services.ApplicationPageService;
import com.appsmith.server.services.ApplicationService;
import com.appsmith.server.services.LayoutActionService;
import com.appsmith.server.services.PermissionGroupService;
import com.appsmith.server.services.SessionUserService;
import com.appsmith.server.services.ThemeService;
import com.appsmith.server.services.WorkspaceService;
import com.appsmith.server.solutions.ActionPermission;
import com.appsmith.server.solutions.ApplicationPermission;
import com.appsmith.server.solutions.ImportExportApplicationService;
import com.appsmith.server.solutions.PagePermission;
import com.appsmith.server.solutions.WorkspacePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ApplicationForkingServiceImpl extends ApplicationForkingServiceCEImpl
        implements ApplicationForkingService {
    private final ApplicationService applicationService;
    private final WorkspaceService workspaceService;

    public ApplicationForkingServiceImpl(
            ApplicationService applicationService,
            WorkspaceService workspaceService,
            SessionUserService sessionUserService,
            AnalyticsService analyticsService,
            ResponseUtils responseUtils,
            WorkspacePermission workspacePermission,
            ApplicationPermission applicationPermission,
            NewPageRepository newPageRepository,
            ImportExportApplicationService importExportApplicationService,
            ApplicationPageService applicationPageService,
            NewActionService newActionService,
            LayoutActionService layoutActionService,
            ActionCollectionService actionCollectionService,
            ThemeService themeService,
            PagePermission pagePermission,
            ActionPermission actionPermission,
            PermissionGroupService permissionGroupService,
            ActionCollectionRepository actionCollectionRepository,
            NewActionRepository newActionRepository,
            WorkspaceRepository workspaceRepository,
            ForkableService<Datasource> datasourceForkableService) {

        super(
                applicationService,
                workspaceService,
                sessionUserService,
                analyticsService,
                responseUtils,
                workspacePermission,
                applicationPermission,
                importExportApplicationService,
                applicationPageService,
                newPageRepository,
                newActionService,
                layoutActionService,
                actionCollectionService,
                themeService,
                pagePermission,
                actionPermission,
                permissionGroupService,
                actionCollectionRepository,
                newActionRepository,
                workspaceRepository,
                datasourceForkableService);

        this.applicationService = applicationService;
        this.workspaceService = workspaceService;
    }

    @Override
    public Mono<Application> forkApplicationToWorkspaceWithEnvironment(
            String srcApplicationId, String targetWorkspaceId, String environmentId) {
        Mono<String> fromEnvironmentIdMono = applicationService
                .findById(srcApplicationId)
                .map(Application::getWorkspaceId)
                .flatMap(workspaceId -> workspaceService.getDefaultEnvironmentId(workspaceId, null));

        return fromEnvironmentIdMono.flatMap(fromEnvironmentId -> super.forkApplicationToWorkspaceWithEnvironment(
                srcApplicationId, targetWorkspaceId, fromEnvironmentId));
    }
}
