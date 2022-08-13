import React, { useEffect, useState } from "react";
import {
  Button,
  Icon,
  IconSize,
  Menu,
  MenuItem,
  MenuItemProps,
  Table,
  Toaster,
  Variant,
} from "components/ads";
import styled from "styled-components";
import { TabComponent, TabProp } from "components/ads/Tabs";
import { ActiveAllGroupsList } from "./ActiveAllGroupsList";
import { PageHeader } from "./PageHeader";
import ProfileImage from "pages/common/ProfileImage";
import { HelpPopoverStyle, SaveButtonBar, TabsWrapper } from "./components";
import { debounce } from "lodash";
import FormDialogComponent from "components/editorComponents/form/FormDialogComponent";
import WorkspaceInviteUsersForm from "pages/workspace/WorkspaceInviteUsersForm";
import { useHistory } from "react-router";
import { HighlightText } from "./helpers/HighlightText";
import { User } from "./UserListing";
import { Position } from "@blueprintjs/core";
import {
  ADD_USERS,
  ARE_YOU_SURE,
  CLONE_GROUP,
  createMessage,
  DELETE_USER,
  DELETE_GROUP,
  INVITE_USERS_SUBMIT_BUTTON_TEXT,
  NO_USERS_MESSAGE,
  ROLES_UPDATED_SUCCESS,
  RENAME_SUCCESSFUL,
  RENAME_GROUP,
  SEARCH_PLACEHOLDER,
} from "@appsmith/constants/messages";
import { BackButton } from "pages/Settings/components";

export type GroupProps = {
  isEditing: boolean;
  isDeleting: boolean;
  rolename: string;
  id: string;
  allUsers: Array<any>;
  allPermissions: Array<any>;
  activePermissions: Array<any>;
  isNew?: boolean;
};

export type GroupEditProps = {
  selected: GroupProps;
  onDelete: any;
  onClone: any;
};

const ListUsers = styled.div`
  margin-top: 4px;

  thead {
    display: none;
  }

  tbody {
    tr {
      td {
        .actions-icon {
          visibility: hidden;
          justify-content: end;
          > svg {
            path {
              fill: var(--appsmith-color-black-400);
            }
            &:hover {
              path {
                fill: var(--appsmith-color-black-700);
              }
            }
          }
          &.active {
            visibility: visible;
          }
        }
      }

      &:hover {
        td {
          .actions-icon {
            visibility: visible;
          }
        }
      }
    }
  }
`;

const EachUser = styled.div`
  display: flex;
  align-items: center;

  .user-icons {
    margin-right 8px;
    cursor: initial;

    span {
      color: var(--appsmith-color-black-0);
    }
  }
`;

const NoUsersWrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 56px 0;
`;

const StyledButton = styled(Button)`
  flex: 1 0 auto;
  margin: 16px 12px 0 0;
  min-width: 88px;
`;

const NoUsersText = styled.div`
  font-weight: 400;
  font-size: 16px;
  line-height: 24px;
  color: var(--appsmith-color-black-700);
`;

export type Permissions = {
  activePermissions: string[];
  allPermissions: string[];
};

export function GroupAddEdit(props: GroupEditProps) {
  const { selected } = props;
  const [selectedTabIndex, setSelectedTabIndex] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const [users, setUsers] = useState<User[]>(selected.allUsers || []);
  const [permissions, setPermissions] = useState<Permissions>({
    activePermissions: selected.activePermissions || [],
    allPermissions: selected.allPermissions || [],
  });

  const [removedActiveGroups, setRemovedActiveGroups] = useState<Array<any>>(
    [],
  );
  const [addedAllGroups, setAddedAllGroups] = useState<Array<any>>([]);
  const [isSaving, setIsSaving] = useState(false);
  const [pageTitle, setPageTitle] = useState(selected.rolename);

  const history = useHistory();

  useEffect(() => {
    const saving =
      removedActiveGroups.length > 0 ||
      addedAllGroups.length > 0 ||
      pageTitle !== selected.rolename;
    setIsSaving(saving);
  }, [removedActiveGroups, addedAllGroups, pageTitle]);

  const onButtonClick = () => {
    setShowModal(true);
  };

  const onSearch = debounce((search: string) => {
    let userResults: User[] = [];
    let permissionResults: Permissions;
    if (search && search.trim().length > 0) {
      setSearchValue(search);
      userResults =
        users &&
        users.filter((user) =>
          user.username?.toLocaleUpperCase().includes(search),
        );
      setUsers(userResults);
      permissionResults = permissions && {
        activePermissions: permissions.activePermissions.filter((permission) =>
          permission?.toLocaleUpperCase().includes(search),
        ),
        allPermissions: permissions.allPermissions.filter((permission: any) =>
          permission?.toLocaleUpperCase().includes(search),
        ),
      };
      setPermissions({ ...permissionResults });
    } else {
      setSearchValue("");
      setUsers(selected.allUsers);
      setPermissions({
        activePermissions: selected.activePermissions || [],
        allPermissions: selected.allPermissions || [],
      });
    }
  }, 300);

  const onAddGroup = (group: any) => {
    if (addedAllGroups.includes(group)) {
      const updateGroups = addedAllGroups.filter((grp) => grp !== group);
      setAddedAllGroups(updateGroups);
    } else {
      setAddedAllGroups([...addedAllGroups, group]);
    }
  };

  const onRemoveGroup = (group: any) => {
    if (removedActiveGroups.includes(group)) {
      const updateGroups = removedActiveGroups.filter((grp) => grp !== group);
      setRemovedActiveGroups(updateGroups);
    } else {
      setRemovedActiveGroups([...removedActiveGroups, group]);
    }
  };

  const onSaveChanges = () => {
    const updatedActiveGroups = permissions.activePermissions.filter(
      (role) => !removedActiveGroups.includes(role),
    );
    updatedActiveGroups.push(...addedAllGroups);
    const updatedAllGroups = permissions.allPermissions.filter(
      (role) => !addedAllGroups.includes(role),
    );
    updatedAllGroups.push(...removedActiveGroups);
    setPermissions({
      activePermissions: updatedActiveGroups,
      allPermissions: updatedAllGroups,
    });
    setRemovedActiveGroups([]);
    setAddedAllGroups([]);
    Toaster.show({
      text: createMessage(ROLES_UPDATED_SUCCESS),
      variant: Variant.success,
    });
  };

  const onClearChanges = () => {
    setRemovedActiveGroups([]);
    setAddedAllGroups([]);
    setPageTitle(selected.rolename);
  };

  const onEditTitle = (name: string) => {
    setPageTitle(name);
    Toaster.show({
      text: createMessage(RENAME_SUCCESSFUL),
      variant: Variant.success,
    });
  };

  const columns = [
    {
      Header: "",
      accessor: "users",
      Cell: function UserCell(props: any) {
        const user = props.cell.row.original;
        return (
          <EachUser>
            <ProfileImage
              className="user-icons"
              size={20}
              source={`/api/v1/users/photo/${user.username}`}
              userName={user.username}
            />
            <HighlightText highlight={searchValue} text={user.username} />
          </EachUser>
        );
      },
    },
    {
      Header: "",
      accessor: "actions",
      Cell: function ActionsCell(props: any) {
        const data = props.cell.row.original;
        const [showOptions, setShowOptions] = useState(false);
        const [showConfirmationText, setShowConfirmationText] = useState(false);

        const onOptionSelect = () => {
          if (showConfirmationText) {
            const updatedData = users.filter((user) => {
              return user.userId !== data.userId;
            });
            setUsers(updatedData);
          } else {
            setShowOptions(true);
            setShowConfirmationText(true);
          }
        };

        return (
          <Menu
            canEscapeKeyClose
            canOutsideClickClose
            className="t--menu-actions-icon"
            data-testid="actions-cell-menu-options"
            isOpen={showOptions}
            menuItemWrapperWidth={"auto"}
            onClose={() => setShowOptions(false)}
            onClosing={() => {
              setShowConfirmationText(false);
              setShowOptions(false);
            }}
            onOpening={() => setShowOptions(true)}
            position={Position.BOTTOM_RIGHT}
            target={
              <Icon
                className={`actions-icon ${showOptions && "active"}`}
                data-testid="actions-cell-menu-icon"
                name="more-2-fill"
                onClick={() => setShowOptions(!showOptions)}
                size={IconSize.XXL}
              />
            }
          >
            <HelpPopoverStyle />
            <MenuItem
              className={"delete-menu-item"}
              icon={"delete-blank"}
              key={createMessage(DELETE_USER)}
              onSelect={() => {
                onOptionSelect();
              }}
              text={
                showConfirmationText
                  ? createMessage(ARE_YOU_SURE)
                  : createMessage(DELETE_USER)
              }
              {...(showConfirmationText ? { type: "warning" } : {})}
            />
          </Menu>
        );
      },
    },
  ];

  const tabs: TabProp[] = [
    {
      key: "users",
      title: "Users",
      count: users.length,
      panelComponent: (
        <ListUsers>
          {users && users.length > 0 ? (
            <Table columns={columns} data={users} data-testid="listing-table" />
          ) : (
            <NoUsersWrapper>
              <NoUsersText data-testid="t--no-users-msg">
                {createMessage(NO_USERS_MESSAGE)}
              </NoUsersText>
              <StyledButton
                data-testid="t--add-users-button"
                height="36"
                onClick={onButtonClick}
                tag="button"
                text={createMessage(ADD_USERS)}
              />
            </NoUsersWrapper>
          )}
        </ListUsers>
      ),
    },
    {
      key: "permissions",
      title: "Roles",
      count:
        permissions.activePermissions.length +
        permissions.allPermissions.length,
      panelComponent: (
        <ActiveAllGroupsList
          activeGroups={permissions.activePermissions}
          addedAllGroups={addedAllGroups}
          allGroups={permissions.allPermissions}
          onAddGroup={onAddGroup}
          onRemoveGroup={onRemoveGroup}
          removedActiveGroups={removedActiveGroups}
          searchValue={searchValue}
        />
      ),
    },
  ];

  const onDeleteHanlder = () => {
    props.onDelete && props.onDelete(selected.id);
    history.push(`/settings/groups`);
  };

  const onCloneHandler = () => {
    props.onClone && props.onClone(selected);
    history.push(`/settings/groups`);
  };

  const menuItems: MenuItemProps[] = [
    {
      className: "clone-menu-item",
      icon: "duplicate",
      onSelect: () => onCloneHandler(),
      text: createMessage(CLONE_GROUP),
      label: "clone",
    },
    {
      className: "rename-menu-item",
      icon: "edit-underline",
      text: createMessage(RENAME_GROUP),
      label: "rename",
    },
    {
      className: "delete-menu-item",
      icon: "delete-blank",
      onSelect: () => onDeleteHanlder(),
      text: createMessage(DELETE_GROUP),
      label: "delete",
    },
  ];

  return (
    <div data-testid="t--user-edit-wrapper" style={{ width: "100%" }}>
      <BackButton />
      <PageHeader
        buttonText={createMessage(ADD_USERS)}
        isEditingTitle={selected.isNew}
        isTitleEditable
        onButtonClick={onButtonClick}
        onEditTitle={onEditTitle}
        onSearch={onSearch}
        pageMenuItems={menuItems}
        searchPlaceholder={createMessage(SEARCH_PLACEHOLDER)}
        title={pageTitle}
      />
      <TabsWrapper data-testid="t--user-edit-tabs-wrapper">
        <TabComponent
          onSelect={setSelectedTabIndex}
          selectedIndex={selectedTabIndex}
          tabs={tabs}
        />
      </TabsWrapper>
      {isSaving && (
        <SaveButtonBar onClear={onClearChanges} onSave={onSaveChanges} />
      )}
      <FormDialogComponent
        Form={WorkspaceInviteUsersForm}
        canOutsideClickClose
        customProps={{
          isAclFlow: true,
          disableEmailSetup: true,
          disableManageUsers: true,
          disableUserList: true,
          isMultiSelectDropdown: true,
          disableDropdown: true,
        }}
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        title={createMessage(INVITE_USERS_SUBMIT_BUTTON_TEXT)}
        trigger
      />
    </div>
  );
}
