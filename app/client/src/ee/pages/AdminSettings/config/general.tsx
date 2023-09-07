export * from "ce/pages/AdminSettings/config/general";
import type {
  Setting,
  AdminConfigType,
} from "@appsmith/pages/AdminSettings/config/types";
import {
  SettingCategories,
  SettingTypes,
} from "@appsmith/pages/AdminSettings/config/types";
import { isAirgapped } from "@appsmith/utils/airgapHelpers";

import {
  config as CE_config,
  APPSMITH_INSTANCE_NAME_SETTING_SETTING,
  APPSMITH__ADMIN_EMAILS_SETTING,
  APPSMITH_DOWNLOAD_DOCKER_COMPOSE_FILE_SETTING,
  APPSMITH_DISABLE_TELEMETRY_SETTING,
  APPSMITH_HIDE_WATERMARK_SETTING as CE_APPSMITH_HIDE_WATERMARK_SETTING,
  APPSMITH_ALLOWED_FRAME_ANCESTORS_SETTING,
} from "ce/pages/AdminSettings/config/general";

import store from "store";
import { isBrandingEnabled } from "ce/utils/planHelpers";
import { selectFeatureFlags } from "@appsmith/selectors/featureFlagsSelectors";
import { isUserSessionLimitEnabled } from "@appsmith/utils/planHelpers";
const featureFlags = selectFeatureFlags(store.getState());
const isBrandingFFEnabled = isBrandingEnabled(featureFlags);
const isSessionLimitEnabled = isUserSessionLimitEnabled(featureFlags);

export const APPSMITH_HIDE_WATERMARK_SETTING: Setting = {
  ...CE_APPSMITH_HIDE_WATERMARK_SETTING,
  isFeatureEnabled: isBrandingFFEnabled,
  isDisabled: () => !isBrandingFFEnabled,
};

export const APPSMITH_SINGLE_USER_PER_SESSION_SETTING: Setting = {
  id: "singleSessionPerUserEnabled",
  name: "singleSessionPerUserEnabled",
  category: SettingCategories.GENERAL,
  controlType: SettingTypes.CHECKBOX,
  label: "User session limit",
  text: "Limit users to a single active session",
};

export const APPSMITH_SHOW_ROLES_AND_GROUPS_SETTING: Setting = {
  id: "showRolesAndGroups",
  name: "showRolesAndGroups",
  category: SettingCategories.GENERAL,
  controlType: SettingTypes.CHECKBOX,
  label: "Programmatic access control",
  text: "Access roles and user groups in code for conditional business logic",
};

const isAirgappedInstance = isAirgapped();

const settings = [
  APPSMITH_INSTANCE_NAME_SETTING_SETTING,
  APPSMITH__ADMIN_EMAILS_SETTING,
  APPSMITH_DOWNLOAD_DOCKER_COMPOSE_FILE_SETTING,
  APPSMITH_DISABLE_TELEMETRY_SETTING,
  APPSMITH_HIDE_WATERMARK_SETTING,
  APPSMITH_SINGLE_USER_PER_SESSION_SETTING,
  APPSMITH_SHOW_ROLES_AND_GROUPS_SETTING,
  APPSMITH_ALLOWED_FRAME_ANCESTORS_SETTING,
];

const removalSettings: Setting[] = [];

if (isAirgappedInstance) {
  removalSettings.push(APPSMITH_DISABLE_TELEMETRY_SETTING);
}

if (!isSessionLimitEnabled) {
  removalSettings.push(APPSMITH_SINGLE_USER_PER_SESSION_SETTING);
}

export const config: AdminConfigType = {
  ...CE_config,
  settings: settings.filter((item) => !removalSettings.includes(item)),
} as AdminConfigType;
