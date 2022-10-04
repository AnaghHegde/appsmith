export * from "ce/constants/messages";

export const SHOW_LESS_GROUPS = () => `show less`;
export const SHOW_MORE_GROUPS = (count: number) => `show ${count} more`;
export const ADD_USERS = () => `Add Users`;
export const ADD_GROUP = () => `Add Group`;
export const ADD_ROLE = () => `Add Role`;
export const SEARCH_PLACEHOLDER = () => `Search`;
export const SEARCH_GROUPS_PLACEHOLDER = () => `Search groups`;
export const SEARCH_ROLES_PLACEHOLDER = () => `Search roles`;
export const GROUPS_UPDATED_SUCCESS = () => `Groups updated successfully`;
export const GROUP_DELETED = () => `Group deleted successfully`;
export const GROUP_CLONED = () => `Group cloned successfully`;
export const RENAME_SUCCESSFUL = () => `Rename successful`;
export const GROUP_UPDATED_SUCCESS = () => `Group updated successfully`;
export const SUCCESSFULLY_SAVED = () => `Successfully Saved`;
export const ENTER_GROUP_NAME = () => `Enter group name`;
export const ACTIVE_ENTITIES = (entity: string) => `Active ${entity}s`;
export const ALL_ENTITIES = (entity: string) => `All ${entity}s`;
export const ADD_ENTITY = (entity: string) => `Add ${entity}`;
export const REMOVE_ENTITY = (entity: string) => `Remove ${entity}`;
export const DELETE_USER = () => `Delete`;
export const DELETE_GROUP = () => `Delete`;
export const DELETE_ROLE = () => `Delete`;
export const REMOVE_USER = () => `Remove`;
export const CLONE_GROUP = () => `Clone`;
export const CLONE_ROLE = () => `Clone`;
export const RENAME_GROUP = () => `Rename`;
export const RENAME_ROLE = () => `Rename`;
export const EDIT_GROUP = () => `Edit`;
export const EDIT_ROLE = () => `Edit`;
export const NO_USERS_MESSAGE = () => `There are no users added to this group`;
export const NO_ROLES_MESSAGE = () => `There are no roles assigned yet. Choose from the list of
roles below to add them.`;
export const BOTTOM_BAR_SAVE_MESSAGE = () => `These changes will affect the users ability to interact with various
aspects of the application. Are you sure?`;
export const BOTTOM_BAR_SAVE_BTN = () => `Save Changes`;
export const BOTTOM_BAR_CLEAR_BTN = () => `Clear`;
