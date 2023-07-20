package com.appsmith.server.services;

import com.appsmith.external.constants.AnalyticsEvents;
import com.appsmith.external.models.Policy;
import com.appsmith.server.acl.AclPermission;
import com.appsmith.server.acl.PolicyGenerator;
import com.appsmith.server.constants.FieldName;
import com.appsmith.server.domains.PermissionGroup;
import com.appsmith.server.domains.ProvisionResourceMetadata;
import com.appsmith.server.domains.QUserGroup;
import com.appsmith.server.domains.Tenant;
import com.appsmith.server.domains.User;
import com.appsmith.server.domains.UserData;
import com.appsmith.server.domains.UserGroup;
import com.appsmith.server.dtos.PagedDomain;
import com.appsmith.server.dtos.PermissionGroupInfoDTO;
import com.appsmith.server.dtos.ProvisionResourceDto;
import com.appsmith.server.dtos.UpdateGroupMembershipDTO;
import com.appsmith.server.dtos.UserCompactDTO;
import com.appsmith.server.dtos.UserGroupCompactDTO;
import com.appsmith.server.dtos.UserGroupDTO;
import com.appsmith.server.dtos.UsersForGroupDTO;
import com.appsmith.server.enums.ProvisionStatus;
import com.appsmith.server.exceptions.AppsmithError;
import com.appsmith.server.exceptions.AppsmithException;
import com.appsmith.server.helpers.AppsmithComparators;
import com.appsmith.server.helpers.PermissionGroupUtils;
import com.appsmith.server.helpers.ProvisionUtils;
import com.appsmith.server.helpers.UserUtils;
import com.appsmith.server.repositories.UserDataRepository;
import com.appsmith.server.repositories.UserGroupRepository;
import com.appsmith.server.repositories.UserRepository;
import com.appsmith.server.solutions.PolicySolution;
import jakarta.validation.Validator;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.appsmith.server.acl.AclPermission.ADD_USERS_TO_USER_GROUPS;
import static com.appsmith.server.acl.AclPermission.CREATE_USER_GROUPS;
import static com.appsmith.server.acl.AclPermission.DELETE_USER_GROUPS;
import static com.appsmith.server.acl.AclPermission.MANAGE_USER_GROUPS;
import static com.appsmith.server.acl.AclPermission.READ_USER_GROUPS;
import static com.appsmith.server.acl.AclPermission.REMOVE_USERS_FROM_USER_GROUPS;
import static com.appsmith.server.constants.Constraint.NO_RECORD_LIMIT;
import static com.appsmith.server.constants.FieldName.EVENT_DATA;
import static com.appsmith.server.constants.FieldName.GROUP_ID;
import static com.appsmith.server.constants.FieldName.NUMBER_OF_REMOVED_USERS;
import static com.appsmith.server.constants.FieldName.NUMBER_OF_USERS_INVITED;
import static com.appsmith.server.constants.QueryParams.COUNT;
import static com.appsmith.server.constants.QueryParams.FILTER_DELIMITER;
import static com.appsmith.server.constants.QueryParams.GROUP_NAME_FILTER;
import static com.appsmith.server.constants.QueryParams.GROUP_USERID_FILTER;
import static com.appsmith.server.constants.QueryParams.START_INDEX;
import static com.appsmith.server.constants.ce.FieldNameCE.CLOUD_HOSTED_EXTRA_PROPS;
import static com.appsmith.server.dtos.UsersForGroupDTO.validate;
import static com.appsmith.server.enums.ProvisionResourceType.GROUP;
import static com.appsmith.server.helpers.CollectionUtils.findSymmetricDiff;
import static com.appsmith.server.repositories.ce.BaseAppsmithRepositoryCEImpl.fieldName;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Service
public class UserGroupServiceImpl extends BaseService<UserGroupRepository, UserGroup, String>
        implements UserGroupService {

    private final SessionUserService sessionUserService;
    private final TenantService tenantService;
    private final PolicyGenerator policyGenerator;
    private final PermissionGroupService permissionGroupService;

    private final UserService userService;

    private final ModelMapper modelMapper;
    private final PermissionGroupUtils permissionGroupUtils;
    private final UserDataRepository userDataRepository;
    private final UserUtils userUtils;
    private final PolicySolution policySolution;
    private final UserRepository userRepository;
    private final ProvisionUtils provisionUtils;

    public UserGroupServiceImpl(
            Scheduler scheduler,
            Validator validator,
            MongoConverter mongoConverter,
            ReactiveMongoTemplate reactiveMongoTemplate,
            UserGroupRepository repository,
            AnalyticsService analyticsService,
            SessionUserService sessionUserService,
            TenantService tenantService,
            PolicyGenerator policyGenerator,
            PermissionGroupService permissionGroupService,
            UserService userService,
            ModelMapper modelMapper,
            PermissionGroupUtils permissionGroupUtils,
            UserDataRepository userDataRepository,
            UserUtils userUtils,
            PolicySolution policySolution,
            UserRepository userRepository,
            ProvisionUtils provisionUtils) {
        super(scheduler, validator, mongoConverter, reactiveMongoTemplate, repository, analyticsService);
        this.sessionUserService = sessionUserService;
        this.tenantService = tenantService;
        this.policyGenerator = policyGenerator;
        this.permissionGroupService = permissionGroupService;
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.permissionGroupUtils = permissionGroupUtils;
        this.userDataRepository = userDataRepository;
        this.userUtils = userUtils;
        this.policySolution = policySolution;
        this.userRepository = userRepository;
        this.provisionUtils = provisionUtils;
    }

    @Override
    public Flux<UserGroup> get(MultiValueMap<String, String> params) {
        return this.getAll(READ_USER_GROUPS, params).sort(AppsmithComparators.userGroupComparator());
    }

    private Flux<UserGroup> getAll(AclPermission aclPermission, MultiValueMap<String, String> queryParams) {
        return tenantService
                .getDefaultTenant()
                .flatMapMany(defaultTenantId ->
                        repository.findAllByTenantId(defaultTenantId.getId(), queryParams, aclPermission));
    }

    @Override
    public Mono<List<UserGroupCompactDTO>> getAllWithAddUserPermission() {
        return this.getAll(ADD_USERS_TO_USER_GROUPS, new LinkedMultiValueMap<>())
                .map(this::generateUserGroupCompactDTO)
                .collectList();
    }

    @Override
    public Mono<List<UserGroupCompactDTO>> getAllReadableGroups() {
        return this.getAll(READ_USER_GROUPS, new LinkedMultiValueMap<>())
                .map(this::generateUserGroupCompactDTO)
                .collectList();
    }

    @Override
    public Mono<UserGroupDTO> createGroup(UserGroup userGroup) {
        return createUserGroup(userGroup).flatMap(savedUserGroup -> getGroupById(savedUserGroup.getId()));
    }

    @Override
    public Mono<UserGroupDTO> updateGroup(String id, UserGroup resource) {
        return repository
                .findById(id, MANAGE_USER_GROUPS)
                .switchIfEmpty(
                        Mono.error(new AppsmithException(AppsmithError.ACTION_IS_NOT_AUTHORIZED, "update user groups")))
                .flatMap(userGroup -> {
                    // The update API should only update the name and description of the group. The fields should not be
                    // updated using this function.
                    userGroup.setName(resource.getName());
                    userGroup.setDescription(resource.getDescription());
                    return super.update(id, userGroup);
                })
                .flatMap(savedUserGroup -> getGroupById(savedUserGroup.getId()));
    }

    @Override
    public Mono<UserGroup> getById(String id) {
        return Mono.error(new AppsmithException(AppsmithError.UNSUPPORTED_OPERATION));
    }

    @Override
    public Mono<UserGroupDTO> getGroupById(String id) {

        if (id == null) {
            return Mono.error(new AppsmithException(AppsmithError.INVALID_PARAMETER, FieldName.ID));
        }

        return this.getGroupDTOById(id, READ_USER_GROUPS);
    }

    private Mono<UserGroupDTO> getGroupDTOById(String id, AclPermission permission) {
        return repository.findById(id, permission).flatMap(userGroup -> {
            Mono<List<PermissionGroupInfoDTO>> groupRolesMono = getRoleDTOsForTheGroup(id);
            Mono<List<UserCompactDTO>> usersMono =
                    getUsersCompactForTheGroup(userGroup).cache();
            Mono<Map<String, UserData>> userIdUserDataMapMono = usersMono.flatMap(users -> {
                List<String> userIds = users.stream().map(UserCompactDTO::getId).toList();
                return userDataRepository.findPhotoAssetsByUserIds(userIds).collectMap(UserData::getUserId);
            });

            return Mono.zip(groupRolesMono, usersMono, userIdUserDataMapMono).flatMap(tuple -> {
                List<PermissionGroupInfoDTO> rolesInfoList = tuple.getT1();
                List<UserCompactDTO> usersList = tuple.getT2();
                Map<String, UserData> userIdUserDataMap = tuple.getT3();
                usersList.forEach(user -> {
                    String userId = user.getId();
                    if (userIdUserDataMap.containsKey(userId)
                            && StringUtils.hasLength(
                                    userIdUserDataMap.get(userId).getProfilePhotoAssetId())) {
                        user.setPhotoId(userIdUserDataMap.get(userId).getProfilePhotoAssetId());
                    }
                });
                return generateUserGroupDTO(userGroup, rolesInfoList, usersList);
            });
        });
    }

    private Mono<UserGroupDTO> generateUserGroupDTO(
            UserGroup userGroup, List<PermissionGroupInfoDTO> rolesInfoList, List<UserCompactDTO> usersList) {

        UserGroupDTO userGroupDTO = new UserGroupDTO();
        modelMapper.map(userGroup, userGroupDTO);
        userGroupDTO.setRoles(rolesInfoList);
        userGroupDTO.setUsers(usersList);
        userGroupDTO.populateTransientFields(userGroup);
        return Mono.just(userGroupDTO);
    }

    private Mono<List<UserCompactDTO>> getUsersCompactForTheGroup(UserGroup userGroup) {
        return userService
                .findAllByIdsIn(userGroup.getUsers())
                .map(user -> {
                    UserCompactDTO userDTO = new UserCompactDTO();
                    modelMapper.map(user, userDTO);
                    return userDTO;
                })
                .collectList();
    }

    private Mono<List<PermissionGroupInfoDTO>> getRoleDTOsForTheGroup(String userGroupId) {
        return permissionGroupUtils
                .mapToPermissionGroupInfoDto(permissionGroupService.findAllByAssignedToGroupIdsIn(Set.of(userGroupId)))
                .collectList();
    }

    @Override
    public Mono<List<UserGroupDTO>> inviteUsers(UsersForGroupDTO inviteUsersToGroupDTO, String originHeader) {

        Set<String> ids = inviteUsersToGroupDTO.getGroupIds();
        Set<String> usernames = inviteUsersToGroupDTO.getUsernames();

        return validate(inviteUsersToGroupDTO)
                // Now that we have validated the input, we can start the process of adding users to the group.
                .flatMapMany(bool -> repository.findAllByIds(ids, ADD_USERS_TO_USER_GROUPS))
                .switchIfEmpty(
                        Mono.error(new AppsmithException(AppsmithError.ACTION_IS_NOT_AUTHORIZED, "add users to group")))
                .collectList()
                .flatMap(userGroups -> {
                    Mono<Set<String>> toBeAddedUserIdsMono = Flux.fromIterable(usernames)
                            .flatMap(username -> {
                                User newUser = new User();
                                newUser.setEmail(username.toLowerCase());
                                newUser.setIsEnabled(false);
                                return userService
                                        .findByEmail(username)
                                        .switchIfEmpty(userService.userCreate(newUser, false));
                            })
                            .map(User::getId)
                            .collect(Collectors.toSet())
                            .cache();

                    // add the users to the group
                    // TODO : Add handling for sending emails intimating the users about the invite.
                    Flux<UserGroup> updateUsersInGroupsMono = Flux.fromIterable(userGroups)
                            .zipWith(toBeAddedUserIdsMono.repeat())
                            .flatMap(tuple -> {
                                UserGroup userGroup = tuple.getT1();
                                Set<String> userIds = tuple.getT2();
                                userGroup.getUsers().addAll(userIds);
                                return repository.save(userGroup);
                            })
                            .flatMap(userGroup -> {
                                Map<String, Object> eventData =
                                        Map.of(FieldName.INVITED_USERS_TO_USER_GROUPS, usernames);
                                Map<String, Object> extraPropsForCloudHostedInstance =
                                        Map.of(FieldName.INVITED_USERS_TO_USER_GROUPS, usernames);
                                Map<String, Object> analyticsProperties = Map.of(
                                        NUMBER_OF_USERS_INVITED, usernames.size(),
                                        EVENT_DATA, eventData,
                                        CLOUD_HOSTED_EXTRA_PROPS, extraPropsForCloudHostedInstance);
                                return analyticsService.sendObjectEvent(
                                        AnalyticsEvents.INVITE_USERS_TO_USER_GROUPS, userGroup, analyticsProperties);
                            })
                            .cache();

                    Flux<PermissionGroup> userGroupRolesFlux = permissionGroupService
                            .findAllByAssignedToGroupIdsIn(ids)
                            .cache();

                    // Get roles for the group, and if there are any, then invalidate the cache for the newly added
                    // users
                    Mono<Boolean> invalidateCacheOfUsersMono = userGroupRolesFlux
                            .next()
                            .zipWith(toBeAddedUserIdsMono)
                            .flatMap(tuple -> {
                                Set<String> newlyAddedUserIds = tuple.getT2();
                                return permissionGroupService.cleanPermissionGroupCacheForUsers(
                                        new ArrayList<>(newlyAddedUserIds));
                            })
                            .thenReturn(TRUE);

                    Mono<List<PermissionGroupInfoDTO>> rolesInfoMono = permissionGroupUtils
                            .mapToPermissionGroupInfoDto(userGroupRolesFlux)
                            .collectList()
                            // In case there are no roles associated with the group, then return an empty list.
                            .switchIfEmpty(Mono.just(new ArrayList<>()));

                    Mono<Map<String, List<UserCompactDTO>>> usersInGroupMapMono = updateUsersInGroupsMono
                            .flatMap(updatedUserGroup -> getUsersCompactForTheGroup(updatedUserGroup)
                                    .map(usersList -> Tuples.of(updatedUserGroup.getId(), usersList)))
                            .collectMap(tuple -> tuple.getT1(), tuple -> tuple.getT2());

                    return Mono.zip(invalidateCacheOfUsersMono, rolesInfoMono, usersInGroupMapMono)
                            .flatMap(tuple -> {
                                List<PermissionGroupInfoDTO> rolesInfoList = tuple.getT2();
                                Map<String, List<UserCompactDTO>> usersInGroupMap = tuple.getT3();
                                return Flux.fromIterable(userGroups)
                                        .flatMap(userGroup -> {
                                            List<UserCompactDTO> usersList = usersInGroupMap.get(userGroup.getId());
                                            return generateUserGroupDTO(userGroup, rolesInfoList, usersList);
                                        })
                                        .collectList();
                            });
                });
    }

    @Override
    public Mono<List<UserGroupDTO>> removeUsers(UsersForGroupDTO removeUsersFromGroupDTO) {

        Set<String> ids = removeUsersFromGroupDTO.getGroupIds();

        if (CollectionUtils.isEmpty(ids)) {
            return Mono.error(new AppsmithException(AppsmithError.INVALID_PARAMETER, GROUP_ID));
        }

        Set<String> usernames = removeUsersFromGroupDTO.getUsernames();

        return validate(removeUsersFromGroupDTO)
                // Now that we have validated the input, we can start the process of removing users from the group.
                .flatMapMany(bool -> repository.findAllByIds(ids, REMOVE_USERS_FROM_USER_GROUPS))
                .switchIfEmpty(Mono.error(
                        new AppsmithException(AppsmithError.ACTION_IS_NOT_AUTHORIZED, "remove users from group")))
                .collectList()
                .flatMap(userGroups -> {
                    Mono<Set<String>> toBeRemovedUserIdsMono = userService
                            .findAllByUsernameIn(usernames)
                            .map(User::getId)
                            .collect(Collectors.toSet())
                            .cache();

                    // remove the users from the group
                    Flux<UserGroup> updateUsersInGroupsMono = Flux.fromIterable(userGroups)
                            .zipWith(toBeRemovedUserIdsMono.repeat())
                            .flatMap(tuple -> {
                                UserGroup userGroup = tuple.getT1();
                                Set<String> userIds = tuple.getT2();
                                userGroup.getUsers().removeAll(userIds);
                                return repository.save(userGroup);
                            })
                            .flatMap(userGroup -> {
                                Map<String, Object> eventData =
                                        Map.of(FieldName.REMOVED_USERS_FROM_USER_GROUPS, usernames);
                                Map<String, Object> extraPropsForCloudHostedInstance =
                                        Map.of(FieldName.REMOVED_USERS_FROM_USER_GROUPS, usernames);
                                Map<String, Object> analyticsProperties = Map.of(
                                        NUMBER_OF_REMOVED_USERS, usernames.size(),
                                        EVENT_DATA, eventData,
                                        CLOUD_HOSTED_EXTRA_PROPS, extraPropsForCloudHostedInstance);
                                return analyticsService.sendObjectEvent(
                                        AnalyticsEvents.REMOVE_USERS_FROM_USER_GROUPS, userGroup, analyticsProperties);
                            })
                            .cache();

                    Flux<PermissionGroup> userGroupRolesFlux = permissionGroupService
                            .findAllByAssignedToGroupIdsIn(ids)
                            .cache();

                    // Get roles for the group, and if there are any, then invalidate the cache for the newly removed
                    // users
                    Mono<Boolean> invalidateCacheOfUsersMono = userGroupRolesFlux
                            .next()
                            .zipWith(toBeRemovedUserIdsMono)
                            .flatMap(tuple -> {
                                Set<String> newlyAddedUserIds = tuple.getT2();
                                return permissionGroupService.cleanPermissionGroupCacheForUsers(
                                        new ArrayList<>(newlyAddedUserIds));
                            })
                            .thenReturn(TRUE);

                    Mono<List<PermissionGroupInfoDTO>> rolesInfoMono = permissionGroupUtils
                            .mapToPermissionGroupInfoDto(userGroupRolesFlux)
                            .collectList()
                            // In case there are no roles associated with the group, then return an empty list.
                            .switchIfEmpty(Mono.just(new ArrayList<>()));

                    Mono<Map<String, List<UserCompactDTO>>> usersInGroupMapMono = updateUsersInGroupsMono
                            .flatMap(updatedUserGroup -> getUsersCompactForTheGroup(updatedUserGroup)
                                    .map(usersList -> Tuples.of(updatedUserGroup.getId(), usersList)))
                            .collectMap(tuple -> tuple.getT1(), tuple -> tuple.getT2());

                    return Mono.zip(invalidateCacheOfUsersMono, rolesInfoMono, usersInGroupMapMono)
                            .flatMap(tuple -> {
                                List<PermissionGroupInfoDTO> rolesInfoList = tuple.getT2();
                                Map<String, List<UserCompactDTO>> usersInGroupMap = tuple.getT3();
                                return Flux.fromIterable(userGroups)
                                        .flatMap(userGroup -> {
                                            List<UserCompactDTO> usersList = usersInGroupMap.get(userGroup.getId());
                                            return generateUserGroupDTO(userGroup, rolesInfoList, usersList);
                                        })
                                        .collectList();
                            });
                });
    }

    @Override
    public Mono<UserGroup> archiveById(String id) {
        Mono<UserGroup> userGroupMono = repository
                .findById(id, DELETE_USER_GROUPS)
                .switchIfEmpty(Mono.error(new AppsmithException(AppsmithError.UNAUTHORIZED_ACCESS)))
                .cache();

        // Find all permission groups that have this user group assigned to it and update them
        Flux<PermissionGroup> updateAllPermissionGroupsFlux = permissionGroupService
                .findAllByAssignedToGroupIdsIn(Set.of(id))
                .flatMap(permissionGroup -> {
                    Set<String> assignedToGroupIds = permissionGroup.getAssignedToGroupIds();
                    assignedToGroupIds.remove(id);
                    PermissionGroup updates = new PermissionGroup();
                    updates.setAssignedToGroupIds(assignedToGroupIds);
                    return permissionGroupService.update(permissionGroup.getId(), updates);
                });

        Mono<UserGroup> archiveGroupAndClearCacheMono = userGroupMono.flatMap(userGroup -> {
            List<String> allUsersAffected = userGroup.getUsers().stream().collect(Collectors.toList());

            // Evict the cache entries for all affected users before archiving
            return permissionGroupService
                    .cleanPermissionGroupCacheForUsers(allUsersAffected)
                    .then(repository.archiveById(id))
                    .then(userGroupMono.flatMap(analyticsService::sendDeleteEvent));
        });

        // First update all the permission groups that have this user group assigned to it
        return updateAllPermissionGroupsFlux
                // then clear cache for all affected users and archive the user group
                .then(archiveGroupAndClearCacheMono)
                // return the deleted group
                .then(userGroupMono);
    }

    private UserGroup generateAndSetUserGroupPolicies(Tenant tenant, UserGroup userGroup) {
        Set<Policy> policies = policyGenerator.getAllChildPolicies(tenant.getPolicies(), Tenant.class, UserGroup.class);
        userGroup.setPolicies(policies);
        return userGroup;
    }

    @Override
    public Mono<UserGroup> findById(String id, AclPermission permission) {
        return repository.findById(id, permission);
    }

    @Override
    public Mono<List<UserGroupDTO>> changeGroupsForUser(
            UpdateGroupMembershipDTO updateGroupMembershipDTO, String originHeader) {

        Set<String> groupIdsAdded = updateGroupMembershipDTO.getGroupsAdded();
        Set<String> groupIdsRemoved = updateGroupMembershipDTO.getGroupsRemoved();
        Set<String> usernames = updateGroupMembershipDTO.getUsernames();

        Mono<List<UserGroupDTO>> userAddedMono = Mono.just(List.of());
        Mono<List<UserGroupDTO>> userRemovedMono = Mono.just(List.of());

        if (!CollectionUtils.isEmpty(groupIdsAdded)) {
            UsersForGroupDTO addUsersDTO = new UsersForGroupDTO();
            addUsersDTO.setUsernames(usernames);
            addUsersDTO.setGroupIds(groupIdsAdded);
            userAddedMono = inviteUsers(addUsersDTO, originHeader);
        }
        if (!CollectionUtils.isEmpty(groupIdsRemoved)) {
            UsersForGroupDTO removeUsersDTO = new UsersForGroupDTO();
            removeUsersDTO.setUsernames(usernames);
            removeUsersDTO.setGroupIds(groupIdsRemoved);
            userRemovedMono = removeUsers(removeUsersDTO);
        }

        return Mono.zip(userAddedMono, userRemovedMono)
                .map(tuple -> Stream.concat(tuple.getT1().stream(), tuple.getT2().stream())
                        .collect(Collectors.toList()));
    }

    @Override
    public Flux<UserGroupCompactDTO> findAllGroupsForUser(String userId) {
        return repository
                .findAllByUsersIn(Set.of(userId), READ_USER_GROUPS)
                .map(userGroup -> new UserGroupCompactDTO(
                        userGroup.getId(), userGroup.getName(), userGroup.getUserPermissions()));
    }

    private UserGroupCompactDTO generateUserGroupCompactDTO(UserGroup userGroup) {
        if (userGroup == null) {
            throw new AppsmithException(AppsmithError.GENERIC_BAD_REQUEST, "user group can't be null");
        }
        UserGroupCompactDTO userGroupCompactDTO = new UserGroupCompactDTO();
        userGroupCompactDTO.setId(userGroup.getId());
        userGroupCompactDTO.setName(userGroup.getName());
        return userGroupCompactDTO;
    }

    @Override
    public Mono<Boolean> bulkRemoveUserFromGroupsWithoutPermission(User user, Set<String> groupIds) {
        return repository
                .findAllById(groupIds)
                .flatMap(userGroup -> {
                    Set<String> usersInGroup = userGroup.getUsers();
                    usersInGroup.remove(user.getId());

                    Update updateObj = new Update();
                    String path = fieldName(QUserGroup.userGroup.users);

                    updateObj.set(path, usersInGroup);
                    return repository.updateById(userGroup.getId(), updateObj).then(Mono.defer(() -> {
                        Map<String, Object> eventData =
                                Map.of(FieldName.REMOVED_USERS_FROM_USER_GROUPS, Set.of(user.getUsername()));
                        Map<String, Object> extraPropsForCloudHostedInstance =
                                Map.of(FieldName.REMOVED_USERS_FROM_USER_GROUPS, Set.of(user.getUsername()));
                        Map<String, Object> analyticsProperties = Map.of(
                                NUMBER_OF_REMOVED_USERS, 1,
                                EVENT_DATA, eventData,
                                CLOUD_HOSTED_EXTRA_PROPS, extraPropsForCloudHostedInstance);
                        return analyticsService.sendObjectEvent(
                                AnalyticsEvents.REMOVE_USERS_FROM_USER_GROUPS, userGroup, analyticsProperties);
                    }));
                })
                .then(Mono.just(TRUE));
    }

    @Override
    public Mono<ProvisionResourceDto> updateProvisionGroup(String id, UserGroup resource) {
        return repository
                .findById(id, MANAGE_USER_GROUPS)
                .switchIfEmpty(
                        Mono.error(new AppsmithException(AppsmithError.ACTION_IS_NOT_AUTHORIZED, "update user groups")))
                .flatMap(userGroup -> {
                    List<String> updateCacheForUserIds =
                            findSymmetricDiff(resource.getUsers(), userGroup.getUsers()).stream()
                                    .toList();
                    userGroup.setName(resource.getName());
                    userGroup.setDescription(resource.getDescription());
                    userGroup.setUsers(resource.getUsers());
                    Mono<Long> updateCacheForUserIdsMono = permissionGroupService
                            .cleanPermissionGroupCacheForUsers(updateCacheForUserIds)
                            .thenReturn(1L);
                    Mono<UserGroup> updateUserGroupMono =
                            super.update(id, userGroup).cache();
                    return Mono.zip(updateUserGroupMono, updateCacheForUserIdsMono)
                            .then(updateUserGroupMono);
                })
                .flatMap(this::updateProvisioningStatus)
                .map(this::getProvisionResourceDto);
    }

    @Override
    public Mono<ProvisionResourceDto> getProvisionGroup(String groupId) {
        return repository
                .findById(groupId, READ_USER_GROUPS)
                .switchIfEmpty(Mono.error(new AppsmithException(AppsmithError.NO_RESOURCE_FOUND, "Group", groupId)))
                .flatMap(this::updateProvisioningStatus)
                .map(this::getProvisionResourceDto);
    }

    @Override
    public Mono<PagedDomain<ProvisionResourceDto>> getProvisionGroups(MultiValueMap<String, String> queryParams) {
        int count = NO_RECORD_LIMIT;
        int startIndex = 0;
        List<String> groupNames = List.of();
        List<String> groupMembers = List.of();

        if (org.apache.commons.lang3.StringUtils.isNotEmpty(queryParams.getFirst(COUNT))) {
            count = Integer.parseInt(queryParams.getFirst(COUNT));
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(queryParams.getFirst(START_INDEX))) {
            startIndex = Integer.parseInt(queryParams.getFirst(START_INDEX));
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(queryParams.getFirst(GROUP_NAME_FILTER))) {
            groupNames = Arrays.stream(queryParams.getFirst(GROUP_NAME_FILTER).split(FILTER_DELIMITER))
                    .toList();
        }

        if (org.apache.commons.lang3.StringUtils.isNotEmpty(queryParams.getFirst(GROUP_USERID_FILTER))) {
            groupMembers = Arrays.stream(
                            queryParams.getFirst(GROUP_USERID_FILTER).split(FILTER_DELIMITER))
                    .toList();
        }

        return repository
                .findUserGroupsWithParamsPaginated(
                        count, startIndex, groupNames, groupMembers, Optional.of(READ_USER_GROUPS))
                .map(pagedUserGroups -> {
                    List<ProvisionResourceDto> provisionedUsersDto = pagedUserGroups.getContent().stream()
                            .map(this::getProvisionResourceDto)
                            .toList();
                    return PagedDomain.<ProvisionResourceDto>builder()
                            .total(pagedUserGroups.getTotal())
                            .count(pagedUserGroups.getCount())
                            .startIndex(pagedUserGroups.getStartIndex())
                            .content(provisionedUsersDto)
                            .build();
                })
                .zipWith(provisionUtils.updateProvisioningStatus(ProvisionStatus.ACTIVE))
                .map(pair -> {
                    PagedDomain<ProvisionResourceDto> pagedGroups = pair.getT1();
                    Boolean updateProvisioningStatus = pair.getT2();
                    return pagedGroups;
                });
    }

    @Override
    public Mono<ProvisionResourceDto> createProvisionGroup(UserGroup userGroup) {
        return createUserGroup(userGroup)
                .flatMap(this::updateProvisionUserGroupPoliciesAndProvisionFlag)
                .flatMap(this::updateProvisioningStatus)
                .map(this::getProvisionResourceDto);
    }

    @Override
    public Mono<List<UserGroupDTO>> removeUsersFromProvisionGroup(UsersForGroupDTO removeUsersFromGroupDTO) {
        List<String> userIds = removeUsersFromGroupDTO.getUserIds();
        return tenantService
                .getDefaultTenantId()
                .flatMap(tenantId -> userRepository
                        .getUserEmailsByIdsAndTenantId(userIds, tenantId, Optional.empty())
                        .collect(Collectors.toSet())
                        .flatMap(userEmails -> {
                            if (CollectionUtils.isEmpty(userEmails)) {
                                return Mono.just(new ArrayList<UserGroupDTO>());
                            }
                            UsersForGroupDTO userEmailsFromGroupDTO = new UsersForGroupDTO();
                            userEmailsFromGroupDTO.setUsernames(userEmails);
                            userEmailsFromGroupDTO.setGroupIds(removeUsersFromGroupDTO.getGroupIds());
                            return this.removeUsers(userEmailsFromGroupDTO);
                        }))
                .zipWith(provisionUtils.updateProvisioningStatus(ProvisionStatus.ACTIVE))
                .map(pair -> {
                    List<UserGroupDTO> userGroupDTOs = pair.getT1();
                    Boolean provisioningStatusUpdate = pair.getT2();
                    return userGroupDTOs;
                });
    }

    @Override
    public Mono<List<UserGroupDTO>> addUsersToProvisionGroup(UsersForGroupDTO addUsersFromGroupDTO) {
        List<String> userIds = addUsersFromGroupDTO.getUserIds();
        return tenantService
                .getDefaultTenantId()
                .flatMap(tenantId -> userRepository
                        .getUserEmailsByIdsAndTenantId(userIds, tenantId, Optional.empty())
                        .collect(Collectors.toSet())
                        .flatMap(userEmails -> {
                            if (CollectionUtils.isEmpty(userEmails)) {
                                return Mono.just(new ArrayList<UserGroupDTO>());
                            }
                            UsersForGroupDTO userEmailsFromGroupDTO = new UsersForGroupDTO();
                            userEmailsFromGroupDTO.setUsernames(userEmails);
                            userEmailsFromGroupDTO.setGroupIds(addUsersFromGroupDTO.getGroupIds());
                            return this.inviteUsers(userEmailsFromGroupDTO, null);
                        }))
                .zipWith(provisionUtils.updateProvisioningStatus(ProvisionStatus.ACTIVE))
                .map(pair -> {
                    List<UserGroupDTO> userGroupDTOs = pair.getT1();
                    Boolean provisioningStatusUpdate = pair.getT2();
                    return userGroupDTOs;
                });
    }

    private ProvisionResourceDto getProvisionResourceDto(UserGroup userGroup) {
        ProvisionResourceMetadata metadata = ProvisionResourceMetadata.builder()
                .created(userGroup.getCreatedAt().toString())
                .lastModified(userGroup.getUpdatedAt().toString())
                .resourceType(GROUP.getValue())
                .build();
        return ProvisionResourceDto.builder()
                .resource(userGroup)
                .metadata(metadata)
                .build();
    }

    private Mono<UserGroup> updateProvisioningStatus(UserGroup userGroup) {
        return provisionUtils.updateProvisioningStatus(ProvisionStatus.ACTIVE).thenReturn(userGroup);
    }

    /**
     * The method edits the existing permissions on the User group resource for Manage, Delete, Invite users to & Remove users from User group permissions.
     * It removes the existing Manage, Delete, Invite users and Remove users permissions for the user group,
     * so that Instance Admin is not able to edit, delete, invite users to or remove users from the User group.
     * It then gives the above-mentioned permissions for the user group to Provision Role, so that the user group can be managed by it.
     * The methods also sets the isProvisioned flag in User resource to True.
     * @param userGroup
     * @return
     */
    private Mono<UserGroup> updateProvisionUserGroupPoliciesAndProvisionFlag(UserGroup userGroup) {
        return userUtils.getProvisioningRole().flatMap(provisioningRole -> {
            userGroup.setIsProvisioned(Boolean.TRUE);
            Set<Policy> currentUserPolicies = userGroup.getPolicies();
            Set<Policy> userGroupPoliciesWithReadUserGroup = currentUserPolicies.stream()
                    .filter(policy -> policy.getPermission().equals(READ_USER_GROUPS.getValue()))
                    .collect(Collectors.toSet());
            userGroup.setPolicies(userGroupPoliciesWithReadUserGroup);
            Map<String, Policy> newManageUserGroupPolicy =
                    policySolution.generatePolicyFromPermissionWithPermissionGroup(
                            MANAGE_USER_GROUPS, provisioningRole.getId());
            Map<String, Policy> newDeleteUserGroupPolicy =
                    policySolution.generatePolicyFromPermissionWithPermissionGroup(
                            DELETE_USER_GROUPS, provisioningRole.getId());
            policySolution.addPoliciesToExistingObject(newManageUserGroupPolicy, userGroup);
            policySolution.addPoliciesToExistingObject(newDeleteUserGroupPolicy, userGroup);
            return repository.save(userGroup);
        });
    }

    private Mono<UserGroup> createUserGroup(UserGroup userGroup) {
        Mono<Boolean> isCreateAllowedMono = sessionUserService
                .getCurrentUser()
                .flatMap(user -> tenantService.findById(user.getTenantId(), CREATE_USER_GROUPS))
                .map(tenant -> TRUE)
                .switchIfEmpty(Mono.just(FALSE));

        Mono<UserGroup> userGroupMono = isCreateAllowedMono.flatMap(allowed -> !allowed
                ? Mono.error(new AppsmithException(AppsmithError.ACTION_IS_NOT_AUTHORIZED, "create user groups"))
                : Mono.just(userGroup));

        return Mono.zip(userGroupMono, tenantService.getDefaultTenant()).flatMap(tuple -> {
            UserGroup userGroupWithPolicy = tuple.getT1();
            Tenant defaultTenant = tuple.getT2();
            userGroupWithPolicy.setTenantId(defaultTenant.getId());
            userGroupWithPolicy = generateAndSetUserGroupPolicies(defaultTenant, userGroupWithPolicy);

            return super.create(userGroupWithPolicy);
        });
    }
}
