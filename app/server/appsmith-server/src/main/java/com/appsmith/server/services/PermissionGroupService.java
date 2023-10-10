package com.appsmith.server.services;

import com.appsmith.server.acl.AclPermission;
import com.appsmith.server.domains.Application;
import com.appsmith.server.domains.PermissionGroup;
import com.appsmith.server.domains.User;
import com.appsmith.server.domains.UserGroup;
import com.appsmith.server.domains.Workspace;
import com.appsmith.server.dtos.PermissionGroupInfoDTO;
import com.appsmith.server.services.ce.PermissionGroupServiceCE;
import com.appsmith.server.solutions.roles.dtos.RoleViewDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.appsmith.server.services.ce_compatible.PermissionGroupServiceCECompatible;

public interface PermissionGroupService extends PermissionGroupServiceCECompatible {

    Mono<List<PermissionGroupInfoDTO>> getAll();

    Flux<PermissionGroup> findAllByAssignedToUsersIn(Set<String> userIds);

    Mono<PermissionGroup> archiveById(String id);

    Mono<PermissionGroup> bulkUnassignFromUserGroupsWithoutPermission(
            PermissionGroup permissionGroup, Set<String> userGroupIds);

    Mono<PermissionGroup> bulkUnassignFromUserGroups(PermissionGroup permissionGroup, Set<UserGroup> userGroups);

    Mono<List<PermissionGroupInfoDTO>> getAllAssignableRoles();

    Mono<PermissionGroup> findById(String id, AclPermission permission);

    Flux<PermissionGroup> findAllByAssignedToGroupIdsIn(Set<String> groupIds);

    Flux<PermissionGroup> getAllByAssignedToUserGroupAndDefaultWorkspace(
            UserGroup userGroup, Workspace defaultWorkspace, AclPermission aclPermission);

    Mono<RoleViewDTO> findConfigurableRoleById(String id);

    Mono<PermissionGroupInfoDTO> updatePermissionGroup(String id, PermissionGroup permissionGroup);

    Mono<RoleViewDTO> createCustomPermissionGroup(PermissionGroup permissionGroup);

    Mono<Boolean> bulkUnassignUserFromPermissionGroupsWithoutPermission(User user, Set<String> permissionGroupIds);

    Mono<PermissionGroup> unassignFromUserGroup(PermissionGroup permissionGroup, UserGroup userGroup);

    Mono<PermissionGroup> assignToUserGroup(PermissionGroup permissionGroup, UserGroup userGroup);

    Mono<PermissionGroup> bulkAssignToUserGroups(PermissionGroup permissionGroup, Set<UserGroup> userGroups);

    Flux<PermissionGroup> findAllByAssignedToUserId(String userId);

    Flux<PermissionGroup> findAllByAssignedToGroupId(String userGroupId);

    Mono<Boolean> bulkAssignToUsersWithoutPermission(PermissionGroup pg, List<User> users);

    Mono<Set<String>> getAllDirectlyAndIndirectlyAssignedUserIds(PermissionGroup permissionGroup);

    Flux<PermissionGroup> getAllDefaultRolesForApplication(
            Application application, Optional<AclPermission> aclPermission);

    Mono<PermissionGroup> bulkAssignToUsersAndGroups(PermissionGroup role, List<User> users, List<UserGroup> groups);

    Mono<PermissionGroup> assignToUserGroupAndSendEvent(PermissionGroup permissionGroup, UserGroup userGroup);

    Mono<PermissionGroup> bulkAssignToUserGroupsAndSendEvent(
            PermissionGroup permissionGroup, Set<UserGroup> userGroups);

    Mono<PermissionGroup> unAssignFromUserGroupAndSendEvent(PermissionGroup permissionGroup, UserGroup userGroup);

    Mono<PermissionGroup> bulkUnAssignFromUserGroupsAndSendEvent(
            PermissionGroup permissionGroup, Set<UserGroup> userGroups);

    Flux<String> getRoleNamesAssignedToUserIds(Set<String> userIds);

    Mono<Boolean> bulkUnAssignUsersAndUserGroupsFromPermissionGroupsWithoutPermission(
            List<User> users, List<UserGroup> groups, List<PermissionGroup> roles);

    Flux<PermissionGroup> findAllByAssignedToUserIdsInWithoutPermission(Set<String> userIds);

    Flux<PermissionGroup> findAllByAssignedToGroupIdsInWithoutPermission(Set<String> groupIds);

    Flux<PermissionGroup> findAllByAssignedToUserIdWithoutPermission(String userId);

    Flux<PermissionGroup> findAllByAssignedToGroupIdWithoutPermission(String groupId);
}
