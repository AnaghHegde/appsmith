export * from "ce/pages/workspace/WorkspaceInviteUsersForm";
import {
  CustomRolesRamp,
  ErrorBox,
  ErrorTextContainer,
  MailConfigContainer,
  ManageUsersContainer,
  OptionLabel,
  StyledCheckbox,
  StyledForm,
  StyledInviteFieldGroup,
  User,
  UserInfo,
  UserList,
  UserName,
  UserRole,
  WorkspaceInviteWrapper,
  WorkspaceText,
} from "ce/pages/workspace/WorkspaceInviteUsersForm";
import React, { useEffect, useState, useMemo, useRef } from "react";
import styled from "styled-components";
import { reduxForm, SubmissionError } from "redux-form";
import { connect, useSelector } from "react-redux";
import type { AppState } from "@appsmith/reducers";
import {
  getRolesForField,
  getAllUsers,
  getCurrentAppWorkspace,
  getGroupSuggestions,
} from "@appsmith/selectors/workspaceSelectors";
import { ReduxActionTypes } from "@appsmith/constants/ReduxActionConstants";
import type { InviteUsersToWorkspaceFormValues } from "./helpers";
import { inviteUsersToWorkspace } from "./helpers";
import { INVITE_USERS_TO_WORKSPACE_FORM } from "@appsmith/constants/forms";
import {
  createMessage,
  INVITE_USERS_SUBMIT_SUCCESS,
  INVITE_USER_SUBMIT_SUCCESS,
  INVITE_USERS_VALIDATION_EMAILS_EMPTY,
  INVITE_USERS_VALIDATION_EMAIL_LIST,
  INVITE_USERS_VALIDATION_ROLE_EMPTY,
  USERS_HAVE_ACCESS_TO_ALL_APPS,
  USERS_HAVE_ACCESS_TO_ONLY_THIS_APP,
  NO_USERS_INVITED,
  BUSINESS_EDITION_TEXT,
  INVITE_USER_RAMP_TEXT,
} from "@appsmith/constants/messages";
import {
  INVITE_USERS_VALIDATION_EMAIL_LIST as CE_INVITE_USERS_VALIDATION_EMAIL_LIST,
  INVITE_USER_SUBMIT_SUCCESS as CE_INVITE_USER_SUBMIT_SUCCESS,
  INVITE_USERS_SUBMIT_SUCCESS as CE_INVITE_USERS_SUBMIT_SUCCESS,
} from "ce/constants/messages";
import { isEmail } from "utils/formhelpers";
import {
  isPermitted,
  PERMISSION_TYPE,
} from "@appsmith/utils/permissionHelpers";
import AnalyticsUtil from "utils/AnalyticsUtil";
import { getInitialsFromName } from "utils/AppsmithUtils";
import ManageUsers from "pages/workspace/ManageUsers";
import {
  fetchRolesForWorkspace,
  fetchUsersForWorkspace,
  fetchWorkspace,
} from "@appsmith/actions/workspaceActions";
import { useHistory } from "react-router-dom";
import { getAppsmithConfigs } from "@appsmith/configs";
import store from "store";
import TagListField from "../../utils/TagInput";
import { getCurrentUser } from "selectors/usersSelectors";
import {
  getAllAppUsers,
  getAppRolesForField,
} from "@appsmith/selectors/applicationSelectors";
import { USER_PHOTO_ASSET_URL } from "constants/userConstants";
import {
  fetchRolesForApplication,
  fetchUsersForApplication,
} from "@appsmith/actions/applicationActions";
import { ENTITY_TYPE } from "@appsmith/constants/workspaceConstants";
import type { WorkspaceUserRoles } from "@appsmith/constants/workspaceConstants";
import {
  Avatar,
  Button,
  Callout,
  Icon,
  Link,
  Option,
  Select,
  Spinner,
  Text,
  Tooltip,
  toast,
} from "design-system";
import { importSvg } from "design-system-old";
import {
  getRampLink,
  showProductRamps,
} from "@appsmith/selectors/rampSelectors";
import {
  RAMP_NAME,
  RampFeature,
  RampSection,
} from "utils/ProductRamps/RampsControlList";
import { getDomainFromEmail } from "utils/helpers";
import PartnerProgramCallout from "./PartnerProgramCallout";
import {
  getPartnerProgramCalloutShown,
  setPartnerProgramCalloutShown,
} from "utils/storage";
import { isFreePlan } from "@appsmith/selectors/tenantSelectors";
import { useFeatureFlag } from "utils/hooks/useFeatureFlag";
import { FEATURE_FLAG } from "@appsmith/entities/FeatureFlag";
import { getShowAdminSettings } from "@appsmith/utils/BusinessFeatures/adminSettingsHelpers";

const NoEmailConfigImage = importSvg(
  () => import("assets/images/email-not-configured.svg"),
);

const { cloudHosting } = getAppsmithConfigs();

const validateFormValues = (
  values: {
    users: string;
    role?: string | string[];
  },
  isAclFlow: boolean,
) => {
  if (values.users && values.users.length > 0) {
    const _users = values.users.split(",").filter(Boolean);

    _users.forEach((user) => {
      if (!isEmail(user) && !isUserGroup(user)) {
        throw new SubmissionError({
          _error: createMessage(
            isAclFlow
              ? CE_INVITE_USERS_VALIDATION_EMAIL_LIST
              : INVITE_USERS_VALIDATION_EMAIL_LIST,
            cloudHosting,
          ),
        });
      }
    });
  } else {
    throw new SubmissionError({
      _error: createMessage(INVITE_USERS_VALIDATION_EMAILS_EMPTY),
    });
  }

  if (typeof values.role === "undefined" || values.role.length === 0) {
    throw new SubmissionError({
      _error: createMessage(INVITE_USERS_VALIDATION_ROLE_EMPTY),
    });
  }
};

const validate = (values: any) => {
  const errors: any = {};
  if (!(values.users && values.users.length > 0)) {
    errors["users"] = createMessage(INVITE_USERS_VALIDATION_EMAILS_EMPTY);
  }

  if (typeof values.role === "undefined" || values.role.length === 0) {
    errors["role"] = createMessage(INVITE_USERS_VALIDATION_ROLE_EMPTY);
  }

  if (values.users && values.users.length > 0) {
    const _users = values.users.split(",").filter(Boolean);

    _users.forEach((user: string) => {
      if (!isEmail(user) && !isUserGroup(user)) {
        errors["users"] = createMessage(
          INVITE_USERS_VALIDATION_EMAIL_LIST,
          cloudHosting,
        );
      }
    });
  }
  return errors;
};

const isUserGroup = (user: string) => {
  return getGroupSuggestions(store.getState())?.some(
    (ug: any) => ug.id === user,
  );
};

const StyledInviteFieldGroupEE = styled(StyledInviteFieldGroup)`
  .user-icons {
    margin-right: 8px;
  }
`;

const StyledUserList = styled(UserList)`
  .user-icons {
    width: 32px;
    justify-content: center;
  }
`;

function InviteUserText({
  isAppLevelInviteOnSelfHost,
  isApplicationInvite,
}: {
  isApplicationInvite: boolean;
  isAppLevelInviteOnSelfHost: boolean;
}) {
  let content: JSX.Element;

  const showRampSelector = showProductRamps(RAMP_NAME.INVITE_USER_TO_APP);
  const canShowRamp = useSelector(showRampSelector);

  const rampLinkSelector = getRampLink({
    section: RampSection.AppShare,
    feature: RampFeature.Gac,
  });
  const rampLink = useSelector(rampLinkSelector);

  if (isAppLevelInviteOnSelfHost) {
    content = <>{createMessage(USERS_HAVE_ACCESS_TO_ONLY_THIS_APP)}</>;
  } else {
    content = <>{createMessage(USERS_HAVE_ACCESS_TO_ALL_APPS)}</>;
  }

  if (cloudHosting && canShowRamp) {
    if (isApplicationInvite) {
      content = (
        <>
          {createMessage(INVITE_USER_RAMP_TEXT)}
          <Link kind="primary" target="_blank" to={rampLink}>
            {createMessage(BUSINESS_EDITION_TEXT)}
          </Link>
        </>
      );
    }
  }
  return (
    <Text
      color="var(--ads-v2-color-fg)"
      data-testid="helper-message"
      kind="action-m"
    >
      {content}
    </Text>
  );
}

function WorkspaceInviteUsersForm(props: any) {
  const [emailError, setEmailError] = useState("");
  const [selectedOption, setSelectedOption] = useState<any[]>([]);
  const user = useSelector(getCurrentUser);
  const userRef = React.createRef<HTMLDivElement>();
  const history = useHistory();
  const selectedId = props?.selected?.id;
  const currentUser = useSelector(getCurrentUser);
  const freePlan = useSelector(isFreePlan);

  const selected = useMemo(
    () =>
      selectedId &&
      props.selected && {
        description: props.selected.name,
        value: props.selected.name,
        key: props.selected.id,
      },
    [selectedId],
  );

  const {
    allUsers,
    anyTouched,
    customProps = {},
    error,
    fetchAllAppRoles,
    fetchAllAppUsers,
    fetchAllRoles,
    fetchCurrentWorkspace,
    fetchGroupSuggestions,
    fetchUser,
    handleSubmit,
    isApplicationInvite = false,
    isLoading,
    isMultiSelectDropdown = false,
    placeholder = "",
    submitFailed,
    submitSucceeded,
    submitting,
    valid,
  } = props;

  const {
    disableDropdown = false,
    disableManageUsers = false,
    disableUserList = false,
    dropdownPlaceholder = "",
    isAclFlow = false,
    onSubmitHandler,
  } = customProps;

  // set state for checking number of users invited
  const [numberOfUsersInvited, updateNumberOfUsersInvited] = useState(0);
  const currentWorkspace = useSelector(getCurrentAppWorkspace);
  const groupSuggestions: any[] = useSelector(getGroupSuggestions);

  const invitedEmails = useRef<undefined | string[]>();
  const emailOutsideCurrentDomain = useRef<undefined | string>();
  const [showPartnerProgramCallout, setShowPartnerProgramCallout] =
    useState(false);

  const userWorkspacePermissions = currentWorkspace?.userPermissions ?? [];
  const canManage = isPermitted(
    userWorkspacePermissions,
    PERMISSION_TYPE.MANAGE_WORKSPACE,
  );
  const isEEFeature = (!isAclFlow && !cloudHosting) || false;
  const isAppLevelInviteOnSelfHost =
    (!cloudHosting && isApplicationInvite) || false;

  const isFeatureEnabled = useFeatureFlag(FEATURE_FLAG.license_gac_enabled);

  useEffect(() => {
    setSelectedOption([]);
  }, [submitSucceeded]);

  useEffect(() => {
    if (!isAclFlow) {
      if (isAppLevelInviteOnSelfHost) {
        fetchAllAppUsers(props.applicationId);
        fetchAllAppRoles(props.applicationId);
      } else {
        fetchUser(props.workspaceId);
        fetchAllRoles(props.workspaceId);
      }
      fetchCurrentWorkspace(props.workspaceId);
      fetchGroupSuggestions();
    }
  }, [
    props.workspaceId,
    fetchUser,
    fetchAllRoles,
    fetchCurrentWorkspace,
    fetchGroupSuggestions,
    fetchAllAppRoles,
    fetchAllAppUsers,
    props.applicationId,
    isAppLevelInviteOnSelfHost,
  ]);

  useEffect(() => {
    if (selected) {
      setSelectedOption([selected]);
      props.initialize({
        role: [selected],
      });
    }
  }, []);

  useEffect(() => {
    if (submitSucceeded) {
      if (isAclFlow) {
        toast.show(
          numberOfUsersInvited > 1
            ? createMessage(CE_INVITE_USERS_SUBMIT_SUCCESS)
            : createMessage(CE_INVITE_USER_SUBMIT_SUCCESS),
          { kind: "success" },
        );
      } else {
        toast.show(
          numberOfUsersInvited > 1
            ? createMessage(INVITE_USERS_SUBMIT_SUCCESS)
            : createMessage(INVITE_USER_SUBMIT_SUCCESS),
          { kind: "success" },
        );
        checkIfInvitedUsersFromDifferentDomain();
      }
    }
  }, [submitSucceeded]);

  const styledRoles =
    props.options && isAclFlow
      ? props.options.length > 0
        ? props.options
        : []
      : props.roles.map((role: any) => {
          return {
            key: role.id,
            value: role.name?.split(" - ")[0],
            description: role.description,
          };
        });

  if (isEEFeature && getShowAdminSettings(isFeatureEnabled, user)) {
    styledRoles.push({
      key: "custom-pg",
      value: "Assign Custom Role",
      link: "/settings/groups",
      icon: "right-arrow",
    });
  }

  const allUsersProfiles = React.useMemo(
    () =>
      allUsers.map(
        (user: {
          userId: string;
          userGroupId: string;
          username: string;
          permissionGroupId: string;
          permissionGroupName: string;
          name: string;
        }) => {
          return {
            ...user,
            initials: getInitialsFromName(user.name || user.username),
          };
        },
      ),
    [allUsers],
  );

  const onSelect = (value: string, option?: any) => {
    if (option.value === "custom-pg") {
      history.push("/settings/groups");
    }
    if (isMultiSelectDropdown) {
      setSelectedOption((selectedOptions) => [...selectedOptions, option]);
    } else {
      setSelectedOption([option]);
    }
  };

  const onRemoveOptions = (value: string, option?: any) => {
    if (isMultiSelectDropdown) {
      setSelectedOption((selectedOptions) =>
        selectedOptions.filter((opt) => opt.value !== option.value),
      );
    }
  };

  const errorHandler = (error: string, values: string[]) => {
    if (values && values.length > 0) {
      const hasInvalidUser = values.some(
        (user) => !isEmail(user) && !isUserGroup(user),
      );
      let error = "";
      if (hasInvalidUser) {
        error = isAclFlow
          ? createMessage(CE_INVITE_USERS_VALIDATION_EMAIL_LIST, cloudHosting)
          : createMessage(INVITE_USERS_VALIDATION_EMAIL_LIST, cloudHosting);
      }
      setEmailError(error);
    } else {
      props.customError?.("");
    }
  };

  const checkIfInvitedUsersFromDifferentDomain = async () => {
    if (!currentUser?.email) return true;

    const currentUserEmail = currentUser?.email;
    const partnerProgramCalloutShown = await getPartnerProgramCalloutShown();
    const currentUserDomain = getDomainFromEmail(currentUserEmail);

    if (invitedEmails.current && !partnerProgramCalloutShown) {
      const _emailOutsideCurrentDomain = invitedEmails.current.find(
        (email) => getDomainFromEmail(email) !== currentUserDomain,
      );
      if (_emailOutsideCurrentDomain) {
        emailOutsideCurrentDomain.current = _emailOutsideCurrentDomain;
        invitedEmails.current = undefined;
        setShowPartnerProgramCallout(true);
      }
    }
  };

  return (
    <WorkspaceInviteWrapper>
      <StyledForm
        onSubmit={handleSubmit((values: any, dispatch: any) => {
          const roles = isMultiSelectDropdown
            ? selectedOption.map((option: any) => option.value).join(",")
            : selectedOption[0].value;
          validateFormValues({ ...values, role: roles }, isAclFlow);
          const usersAsStringsArray = values.users.split(",");
          // update state to show success message correctly
          updateNumberOfUsersInvited(usersAsStringsArray.length);
          const usersArray = usersAsStringsArray.filter((user: any) =>
            isEmail(user),
          );
          invitedEmails.current = usersArray;
          const groupsArray = usersAsStringsArray.filter(
            (user: any) => !isEmail(user),
          );
          const usersStr = usersArray.join(",");
          const groupsStr = groupsArray.join(",");
          const groupsData = [];
          for (const gId of groupsArray) {
            const data = groupSuggestions.find((g) => g.id === gId);
            data && groupsData.push(data);
          }
          AnalyticsUtil.logEvent("INVITE_USER", {
            ...(isEEFeature
              ? {
                  groups: groupsData.map((grp: any) => grp.id),
                  numberOfGroupsInvited: groupsArray.length,
                }
              : {}),
            ...(cloudHosting ? { users: usersStr } : {}),
            ...(isAppLevelInviteOnSelfHost
              ? { appId: props.applicationId }
              : {}),
            numberOfUsersInvited: usersArray.length,
            role: roles,
            orgId: props.workspaceId,
          });
          if (onSubmitHandler) {
            return onSubmitHandler({
              ...(props.workspaceId ? { workspaceId: props.workspaceId } : {}),
              users: usersStr,
              options: isMultiSelectDropdown
                ? selectedOption
                : selectedOption[0],
            });
          }
          return inviteUsersToWorkspace(
            {
              ...(isEEFeature ? { groups: groupsStr } : {}),
              ...(props.workspaceId ? { workspaceId: props.workspaceId } : {}),
              users: usersStr,
              permissionGroupId: roles,
              isApplicationInvite: isAppLevelInviteOnSelfHost,
              ...(isAppLevelInviteOnSelfHost
                ? {
                    applicationId: props.applicationId,
                    roleType: selectedOption[0].value,
                  }
                : {}),
            },
            dispatch,
          );
        })}
      >
        <StyledInviteFieldGroupEE>
          <div style={{ width: "60%" }}>
            <TagListField
              autofocus
              customError={(err: string, values?: string[]) =>
                errorHandler(err, values || [])
              }
              data-testid="t--invite-email-input"
              intent="success"
              label="Emails"
              name="users"
              placeholder={placeholder || "Enter email address(es)"}
              suggestionLeftIcon={
                <Icon className="user-icons" name="group-line" size="md" />
              }
              suggestions={isEEFeature ? groupSuggestions : undefined}
              type="text"
            />
            {emailError && (
              <ErrorTextContainer>
                <Icon name="alert-line" size="sm" />
                <Text kind="body-s" renderAs="p">
                  {emailError}
                </Text>
              </ErrorTextContainer>
            )}
          </div>
          <div style={{ width: "40%" }}>
            <Select
              data-testid="t--invite-role-input"
              filterOption={(input, option) =>
                (option &&
                  option.label &&
                  option.label
                    .toString()
                    .toLowerCase()
                    .includes(input.toLowerCase())) ||
                false
              }
              getPopupContainer={(triggerNode) =>
                triggerNode.parentNode.parentNode
              }
              isDisabled={disableDropdown}
              isMultiSelect={isMultiSelectDropdown}
              listHeight={isAclFlow ? 200 : 400}
              onDeselect={onRemoveOptions}
              onSelect={onSelect}
              optionLabelProp="label"
              placeholder={dropdownPlaceholder || "Select a role"}
              showSearch={isAclFlow ? true : false}
              value={selectedOption}
            >
              {styledRoles.map((role: any) => (
                <Option
                  key={isAppLevelInviteOnSelfHost ? role.value : role.key}
                  label={role.value}
                  value={isAppLevelInviteOnSelfHost ? role.value : role.key}
                >
                  <div className="flex items-center gap-1">
                    {isMultiSelectDropdown && (
                      <StyledCheckbox
                        isSelected={selectedOption.find((v) =>
                          isAppLevelInviteOnSelfHost
                            ? v.value === role.value
                            : v.key === role.key,
                        )}
                      />
                    )}
                    <div className="flex flex-col gap-1">
                      <div className="flex gap-1">
                        {role.icon && <Icon name={role.icon} size="md" />}
                        <OptionLabel
                          color="var(--ads-v2-color-fg-emphasis)"
                          kind={role.description && "heading-xs"}
                        >
                          {role.value}
                        </OptionLabel>
                      </div>
                      {role.description && (
                        <Text kind="body-s">{role.description}</Text>
                      )}
                    </div>
                  </div>
                </Option>
              ))}
              {cloudHosting && showProductRamps(RAMP_NAME.CUSTOM_ROLES) && (
                <Option disabled>
                  <CustomRolesRamp />
                </Option>
              )}
            </Select>
          </div>
          <div>
            <Button
              className="t--invite-user-btn"
              isDisabled={!valid || selectedOption.length === 0}
              isLoading={submitting && !(submitFailed && !anyTouched)}
              size="md"
              type="submit"
            >
              Invite
            </Button>
          </div>
        </StyledInviteFieldGroupEE>

        {!isAclFlow && (
          <div className="flex items-start gap-2 mt-2">
            <Icon className="mt-1" name="user-3-line" size="md" />
            <WorkspaceText>
              <InviteUserText
                isAppLevelInviteOnSelfHost={isAppLevelInviteOnSelfHost}
                isApplicationInvite={isApplicationInvite}
              />
            </WorkspaceText>
          </div>
        )}

        {!cloudHosting &&
          !isAclFlow &&
          freePlan &&
          showPartnerProgramCallout &&
          emailOutsideCurrentDomain.current && (
            <div className="mt-2">
              <PartnerProgramCallout
                email={emailOutsideCurrentDomain.current}
                onClose={() => {
                  setShowPartnerProgramCallout(false);
                  setPartnerProgramCalloutShown();
                  emailOutsideCurrentDomain.current = undefined;
                }}
              />
            </div>
          )}

        {isLoading ? (
          <div className="pt-4 overflow-hidden">
            <Spinner size="lg" />
          </div>
        ) : (
          <>
            {allUsers.length === 0 && !disableUserList && (
              <MailConfigContainer data-testid="no-users-content">
                <NoEmailConfigImage />
                <Text kind="action-s">{createMessage(NO_USERS_INVITED)}</Text>
              </MailConfigContainer>
            )}
            {!disableUserList && (
              <StyledUserList ref={userRef}>
                {allUsersProfiles.map(
                  (user: {
                    username: string;
                    name: string;
                    roles: WorkspaceUserRoles[];
                    initials: string;
                    userGroupId: string;
                    userId: string;
                    photoId?: string;
                  }) => {
                    const showUser =
                      (isAppLevelInviteOnSelfHost
                        ? user.roles?.[0]?.entityType ===
                          ENTITY_TYPE.APPLICATION
                        : user.roles?.[0]?.entityType ===
                          ENTITY_TYPE.WORKSPACE) && user.roles?.[0]?.id;
                    return showUser ? (
                      <User
                        key={
                          user?.userGroupId ? user.userGroupId : user.username
                        }
                      >
                        <UserInfo>
                          {user?.userGroupId ? (
                            <>
                              <Icon
                                className="user-icons"
                                name="group-line"
                                size="lg"
                              />
                              <UserName>
                                <Text
                                  color="var(--ads-v2-color-fg)"
                                  kind="heading-xs"
                                >
                                  {user.name}
                                </Text>
                              </UserName>
                            </>
                          ) : (
                            <>
                              <Avatar
                                firstLetter={user.initials}
                                image={
                                  user.photoId
                                    ? `/api/${USER_PHOTO_ASSET_URL}/${user.photoId}`
                                    : undefined
                                }
                                isTooltipEnabled={false}
                                label={user.name || user.username}
                              />
                              <UserName>
                                <Tooltip
                                  content={user.username}
                                  placement="top"
                                >
                                  <Text
                                    color="var(--ads-v2-color-fg)"
                                    kind="heading-xs"
                                  >
                                    {user.name}
                                  </Text>
                                </Tooltip>
                              </UserName>
                            </>
                          )}
                        </UserInfo>
                        <UserRole>
                          <Text kind="action-m">
                            {user.roles?.[0]?.name?.split(" - ")[0] || ""}
                          </Text>
                        </UserRole>
                      </User>
                    ) : null;
                  },
                )}
              </StyledUserList>
            )}
          </>
        )}
        <ErrorBox message={submitFailed}>
          {submitFailed && error && <Callout kind="error">{error}</Callout>}
        </ErrorBox>
        {canManage && !disableManageUsers && (
          <ManageUsersContainer>
            <ManageUsers workspaceId={props.workspaceId} />
          </ManageUsersContainer>
        )}
      </StyledForm>
    </WorkspaceInviteWrapper>
  );
}

export default connect(
  (
    state: AppState,
    {
      formName,
      isApplicationInvite,
    }: { formName?: string; isApplicationInvite?: boolean },
  ): any => {
    const isAppLevelInviteOnSelfHost =
      (!cloudHosting && isApplicationInvite) || false;
    return {
      roles: isAppLevelInviteOnSelfHost
        ? getAppRolesForField(state)
        : getRolesForField(state),
      allUsers: isAppLevelInviteOnSelfHost
        ? getAllAppUsers(state)
        : getAllUsers(state),
      isLoading: isAppLevelInviteOnSelfHost
        ? state.ui.applications.loadingStates.isFetchAllUsers
        : state.ui.workspaces.loadingStates.isFetchAllUsers,
      form: formName || INVITE_USERS_TO_WORKSPACE_FORM,
    };
  },
  (dispatch: any) => ({
    fetchAllRoles: (workspaceId: string) =>
      dispatch(fetchRolesForWorkspace(workspaceId)),
    fetchCurrentWorkspace: (workspaceId: string) =>
      dispatch(fetchWorkspace(workspaceId)),
    fetchUser: (workspaceId: string) =>
      dispatch(fetchUsersForWorkspace(workspaceId)),
    fetchGroupSuggestions: () =>
      dispatch({
        type: ReduxActionTypes.FETCH_GROUP_SUGGESTIONS,
      }),
    fetchAllAppRoles: (applicationId: string) =>
      dispatch(fetchRolesForApplication(applicationId)),
    fetchAllAppUsers: (applicationId: string) =>
      dispatch(fetchUsersForApplication(applicationId)),
  }),
)(
  reduxForm<
    InviteUsersToWorkspaceFormValues,
    {
      roles?: any;
      applicationId?: string;
      workspaceId?: string;
      isApplicationInvite?: boolean;
      placeholder?: string;
      customProps?: any;
      selected?: any;
      options?: any;
      isMultiSelectDropdown?: boolean;
    }
  >({
    validate,
  })(WorkspaceInviteUsersForm),
);
