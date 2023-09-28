package com.appsmith.server.fork.internal;

import com.appsmith.external.models.BaseDomain;
import com.appsmith.external.models.Datasource;
import com.appsmith.server.constants.FieldName;
import com.appsmith.server.domains.Application;
import com.appsmith.server.dtos.ApplicationImportDTO;
import com.appsmith.server.exceptions.AppsmithError;
import com.appsmith.server.fork.forkable.ForkableService;
import com.appsmith.server.helpers.ResponseUtils;
import com.appsmith.server.helpers.UserPermissionUtils;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class ApplicationForkingServiceImpl extends ApplicationForkingServiceCEImpl
        implements ApplicationForkingService {
    private final ApplicationService applicationService;
    private final PermissionGroupService permissionGroupService;
    private final NewPageRepository newPageRepository;
    private final NewActionRepository newActionRepository;
    private final ActionCollectionRepository actionCollectionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ApplicationPermission applicationPermission;
    private final PagePermission pagePermission;
    private final ActionPermission actionPermission;
    private final WorkspacePermission workspacePermission;

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
        this.permissionGroupService = permissionGroupService;
        this.newPageRepository = newPageRepository;
        this.newActionRepository = newActionRepository;
        this.actionCollectionRepository = actionCollectionRepository;
        this.applicationPermission = applicationPermission;
        this.workspaceRepository = workspaceRepository;
        this.pagePermission = pagePermission;
        this.actionPermission = actionPermission;
        this.workspacePermission = workspacePermission;
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

    @Override
    public Mono<ApplicationImportDTO> forkApplicationToWorkspace(
            String srcApplicationId, String targetWorkspaceId, String branchName) {
        Mono<Application> applicationMono = applicationService
                .findBranchedApplicationId(branchName, srcApplicationId, applicationPermission.getEditPermission())
                .flatMap(branchedApplicationId ->
                        applicationService.findById(branchedApplicationId, applicationPermission.getEditPermission()));

        Flux<BaseDomain> pageFlux = applicationMono.flatMapMany(application -> newPageRepository
                .findAllByApplicationIdsWithoutPermission(List.of(application.getId()), List.of("id", "policies"))
                .flatMap(newPageRepository::setUserPermissionsInObject));
        Flux<BaseDomain> actionFlux = applicationMono.flatMapMany(application -> newActionRepository
                .findAllByApplicationIdsWithoutPermission(List.of(application.getId()), List.of("id", "policies"))
                .flatMap(newActionRepository::setUserPermissionsInObject));
        Flux<BaseDomain> actionCollectionFlux = applicationMono.flatMapMany(application -> actionCollectionRepository
                .findAllByApplicationIds(List.of(application.getId()), List.of("id", "policies"))
                .flatMap(actionCollectionRepository::setUserPermissionsInObject));
        Flux<BaseDomain> workspaceFlux = Flux.from(workspaceRepository
                .retrieveById(targetWorkspaceId)
                .flatMap(workspaceRepository::setUserPermissionsInObject));

        Mono<Boolean> pagesValidatedForPermission = UserPermissionUtils.validateDomainObjectPermissionsOrError(
                pageFlux,
                FieldName.PAGE,
                permissionGroupService.getSessionUserPermissionGroupIds(),
                pagePermission.getEditPermission(),
                AppsmithError.APPLICATION_NOT_FORKED_MISSING_PERMISSIONS);
        Mono<Boolean> actionsValidatedForPermission = UserPermissionUtils.validateDomainObjectPermissionsOrError(
                actionFlux,
                FieldName.ACTION,
                permissionGroupService.getSessionUserPermissionGroupIds(),
                actionPermission.getEditPermission(),
                AppsmithError.APPLICATION_NOT_FORKED_MISSING_PERMISSIONS);
        Mono<Boolean> actionCollectionsValidatedForPermission =
                UserPermissionUtils.validateDomainObjectPermissionsOrError(
                        actionCollectionFlux,
                        FieldName.ACTION,
                        permissionGroupService.getSessionUserPermissionGroupIds(),
                        actionPermission.getEditPermission(),
                        AppsmithError.APPLICATION_NOT_FORKED_MISSING_PERMISSIONS);
        Mono<Boolean> workspaceValidatedForCreateApplicationPermission =
                UserPermissionUtils.validateDomainObjectPermissionsOrError(
                        workspaceFlux,
                        FieldName.WORKSPACE,
                        permissionGroupService.getSessionUserPermissionGroupIds(),
                        workspacePermission.getApplicationCreatePermission(),
                        AppsmithError.APPLICATION_NOT_FORKED_MISSING_PERMISSIONS);
        Mono<Boolean> workspaceValidatedForCreateDatasourcePermission =
                UserPermissionUtils.validateDomainObjectPermissionsOrError(
                        workspaceFlux,
                        FieldName.WORKSPACE,
                        permissionGroupService.getSessionUserPermissionGroupIds(),
                        workspacePermission.getDatasourceCreatePermission(),
                        AppsmithError.APPLICATION_NOT_FORKED_MISSING_PERMISSIONS);

        return Mono.when(
                        pagesValidatedForPermission,
                        actionsValidatedForPermission,
                        actionCollectionsValidatedForPermission,
                        workspaceValidatedForCreateApplicationPermission,
                        workspaceValidatedForCreateDatasourcePermission)
                .then(super.forkApplicationToWorkspace(srcApplicationId, targetWorkspaceId, branchName));
    }
}
