package com.appsmith.server.services;

import com.appsmith.external.constants.AnalyticsEvents;
import com.appsmith.external.models.Datasource;
import com.appsmith.external.models.Policy;
import com.appsmith.server.acl.AclPermission;
import com.appsmith.server.acl.AppsmithRole;
import com.appsmith.server.acl.PolicyGenerator;
import com.appsmith.server.constants.FieldName;
import com.appsmith.server.domains.Action;
import com.appsmith.server.domains.Application;
import com.appsmith.server.domains.NewAction;
import com.appsmith.server.domains.NewPage;
import com.appsmith.server.domains.Page;
import com.appsmith.server.domains.PermissionGroup;
import com.appsmith.server.domains.User;
import com.appsmith.server.domains.UserGroup;
import com.appsmith.server.domains.Workspace;
import com.appsmith.server.dtos.InviteUsersToApplicationDTO;
import com.appsmith.server.dtos.MemberInfoDTO;
import com.appsmith.server.dtos.PermissionGroupInfoDTO;
import com.appsmith.server.dtos.UpdateApplicationRoleDTO;
import com.appsmith.server.exceptions.AppsmithError;
import com.appsmith.server.exceptions.AppsmithException;
import com.appsmith.server.helpers.PolicyUtils;
import com.appsmith.server.helpers.ResponseUtils;
import com.appsmith.server.repositories.ApplicationRepository;
import com.appsmith.server.repositories.PermissionGroupRepository;
import com.appsmith.server.repositories.UserGroupRepository;
import com.appsmith.server.repositories.UserRepository;
import com.appsmith.server.services.ce.ApplicationServiceCEImpl;
import com.appsmith.server.solutions.ApplicationPermission;
import com.appsmith.server.solutions.DatasourcePermission;
import com.appsmith.server.solutions.PermissionGroupPermission;
import com.appsmith.server.solutions.roles.RoleConfigurationSolution;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.appsmith.server.constants.FieldName.ADMINISTRATOR;
import static com.appsmith.server.constants.FieldName.APPLICATION_DEVELOPER;
import static com.appsmith.server.constants.FieldName.APPLICATION_DEVELOPER_DESCRIPTION;
import static com.appsmith.server.constants.FieldName.APPLICATION_VIEWER;
import static com.appsmith.server.constants.FieldName.APPLICATION_VIEWER_DESCRIPTION;
import static com.appsmith.server.constants.FieldName.DEVELOPER;
import static com.appsmith.server.constants.FieldName.GROUP_ID;
import static com.appsmith.server.constants.FieldName.ROLE;
import static com.appsmith.server.constants.FieldName.USERNAME;
import static com.appsmith.server.constants.FieldName.VIEWER;
import static com.appsmith.server.helpers.TextUtils.generateDefaultRoleNameForResource;
import static com.appsmith.server.helpers.AppsmithComparators.permissionGroupInfoWithEntityTypeComparator;


@Slf4j
@Service
public class ApplicationServiceImpl extends ApplicationServiceCEImpl implements ApplicationService {

    private final PermissionGroupService permissionGroupService;
    private final PolicyUtils policyUtils;
    private final PermissionGroupRepository permissionGroupRepository;
    private final PermissionGroupPermission permissionGroupPermission;
    private final RoleConfigurationSolution roleConfigurationSolution;
    private final PolicyGenerator policyGenerator;
    private final UserService userService;
    private final UserGroupRepository userGroupRepository;
    private final ApplicationPermission applicationPermission;
    private final SessionUserService sessionUserService;

    public ApplicationServiceImpl(Scheduler scheduler,
                                  Validator validator,
                                  MongoConverter mongoConverter,
                                  ReactiveMongoTemplate reactiveMongoTemplate,
                                  ApplicationRepository repository,
                                  AnalyticsService analyticsService,
                                  PolicyUtils policyUtils,
                                  ConfigService configService,
                                  SessionUserService sessionUserService,
                                  ResponseUtils responseUtils,
                                  PermissionGroupService permissionGroupService,
                                  TenantService tenantService,
                                  AssetService assetService,
                                  UserRepository userRepository,
                                  DatasourcePermission datasourcePermission,
                                  ApplicationPermission applicationPermission,
                                  PermissionGroupRepository permissionGroupRepository,
                                  PermissionGroupPermission permissionGroupPermission,
                                  RoleConfigurationSolution roleConfigurationSolution,
                                  PolicyGenerator policyGenerator,
                                  UserService userService,
                                  UserGroupRepository userGroupRepository) {

        super(scheduler, validator, mongoConverter, reactiveMongoTemplate, repository, analyticsService, policyUtils,
                configService, sessionUserService, responseUtils, permissionGroupService, tenantService, assetService,
                userRepository, datasourcePermission, applicationPermission);
        this.permissionGroupService = permissionGroupService;
        this.policyUtils = policyUtils;
        this.permissionGroupRepository = permissionGroupRepository;
        this.permissionGroupPermission = permissionGroupPermission;
        this.roleConfigurationSolution = roleConfigurationSolution;
        this.policyGenerator = policyGenerator;
        this.userService = userService;
        this.userGroupRepository = userGroupRepository;
        this.applicationPermission = applicationPermission;
        this.sessionUserService = sessionUserService;
    }
    /**
     * <p>
     * Generate a Default Application Role for given {@code application} and {@code roleType}.
     * <p>
     * If the {@code roleType} doesn't match {@code APPLICATION_DEVELOPER} or {@code APPLICATION_VIEWER}, then
     * an {@link AppsmithException} with error {@code INVALID_PARAMETER} is thrown.
     * @param application {@link Application}
     * @param roleType {@link String}
     * @return {@link Mono}<{@link PermissionGroup}>
     */
    @Override
    public Mono<PermissionGroup> createDefaultRole(Application application, String roleType) {
        Mono<PermissionGroup> createdDefaultRoleMono;
        if (roleType.equalsIgnoreCase(APPLICATION_DEVELOPER)) {
            createdDefaultRoleMono = createDefaultDeveloperRole(application);
        } else if (roleType.equalsIgnoreCase(APPLICATION_VIEWER)) {
            createdDefaultRoleMono = createDefaultViewerRole(application);
        } else {
            return Mono.error(new AppsmithException(AppsmithError.INVALID_PARAMETER, "Role Type"));
        }
        return createdDefaultRoleMono
                /*
                 * Now that the role has been created, we will update the policy of application and related resources
                 * with the created role, so that the resources are accessible to entities who will be assigned this
                 * role.
                 */
                .flatMap(role -> updatePoliciesForApplicationAndRelatedResources(application, role, roleType));
    }

    /**
     * <p>
     * The method is responsible for creating the application default role - Developer.
     * <br>
     * Steps:
     * <ol>
     *    <li>Create an empty role with auto-generated name.</li>
     *    <li>Give the workspace default roles permissions to assign/un-assign/read members for the created role.</li>
     *    <li>Generate and set policies for the created role to assign/un-assign/read members for itself.</li>
     * </ol>
     *
     * @param application {@link Application} for which we want to create the application default role - Developer.
     * @return {@link Mono}<{@link PermissionGroup}>
     */
    private Mono<PermissionGroup> createDefaultDeveloperRole(Application application) {
        PermissionGroup defaultDeveloperRole = new PermissionGroup();
        defaultDeveloperRole.setDefaultDomainId(application.getId());
        defaultDeveloperRole.setDefaultDomainType(Application.class.getSimpleName());
        defaultDeveloperRole.setName(generateDefaultRoleNameForResource(APPLICATION_DEVELOPER, application.getName()));
        defaultDeveloperRole.setDescription(APPLICATION_DEVELOPER_DESCRIPTION);
        return permissionGroupService.create(defaultDeveloperRole)
                /*
                 * Default workspace roles: Admin / Developer are given assign/un-assign/read members for
                 * defaultDeveloperRole.
                 */
                .flatMap(role -> giveDefaultWorkspaceRolesAccessToRole(application.getWorkspaceId(), role))
                .flatMap(role -> generateAndUpdatePoliciesForDefaultDeveloperRole(role, application));
    }

    /**
     * Method responsible for creating the application default role - App Viewer.
     * <br>
     * Steps:
     * <ol>
     *    <li>Create an empty role with auto-generated name.</li>
     *    <li>Give the workspace default roles permissions to assign/un-assign/read members for the created role.</li>
     *    <li>Generate and set policies for the created role to assign/un-assign/read members for itself.</li>
     * </ol>
     *
     * @param application {@link Application} for which we want to create the application default role - Viewer.
     * @return {@link Mono}<{@link PermissionGroup}>
     */
    private Mono<PermissionGroup> createDefaultViewerRole(Application application) {
        PermissionGroup defaultViewerRole = new PermissionGroup();
        defaultViewerRole.setDefaultDomainId(application.getId());
        defaultViewerRole.setDefaultDomainType(Application.class.getSimpleName());
        defaultViewerRole.setName(generateDefaultRoleNameForResource(APPLICATION_VIEWER, application.getName()));
        defaultViewerRole.setDescription(APPLICATION_VIEWER_DESCRIPTION);
        return permissionGroupService.create(defaultViewerRole)
                /*
                 * Default workspace roles: Admin / Developer / App Viewer are given assign/un-assign/read members for
                 * defaultViewerRole.
                 */
                .flatMap(role -> giveDefaultWorkspaceRolesAccessToRole(application.getWorkspaceId(), role))
                .flatMap(role -> generateAndUpdatePoliciesForDefaultViewerRole(role, application));
    }

    /**
     * <p>
     * The method is responsible for generating and adding policies to the application default role - Developer.
     * It is also responsible for updating the policies for the application default role - App Viewer as well, if it exists.
     * Method gives <b>{@code appDeveloperRole}</b>, access to following permissions for <b>{@code appDeveloperRole}</b> and
     * application default role - App Viewer (if it exists).
     * <li> {@link AclPermission}.{@code ASSIGN_PERMISSION_GROUPS}
     * <li> {@link AclPermission}.{@code UNASSIGN_PERMISSION_GROUPS}
     * <li> {@link AclPermission}.{@code READ_PERMISSION_GROUP_MEMBERS}
     * @param appDeveloperRole
     * @param application
     * @implNote The return statement may look similar to the return statement of
     * {@linkplain ApplicationServiceImpl#generateAndUpdatePoliciesForDefaultViewerRole(PermissionGroup, Application)}
     * but there is a very minute difference, i.e., here {@code appViewerRole} is coming from the
     * {@code appViewerRoleFlux}.
     */
    private Mono<PermissionGroup> generateAndUpdatePoliciesForDefaultDeveloperRole(PermissionGroup appDeveloperRole,
                                                                                   Application application) {
        /*
         * Generate policy map using assign permission group.
         * This way, it will generate a map, which will contain policies related to assign, un-assign and read members
         * permissions which will be added to it's existing policy.
         */
        Map<String, Policy> policyMap = policyUtils.generatePolicyFromPermissionWithPermissionGroup(
                permissionGroupPermission.getAssignPermission(), appDeveloperRole.getId());
        policyUtils.addPoliciesToExistingObject(policyMap, appDeveloperRole);
        Flux<PermissionGroup> appViewerRoleFlux = permissionGroupRepository
                .findByDefaultDomainIdAndDefaultDomainType(application.getId(), Application.class.getSimpleName())
                .filter(role -> role.getDefaultDomainType().equals(Application.class.getSimpleName())
                        && role.getName().startsWith(APPLICATION_VIEWER))
                .cache();
        return appViewerRoleFlux.hasElements()
                .flatMap(isAppViewerRolePresent -> {
                    if (isAppViewerRolePresent) {
                        return appViewerRoleFlux.single()
                                .flatMap(appViewerRole -> {
                                    giveDevAppRolePermissionsToViewAppRole(appDeveloperRole, appViewerRole);
                                    return permissionGroupService.save(appViewerRole);
                                });
                    }
                    return Mono.empty();
                })
                .then(permissionGroupService.save(appDeveloperRole));
    }

    /**
     * <p>
     * The method is responsible for generating and adding policies to the application default role - App Viewer
     * When updating the policies, it also takes into consideration, whether the Application Developer Role exists or
     * not. Method gives <b>{@code appViewerRole}</b> and application default role - Developer (if it exists) following
     * permissions for <b>{@code appViewerRole}</b>.
     * <li> {@link AclPermission}.{@code ASSIGN_PERMISSION_GROUPS}
     * <li> {@link AclPermission}.{@code UNASSIGN_PERMISSION_GROUPS}
     * <li> {@link AclPermission}.{@code READ_PERMISSION_GROUP_MEMBERS}
     * @param appViewerRole
     * @param application
     *
     * @implNote The return statement may look similar to the return statement of
     * {@linkplain ApplicationServiceImpl#generateAndUpdatePoliciesForDefaultDeveloperRole(PermissionGroup, Application)}
     * but there is a very minute difference, i.e., here {@code appViewerRole} is not coming from the
     * {@code appDeveloperRoleFlux} but from outside its context.
     */
    private Mono<PermissionGroup> generateAndUpdatePoliciesForDefaultViewerRole(PermissionGroup appViewerRole,
                                                                                Application application) {
        /*
         * Generate policy map using assign permission group.
         * This way, it will generate a map, which will contain policies related to assign, un-assign and read members
         * permissions which will be added to it's existing policy.
         */
        Map<String, Policy> policyMap = policyUtils.generatePolicyFromPermissionWithPermissionGroup(
                permissionGroupPermission.getAssignPermission(), appViewerRole.getId());
        policyUtils.addPoliciesToExistingObject(policyMap, appViewerRole);
        Flux<PermissionGroup> appDeveloperRoleFlux = permissionGroupRepository
                .findByDefaultDomainIdAndDefaultDomainType(application.getId(), Application.class.getSimpleName())
                .filter(role -> role.getDefaultDomainType().equals(Application.class.getSimpleName())
                        && role.getName().startsWith(APPLICATION_DEVELOPER))
                .cache();
        return appDeveloperRoleFlux.hasElements()
                .flatMap(isAppDeveloperRolePresent -> {
                    if (isAppDeveloperRolePresent) {
                        return appDeveloperRoleFlux.single()
                                .flatMap(developerRole -> {
                                    giveDevAppRolePermissionsToViewAppRole(developerRole, appViewerRole);
                                    return permissionGroupService.save(appViewerRole);
                                });
                    }
                    return Mono.empty();
                }).switchIfEmpty(permissionGroupService.save(appViewerRole));
    }

    /**
     * <p>
     * Method gives the application default role - Developer, access to following permissions for application default
     * role - App Viewer.
     * <ul>
     * <li> {@link AclPermission}.{@code ASSIGN_PERMISSION_GROUPS}
     * <li> {@link AclPermission}.{@code UNASSIGN_PERMISSION_GROUPS}
     * <li> {@link AclPermission}.{@code READ_PERMISSION_GROUP_MEMBERS}
     * </ul>
     * @param devRole {@link PermissionGroup}
     * @param viewRole {@link PermissionGroup}
     */
    private void giveDevAppRolePermissionsToViewAppRole(PermissionGroup devRole, PermissionGroup viewRole) {
        /*
         * Generate policy map using assign permission group.
         * This way, it will generate a map, which will contain policies related to assign, un-assign and read members
         * permissions which will be added to it's existing policy.
         */
        Map<String, Policy> policyMap = policyUtils.generatePolicyFromPermissionWithPermissionGroup(
                permissionGroupPermission.getAssignPermission(), devRole.getId());
        policyUtils.addPoliciesToExistingObject(policyMap, viewRole);
    }

    /**
     * Method gives default workspace roles permissions to assign, un-assign and read members permissions to default
     * application role.
     * Default workspace roles - Administrator and Developer are given permissions to default application roles - Developer and App Viewer.
     * Default workspace role - App Viewer is given permissions to default application role - App Viewer.
     * If the role doesn't begin with either <b>Developer</b> or <b>App Viewer</b>,
     * return an {@link AppsmithException} with Error as {@code UNSUPPORTED_OPERATION}.
     * @param workspaceId
     * @param role
     * @return
     */
    private Mono<PermissionGroup> giveDefaultWorkspaceRolesAccessToRole(String workspaceId,
                                                                        PermissionGroup role) {
        Flux<PermissionGroup> allDefaultWorkspaceRoles = permissionGroupRepository
                .findByDefaultDomainIdAndDefaultDomainType(workspaceId, Workspace.class.getSimpleName());
        /*
         * If the role is Application Developer Role, then we only give Workspace Admin and Developer roles, permissions to access it.
         * If the role is Application Viewer Role, then we give Workspace Admin / Developer / App Viewer roles, permissions to access it.
         */
        Flux<PermissionGroup> requiredDefaultWorkspaceRoles;
        if (role.getName().startsWith(APPLICATION_DEVELOPER)) {
            requiredDefaultWorkspaceRoles = allDefaultWorkspaceRoles.filter(role1 -> role1.getName().startsWith(ADMINISTRATOR) || role1.getName().startsWith(DEVELOPER))
                    .cache();
        } else if (role.getName().startsWith(APPLICATION_VIEWER)) {
            requiredDefaultWorkspaceRoles = allDefaultWorkspaceRoles;
        } else {
            return Mono.error(new AppsmithException(AppsmithError.UNSUPPORTED_OPERATION));
        }
        return requiredDefaultWorkspaceRoles.collectList()
                .map(roles -> {
                    Set<String> roleIds = roles.stream().map(PermissionGroup::getId).collect(Collectors.toSet());
                    /*
                     * Making a deep copy of policies, to avoid unnecessary changes which can be reflected in other policies
                     * because of the reason mentioned below.
                     * At times there is a possibility that the permissionGroups data member inside policy for different policies
                     * may have same reference. Due to this it is a possibility that the changes required for a certain policy
                     * may end up reflecting in a different policy as well.
                     */
                    Set<Policy> copyPolicies = role.getPolicies().stream().map(SerializationUtils::clone).collect(Collectors.toSet());
                    copyPolicies.stream()
                            .filter(policy -> policy.getPermission().equals(permissionGroupPermission.getAssignPermission().getValue())
                                    || policy.getPermission().equals(permissionGroupPermission.getUnAssignPermission().getValue())
                                    || policy.getPermission().equals(permissionGroupPermission.getMembersReadPermission().getValue()))
                            .toList()
                            .forEach(policy -> policy.getPermissionGroups().addAll(roleIds));
                    role.setPolicies(copyPolicies);
                    return role;
                });
    }

    private Mono<PermissionGroup> updatePoliciesForApplicationAndRelatedResources(Application application, PermissionGroup applicationRole, String applicationRoleType) {
        Map<String, List<AclPermission>> permissionListMap = getPermissionListMapForDefaultApplicationRole(applicationRoleType);
        Mono<Long> updateAllResourcesWithPermissionForRoleMono = Mono.just(1L);
        Mono<Long> updateApplicationAndRelatedResourcesWithPermissionsForRoleMono = roleConfigurationSolution
                .updateApplicationAndRelatedResourcesWithPermissionsForRole(application.getId(), applicationRole.getId(), permissionListMap, Map.of());
        Mono<Long> updateWorkspaceAndDatasourcesInWorkspaceWithPermissionsForRoleMono = roleConfigurationSolution
                .updateWorkspaceAndDatasourcesInWorkspaceWithPermissionsForRole(application.getWorkspaceId(), applicationRole.getId(), permissionListMap, Map.of());
        if (APPLICATION_DEVELOPER.equals(applicationRoleType)) {
            /*
             * Updating the resources in sequence, because some common datasources are being updated in both the Monos.
             * This is happening because we are in first mono, we are updating only datasources, which the application
             * is using. In second one, we are updating all the datasources present in the workspace.
             */
            updateAllResourcesWithPermissionForRoleMono = updateApplicationAndRelatedResourcesWithPermissionsForRoleMono
                    .then(updateWorkspaceAndDatasourcesInWorkspaceWithPermissionsForRoleMono)
                    .thenReturn(1L);
        } else if (APPLICATION_VIEWER.equals(applicationRoleType)) {
            updateAllResourcesWithPermissionForRoleMono = updateApplicationAndRelatedResourcesWithPermissionsForRoleMono;
        }
        return updateAllResourcesWithPermissionForRoleMono
                .thenReturn(applicationRole);
    }

    private Map<String, List<AclPermission>> getPermissionListMapForDefaultApplicationRole(String applicationRoleType) {
        AppsmithRole appsmithRole;
        if (applicationRoleType.equals(APPLICATION_DEVELOPER)) {
            appsmithRole = AppsmithRole.APPLICATION_DEVELOPER;
        } else if (applicationRoleType.equals(APPLICATION_VIEWER)) {
            appsmithRole = AppsmithRole.APPLICATION_VIEWER;
        } else {
            throw new AppsmithException(AppsmithError.UNSUPPORTED_OPERATION);
        }

        List<AclPermission> workspacePermissions = appsmithRole.getPermissions()
                .stream().filter(aclPermission -> aclPermission.getEntity().equals(Workspace.class))
                .toList();
        List<AclPermission> applicationPermissions = appsmithRole.getPermissions()
                .stream().filter(aclPermission -> aclPermission.getEntity().equals(Application.class))
                .toList();
        /*
         * Note: WORKSPACE_DATASOURCE_CREATE_DATASOURCE_ACTIONS (workspace permission) has a hierarchical relationship
         * with CREATE_DATASOURCE_ACTIONS (datasource permission), and is required by the application developer role so
         * that it has the permissions to create datasource actions in all the datasources within the workspace, we will
         * need to evaluate this special permission as an indirect datasource permission which is being given to the role.
         *
         * Also, in Application viewer role, we have a direct datasource permission EXECUTE_DATASOURCES.
         *
         * In order to keep the flow generic, we are separately calculating all direct and indirect permissions which
         * would be given to the datasource, and combine them.
         */
        List<AclPermission> directDatasourcePermissions = appsmithRole.getPermissions()
                .stream().filter(aclPermission -> aclPermission.getEntity().equals(Datasource.class))
                .toList();
        Set<AclPermission> indirectDatasourcePermissions = policyGenerator.getAllChildPermissions(workspacePermissions, Datasource.class);
        List<AclPermission> datasourcePermissions = new ArrayList<>();
        datasourcePermissions.addAll(directDatasourcePermissions);
        datasourcePermissions.addAll(indirectDatasourcePermissions);
        List<AclPermission> pagePermissions = policyGenerator.getAllChildPermissions(applicationPermissions, Page.class)
                .stream().toList();
        List<AclPermission> actionPermissions = policyGenerator.getAllChildPermissions(pagePermissions, Action.class)
                .stream().toList();


        return Map.of(Workspace.class.getSimpleName(), workspacePermissions,
                Application.class.getSimpleName(), applicationPermissions,
                Datasource.class.getSimpleName(), datasourcePermissions,
                NewPage.class.getSimpleName(), pagePermissions,
                NewAction.class.getSimpleName(), actionPermissions);
    }

    /**
     * The method is responsible for deleting a given default application role for application.
     * The role will be deleted if the role's defaultDomainId matches the application's ID, and
     * is either a Developer or App Viewer role.
     * Else it will return an {@link AppsmithException} with Error as {@code UNSUPPORTED_OPERATION}.
     * @param application Application for which the default role is being deleted.
     * @param role Role which is being deleted.
     */
    @Override
    public Mono<Void> deleteDefaultRole(Application application, PermissionGroup role) {
        if (StringUtils.isNotEmpty(role.getDefaultDomainId())
                && role.getDefaultDomainId().equals(application.getId())
                && (role.getName().startsWith(APPLICATION_VIEWER) || role.getName().startsWith(APPLICATION_DEVELOPER))) {
            return permissionGroupService.deleteWithoutPermission(role.getId());
        } else {
            return Mono.error(new AppsmithException(AppsmithError.UNSUPPORTED_OPERATION));
        }
    }

    /**
     * Returns all the default role types, the logged-in user has access to.
     * If the user has access to assign permission for either default workspace roles - Admin/Developer or default
     * application role - Developer, we return Developer and Viewer role types.
     * If the user has access to assign permission for either default workspace role - App Viewer or  default
     * application role - App Viewer, we return App Viewer role.
     * If none of the default roles are assigned to the user, then we return an empty list.
     * @param applicationId Application ID for which the role types would be fetched.
     * @return {@link Mono}<{@link List}<{@link PermissionGroupInfoDTO}>>
     */
    @Override
    public Mono<List<PermissionGroupInfoDTO>> fetchAllDefaultRoles(String applicationId) {
        Mono<Application> applicationMono = getById(applicationId)
                .cache();

        Flux<PermissionGroup> defaultApplicationRolesFlux = applicationMono
                .flatMapMany(application ->
                        permissionGroupService.getAllDefaultRolesForApplication(application,
                                Optional.of(permissionGroupPermission.getAssignPermission())));
        Flux<PermissionGroup> defaultWorkspaceRolesFlux = applicationMono
                .flatMapMany(application ->
                        permissionGroupService.getByDefaultWorkspaces(Set.of(application.getWorkspaceId()),
                                permissionGroupPermission.getAssignPermission()));


        // Based on default application roles it creates a set of static application roles.
        Mono<Set<String>> accessibleApplicationRolesFromDefaultApplicationRolesMono = defaultApplicationRolesFlux.collectList()
                .map(defaultApplicationRoles -> {
                    Set<String> staticApplicationRoles = new HashSet<>();
                    defaultApplicationRoles.stream().map(this::getAccessibleStaticApplicationRoles).forEach(staticApplicationRoles::addAll);
                    return staticApplicationRoles;
                });

        // Based on default workspace roles it creates a set of static workspace roles.
        Mono<Set<String>> accessibleApplicationRolesFromDefaultWorkspaceRolesMono = defaultWorkspaceRolesFlux.collectList()
                .map(defaultApplicationRoles -> {
                    Set<String> staticApplicationRoles = new HashSet<>();
                    defaultApplicationRoles.stream().map(this::getAccessibleStaticApplicationRoles).forEach(staticApplicationRoles::addAll);
                    return staticApplicationRoles;
                });

        /*
         * Here, all the static application roles from accessibleApplicationRolesFromDefaultApplicationRolesMono is calculated first.
         * If the static application roles does not contain both APPLICATION_DEVELOPER & APPLICATION_VIEWER, then
         * static application roles are calculated from accessibleApplicationRolesFromDefaultWorkspaceRolesMono, and
         * appended to the already existing set.
         *
         * This ensures that an extra call to DB is only made, if all the static roles are not present.
         */
        Mono<Set<String>> allAccessibleApplicationRolesMono = accessibleApplicationRolesFromDefaultApplicationRolesMono
                .flatMap(accessibleApplicationRoles -> {
                    if (!areAllStaticApplicationRolesPresent(accessibleApplicationRoles)) {
                        return accessibleApplicationRolesFromDefaultWorkspaceRolesMono
                                .map(accessibleApplicationRoles1 -> {
                                    accessibleApplicationRoles.addAll(accessibleApplicationRoles1);
                                    return accessibleApplicationRoles;
                                });
                    }
                    return Mono.just(accessibleApplicationRoles);
                });

        return allAccessibleApplicationRolesMono
                .zipWith(applicationMono)
                .map(tuple -> {
                    Set<String> roleSet = tuple.getT1();
                    Application application = tuple.getT2();
                    List<PermissionGroupInfoDTO> roleDescriptionDTOS = new ArrayList<>();
                    if (roleSet.contains(APPLICATION_DEVELOPER)) {
                        PermissionGroupInfoDTO roleDescriptionDTO = new PermissionGroupInfoDTO();
                        roleDescriptionDTO.setName(generateDefaultRoleNameForResource(APPLICATION_DEVELOPER, application.getName()));
                        roleDescriptionDTO.setDescription(APPLICATION_DEVELOPER_DESCRIPTION);
                        roleDescriptionDTO.setAutoCreated(Boolean.TRUE);
                        roleDescriptionDTOS.add(roleDescriptionDTO);
                    }
                    if (roleSet.contains(APPLICATION_VIEWER)) {
                        PermissionGroupInfoDTO roleDescriptionDTO = new PermissionGroupInfoDTO();
                        roleDescriptionDTO.setName(generateDefaultRoleNameForResource(APPLICATION_VIEWER, application.getName()));
                        roleDescriptionDTO.setDescription(APPLICATION_VIEWER_DESCRIPTION);
                        roleDescriptionDTO.setAutoCreated(Boolean.TRUE);
                        roleDescriptionDTOS.add(roleDescriptionDTO);
                    }
                    roleDescriptionDTOS.sort(permissionGroupInfoWithEntityTypeComparator());
                    return roleDescriptionDTOS;
                });
    }

    private HashSet<String> getAccessibleStaticApplicationRoles(PermissionGroup role) {
        Set<String> accessibleStaticRoles = Set.of();
        if ((role.getName().startsWith(APPLICATION_DEVELOPER) && role.getDefaultDomainType().equals(Application.class.getSimpleName()))
                || (role.getName().startsWith(ADMINISTRATOR) && role.getDefaultDomainType().equals(Workspace.class.getSimpleName()))
                || (role.getName().startsWith(DEVELOPER) && role.getDefaultDomainType().equals(Workspace.class.getSimpleName()))) {
            accessibleStaticRoles = Set.of(APPLICATION_DEVELOPER, APPLICATION_VIEWER);
        } else if ((role.getName().startsWith(APPLICATION_VIEWER) && role.getDefaultDomainType().equals(Application.class.getSimpleName()))
                || (role.getName().startsWith(VIEWER) && role.getDefaultDomainType().equals(Workspace.class.getSimpleName()))) {
            accessibleStaticRoles = Set.of(APPLICATION_VIEWER);
        }
        return new HashSet<>(accessibleStaticRoles);
    }

    private boolean areAllStaticApplicationRolesPresent(Set<String> staticDefaultRoles) {
        return staticDefaultRoles.containsAll(Set.of(APPLICATION_DEVELOPER, APPLICATION_VIEWER));
    }

    /**
     * The method is responsible for updating the application.
     * It also updates the names of default application roles which are associated with the application, if name of the
     * application is changed.
     * @param applicationId ID of the application to be updated.
     * @param application Resources to update.
     * @param branchName updates application in a particular branch.
     * @return
     */
    @Override
    public Mono<Application> update(String applicationId, Application application, String branchName) {
        Mono<Application> updateApplicationMono = super.update(applicationId, application, branchName);
        if (StringUtils.isEmpty(application.getName())) {
            return updateApplicationMono;
        }
        String newApplicationName = application.getName();
        return updateApplicationMono
                .flatMap(application1 -> {
                    /*
                     * Here we check if the application which has been updated is the application from default branch, or not.
                     * If the application is from any other branch other than the default branch, we don't update
                     * the names of default application role.
                     */
                    if (! isDefaultBranchApplication(application1)) {
                        return Mono.just(application1);
                    }
                    Flux<PermissionGroup> defaultApplicationRoles = permissionGroupService
                            .getAllDefaultRolesForApplication(application1, Optional.empty());
                    Flux<PermissionGroup> updateDefaultApplicationRoles = defaultApplicationRoles
                            .flatMap(role -> {
                                role.setName(generateNewDefaultName(role.getName(), newApplicationName));
                                return permissionGroupService.save(role);
                            });
                    return updateDefaultApplicationRoles.then(Mono.just(application1));
                });
    }

    private String generateNewDefaultName(String oldName, String applicationName) {
        if (oldName.startsWith(APPLICATION_DEVELOPER)) {
            return generateDefaultRoleNameForResource(APPLICATION_DEVELOPER, applicationName);
        } else if (oldName.startsWith(APPLICATION_VIEWER)) {
            return generateDefaultRoleNameForResource(APPLICATION_VIEWER, applicationName);
        }
        // If this is not a default group i.e. does not start with the expected prefix, don't update it.
        return oldName;
    }

    private boolean isDefaultBranchApplication(Application application) {
        return Objects.isNull(application.getGitApplicationMetadata())
                || application.getGitApplicationMetadata().getDefaultApplicationId().equals(application.getId());

    }

    /**
     * The method is responsible for inviting users and user groups to a specific application.
     * This will also create User for usernames, if they don't already exist in the appsmith ecosystem.
     * <br>
     * Restrictions:
     * <ol>
     *     <li>Both usernames and groupsIds can't be null. One of them should be non-empty.</li>
     *     <li>applicationId can't be empty</li>
     *     <li>roleType should be either App Viewer or Developer</li>
     * </ol>
     * @param inviteToApplicationDTO
     * @return {@link Mono}<{@link List}<{@link MemberInfoDTO}>> which contains details about the invited users and
     * user groups who have been invited.
     */
    @Override
    public Mono<List<MemberInfoDTO>> inviteToApplication(InviteUsersToApplicationDTO inviteToApplicationDTO) {
        Set<String> usernames = inviteToApplicationDTO.getUsernames();
        Set<String> groupIds = inviteToApplicationDTO.getGroups();
        String applicationId = inviteToApplicationDTO.getApplicationId();
        String appRoleType = inviteToApplicationDTO.getRoleType();


        if (CollectionUtils.isEmpty(usernames) && CollectionUtils.isEmpty(groupIds)) {
            return Mono.error(new AppsmithException(AppsmithError.INVALID_PARAMETER, USERNAME + " or " + GROUP_ID));
        }

        if (StringUtils.isEmpty(appRoleType)
                || !(appRoleType.equals(APPLICATION_VIEWER) ||  appRoleType.equals(APPLICATION_DEVELOPER))) {
            return Mono.error(new AppsmithException(AppsmithError.INVALID_PARAMETER, ROLE));
        }

        if (StringUtils.isEmpty(applicationId)){
            return Mono.error(new AppsmithException(AppsmithError.INVALID_PARAMETER, FieldName.APPLICATION));
        }

        Mono<Application> applicationMono = findById(applicationId);
        Mono<PermissionGroup> defaultAppRoleMono = getOrCreateDefaultAppRole(applicationId, appRoleType);

        /*
         * We are initialising the User and UserGroup Mono Lists with empty lists, so that they can be zipped with
         * other non-empty Mono, without any hidden Mono.empty() being returned.
         */
        Mono<List<User>> userListMono = Mono.just(List.of());
        Mono<List<UserGroup>> groupListMono = Mono.just(List.of());
        Mono<Long> sendInviteUsersToApplicationEvent = Mono.just(1L);

        if (CollectionUtils.isNotEmpty(usernames)) {
            userListMono = Flux.fromIterable(usernames)
                    .flatMap(username -> {
                        User newUser = new User();
                        newUser.setEmail(username.toLowerCase());
                        newUser.setIsEnabled(false);
                        /* TODO: Use method createNewUserAndSendInviteEmail instead of userCreate once the Email templates are ready.
                         * Note: Currently we are just creating a non-admin user if the username already doesn't exist.
                         * But going forward we will be receiving email templates for inviting users to applications,
                         * who already don't exist on the instance. We will need to update the above mentioned method,
                         * and use that to create users and send appropriate mails.
                         */
                        return userService.findByEmail(username)
                                .switchIfEmpty(userService.userCreate(newUser, false));
                    })
                    .collectList()
                    .cache();
            sendInviteUsersToApplicationEvent = Mono.zip(applicationMono, sessionUserService.getCurrentUser())
                    .flatMap(tuple -> sendEventInviteUsersToApplication(tuple.getT2(), usernames, tuple.getT1()))
                    .thenReturn(1L);
        }

        if (CollectionUtils.isNotEmpty(groupIds)) {
            groupListMono = userGroupRepository.findAllById(groupIds).collectList().cache();
        }

        Mono<PermissionGroup> roleAfterInvitation = Mono.zip(defaultAppRoleMono, userListMono, groupListMono)
                .flatMap(tuple -> permissionGroupService.bulkAssignToUsersAndGroups(tuple.getT1(), tuple.getT2(), tuple.getT3()));


        Mono<List<MemberInfoDTO>> invitedMembersListMono = Mono.zip(roleAfterInvitation, userListMono, groupListMono)
                .map(tuple -> {
                    PermissionGroup role = tuple.getT1();
                    PermissionGroupInfoDTO roleInfoDTO = new PermissionGroupInfoDTO(role.getId(), role.getName(),
                            role.getDescription(), role.getDefaultDomainId(), role.getDefaultDomainType(), null);
                    List<MemberInfoDTO> userMembers = tuple.getT2().stream().map(user -> MemberInfoDTO.builder()
                            .username(user.getUsername()).userId(user.getId()).name(user.getName())
                            .roles(List.of(roleInfoDTO))
                            .build()).toList();
                    List<MemberInfoDTO> groupMembers = tuple.getT3().stream().map(group -> MemberInfoDTO.builder()
                            .userGroupId(group.getId()).name(group.getName())
                            .roles(List.of(roleInfoDTO))
                            .build()).toList();

                    return Stream.of(userMembers, groupMembers)
                            .flatMap(Collection::stream).toList();
                });

        return invitedMembersListMono
                .flatMap(sendInviteUsersToApplicationEvent::thenReturn);
    }

    private Mono<User> sendEventInviteUsersToApplication(User currentUser, Set<String> invitedUserEmails, Application application) {
        Map<String, Object> analyticsProperties = new HashMap<>();
        long numberOfUsers = invitedUserEmails.size();
        analyticsProperties.put(FieldName.NUMBER_OF_USERS_INVITED, numberOfUsers);
        Map<String, Object> eventData = Map.of(FieldName.USER_EMAILS, invitedUserEmails,
                FieldName.APPLICATION, application.getName());
        Map<String, Object> extraPropsForCloudHostedInstance = Map.of(FieldName.USER_EMAILS, invitedUserEmails,
                FieldName.APPLICATION, application.getName());
        analyticsProperties.put(FieldName.EVENT_DATA, eventData);
        analyticsProperties.put(FieldName.CLOUD_HOSTED_EXTRA_PROPS, extraPropsForCloudHostedInstance);
        return analyticsService.sendObjectEvent(AnalyticsEvents.EXECUTE_INVITE_USERS, currentUser, analyticsProperties);
    }

    /**
     * The method will either get or create a default application role, based on applicationId and roleType.
     * Firstly we check if the role with the requested roleType exists for the applicationId. This is done, with the
     * assign permission filter, which will check if the user has the ability to assign the role or not.
     * We such a role exists, we return the role as is.
     * Else, we fetch all the static application roles the user has access to.
     * With these 2 conditions, we can determine whether the role needs to be created, or not and throw an error that
     * the user doesn't have permission to invite a user.
     * @param applicationId
     * @param roleType
     * @return
     */
    private Mono<PermissionGroup> getOrCreateDefaultAppRole(String applicationId, String roleType) {
        if (StringUtils.isEmpty(roleType)
                || !(roleType.equals(APPLICATION_VIEWER) ||  roleType.equals(APPLICATION_DEVELOPER))) {
            return Mono.error(new AppsmithException(AppsmithError.INVALID_PARAMETER, ROLE));
        }
        Mono<Application> applicationMono = findById(applicationId, Optional.of(applicationPermission.getReadPermission()))
                .switchIfEmpty(Mono.error(new AppsmithException(AppsmithError.ACL_NO_RESOURCE_FOUND, applicationId)))
                .cache();
        Flux<PermissionGroup> defaultAppRoleFlux = applicationMono
                .flatMapMany(application -> permissionGroupService
                        .getAllDefaultRolesForApplication(application, Optional.of(permissionGroupPermission.getAssignPermission())))
                .filter(role -> role.getName().startsWith(roleType))
                .cache();
        return defaultAppRoleFlux.hasElements()
                .flatMap(defaultAppRoleExist -> {
                    if (defaultAppRoleExist) {
                        return defaultAppRoleFlux.single();
                    }

                    // This will get a list of all static default application roles, the user has access to.
                    Mono<List<PermissionGroupInfoDTO>> userAssignableStaticApplicationRolesMono = fetchAllDefaultRoles(applicationId);
                    return Mono.zip(userAssignableStaticApplicationRolesMono, applicationMono)
                            .flatMap(tuple -> {
                                List<PermissionGroupInfoDTO> staticApplicationRoles = tuple.getT1();
                                Application application = tuple.getT2();
                                boolean requiredApplicationRoleCanBeCreated = staticApplicationRoles.stream()
                                        .anyMatch(staticRole -> staticRole.getName().startsWith(roleType));
                                if (requiredApplicationRoleCanBeCreated) {
                                    return createDefaultRole(application, roleType);
                                }
                                return Mono.error(new AppsmithException(AppsmithError.UNAUTHORIZED_ACCESS));
                            });
                });
    }

    /**
     * The method is used to update the default application roles for default application members.
     * Also, once the member has been unassigned from the oldRole, then if the oldRole, doesn't contain any more members
     * to which it has been assigned, then oldRole is deleted.
     * <br>
     * Restrictions:
     * <ol>
     *     <li>Both username and groupsId can't be null. One of them should be non-empty.</li>
     *     <li>oldRole should not be empty and have one of the 2 values: App Viewer or Developer</li>
     *     <li>If newRole is not empty, it should either be App Viewer or Developer</li>
     * </ol>
     * @param applicationId
     * @param updateApplicationRoleDTO
     * @return {@link Mono}<{@link MemberInfoDTO}> updated member info
     */
    @Override
    public Mono<MemberInfoDTO> updateRoleForMember(String applicationId, UpdateApplicationRoleDTO updateApplicationRoleDTO) {
        String username = updateApplicationRoleDTO.getUsername();
        String groupId = updateApplicationRoleDTO.getUserGroupId();
        String newRole = updateApplicationRoleDTO.getNewRole();

        if (StringUtils.isNotEmpty(newRole) &&
                !(newRole.equals(APPLICATION_DEVELOPER) || newRole.equals(APPLICATION_VIEWER))) {
            return Mono.error(new AppsmithException(AppsmithError.INVALID_PARAMETER, "New " + ROLE));
        }

        if ((StringUtils.isEmpty(username) && StringUtils.isEmpty(groupId))
                || (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(groupId))) {
            String errorString = "Either" + USERNAME + " or " + GROUP_ID + " should be present.";
            return Mono.error(new AppsmithException(AppsmithError.INVALID_PARAMETER, errorString));
        }

        Mono<PermissionGroup> unAssignUserFromOldRole_deleteRoleIfRequired_thenRole = Mono.empty();
        Mono<PermissionGroup> unAssignGroupFromOldRole_deleteRoleIfRequired_thenRole = Mono.empty();
        MemberInfoDTO memberInfoForUnassignedMember = MemberInfoDTO.builder().build();

        if (StringUtils.isNotEmpty(username)) {
            Mono<User> userMono = userService.findByEmail(username)
                    .switchIfEmpty(Mono.error(new AppsmithException(AppsmithError.NO_RESOURCE_FOUND, FieldName.USER, username)))
                    .cache();
            Mono<PermissionGroup> oldDefaultRoleMono = userMono
                    .flatMap(user -> permissionGroupRepository.findAllByAssignedToUserIdAndDefaultDomainIdAndDefaultDomainType(user.getId(), applicationId, Application.class.getSimpleName(), Optional.of(permissionGroupPermission.getAssignPermission()))
                            .switchIfEmpty(Mono.error(new AppsmithException(AppsmithError.ACTION_IS_NOT_AUTHORIZED, "No application role assigned")))
                            .single());
            unAssignUserFromOldRole_deleteRoleIfRequired_thenRole = oldDefaultRoleMono
                    .zipWith(userMono)
                    .flatMap(pair -> {
                        User user = pair.getT2();
                        memberInfoForUnassignedMember.setUserId(user.getId());
                        memberInfoForUnassignedMember.setUsername(user.getUsername());
                        memberInfoForUnassignedMember.setName(user.getName());
                        return permissionGroupService.unAssignFromUserAndSendEvent(pair.getT1(), pair.getT2());
                    })
                    .flatMap(this::deleteDefaultRoleIfNoUserOrUserGroupAssigned);
        } else if (StringUtils.isNotEmpty(groupId)) {
            Mono<PermissionGroup> oldDefaultRoleMono = permissionGroupRepository.findAllByAssignedToGroupIdAndDefaultDomainIdAndDefaultDomainType(groupId, applicationId, Application.class.getSimpleName(), Optional.of(permissionGroupPermission.getAssignPermission()))
                            .switchIfEmpty(Mono.error(new AppsmithException(AppsmithError.ACTION_IS_NOT_AUTHORIZED, "No application role assigned")))
                            .single();
            Mono<UserGroup> groupMono = userGroupRepository.findById(groupId)
                    .switchIfEmpty(Mono.error(new AppsmithException(AppsmithError.NO_RESOURCE_FOUND, FieldName.USER_GROUP, groupId)))
                    .cache();
            unAssignGroupFromOldRole_deleteRoleIfRequired_thenRole = oldDefaultRoleMono
                    .zipWith(groupMono)
                    .flatMap(pair -> {
                        UserGroup userGroup = pair.getT2();
                        memberInfoForUnassignedMember.setUserGroupId(userGroup.getId());
                        memberInfoForUnassignedMember.setName(userGroup.getName());
                        return permissionGroupService.unAssignFromUserGroupAndSendEvent(pair.getT1(), pair.getT2());
                    })
                    .flatMap(this::deleteDefaultRoleIfNoUserOrUserGroupAssigned);
        }

        Mono<Long> oldDefaultRolePostUnAssignAndDeleteIfRequired = Mono
                .when(unAssignUserFromOldRole_deleteRoleIfRequired_thenRole, unAssignGroupFromOldRole_deleteRoleIfRequired_thenRole)
                .thenReturn(1L);

        Mono<List<MemberInfoDTO>> invitedToNewRoleMono = Mono.empty();
        if (StringUtils.isNotEmpty(newRole)) {
            InviteUsersToApplicationDTO inviteToApplicationDTO = new InviteUsersToApplicationDTO();
            inviteToApplicationDTO.setApplicationId(applicationId);
            inviteToApplicationDTO.setRoleType(newRole);
            if (StringUtils.isNotEmpty(username)) {
                inviteToApplicationDTO.setUsernames(Set.of(username));
            }
            else if (StringUtils.isNotEmpty(groupId)) {
                inviteToApplicationDTO.setGroups(Set.of(groupId));
            }
            invitedToNewRoleMono = this.inviteToApplication(inviteToApplicationDTO);
        }

        return Mono.when(oldDefaultRolePostUnAssignAndDeleteIfRequired)
                .then(invitedToNewRoleMono
                        .map(invitedToNewRole -> invitedToNewRole.stream().findFirst().get()))
                .switchIfEmpty(Mono.just(memberInfoForUnassignedMember));
    }

    /**
     * The method deletes the role, if there are no users or user groups to which the defaultRole has been assigned.
     * @param defaultRole
     * @return
     */
    private Mono<PermissionGroup> deleteDefaultRoleIfNoUserOrUserGroupAssigned(PermissionGroup defaultRole) {
        Mono<PermissionGroup> roleMono = permissionGroupService.findById(defaultRole.getId());
        return roleMono
                .flatMap(role -> {
                    if (CollectionUtils.isEmpty(role.getAssignedToUserIds())
                            && CollectionUtils.isEmpty(role.getAssignedToGroupIds())) {
                        return permissionGroupService.deleteWithoutPermission(role.getId()).thenReturn(role);
                    }
                    return Mono.just(role);
                });
    }

    /**
     * The method returns a hard coded list of all application default roles.
     * Note: We haven't used List.of() and instead used an ArrayList, because we are sorting the order of the roles.
     * @return
     */
    @Override
    public Mono<List<PermissionGroupInfoDTO>> fetchAllDefaultRolesWithoutPermissions() {
            List<PermissionGroupInfoDTO> roleDescriptionDTOS = new ArrayList<>();

            PermissionGroupInfoDTO roleDescriptionDTODeveloper = new PermissionGroupInfoDTO();
            roleDescriptionDTODeveloper.setName(APPLICATION_DEVELOPER);
            roleDescriptionDTODeveloper.setDescription(APPLICATION_DEVELOPER_DESCRIPTION);
            roleDescriptionDTODeveloper.setAutoCreated(Boolean.TRUE);
            roleDescriptionDTOS.add(roleDescriptionDTODeveloper);

            PermissionGroupInfoDTO roleDescriptionDTOViewer = new PermissionGroupInfoDTO();
            roleDescriptionDTOViewer.setName(APPLICATION_VIEWER);
            roleDescriptionDTOViewer.setDescription(APPLICATION_VIEWER_DESCRIPTION);
            roleDescriptionDTOViewer.setAutoCreated(Boolean.TRUE);
            roleDescriptionDTOS.add(roleDescriptionDTOViewer);

            roleDescriptionDTOS.sort(permissionGroupInfoWithEntityTypeComparator());

            return Mono.just(roleDescriptionDTOS);
    }
}
