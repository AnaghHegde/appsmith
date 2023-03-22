export * from "ce/selectors/tenantSelectors";
import { Status } from "@appsmith/pages/Billing/StatusBadge";
import { LICENSE_TYPE } from "@appsmith/pages/Billing/types";
import type { AppState } from "@appsmith/reducers";
import { getRemainingDaysFromTimestamp } from "@appsmith/utils/billingUtils";
import { EE_PERMISSION_TYPE } from "@appsmith/utils/permissionHelpers";
import { createSelector } from "reselect";

export const isValidLicense = (state: AppState) => {
  return state.tenant?.tenantConfiguration?.license?.active;
};

export const getLicenseType = (state: AppState) => {
  return state.tenant?.tenantConfiguration?.license?.type;
};

export const getLicenseExpiry = (state: AppState) => {
  const date = new Date(
    state.tenant?.tenantConfiguration?.license?.expiry * 1000,
  ).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
  return date;
};

export const getLicenseKey = (state: AppState) => {
  return state.tenant?.tenantConfiguration?.license?.key;
};

export const getLicenseId = (state: AppState) => {
  return state.tenant?.tenantConfiguration?.license?.id;
};

export const getLicenseDetails = (state: AppState) => {
  return state.tenant?.tenantConfiguration?.license;
};

export const getExpiry = (state: AppState) => {
  return state.tenant?.tenantConfiguration?.license?.expiry;
};

export const getRemainingDays = createSelector(getExpiry, (expiry) => {
  const timeStamp = expiry * 1000;
  return getRemainingDaysFromTimestamp(timeStamp);
});

export const isTrialLicense = (state: AppState) =>
  state.tenant?.tenantConfiguration?.license?.type === LICENSE_TYPE.TRIAL;

export const isLicensePaymentFailed = (state: AppState) =>
  state.tenant?.tenantConfiguration?.license?.status ===
  LICENSE_TYPE.PAYMENT_FAILED;

export const isBEBannerVisible = (state: AppState) => {
  const value = state.tenant?.tenantConfiguration?.license?.showBEBanner;
  return value;
};

export const shouldShowLicenseBanner = (state: AppState) => {
  const isTrialLicenseOrFailed =
    isTrialLicense(state) ||
    (isLicensePaymentFailed(state) && isAdminUser(state));
  const isBEBanner = isBEBannerVisible(state);
  return !isBEBanner && isTrialLicenseOrFailed;
};

export const hasInvalidLicenseKeyError = (state: AppState) => {
  return state.tenant.tenantConfiguration?.license?.invalidLicenseKeyError;
};

export const getLicenseStatus = (state: AppState) => {
  const isLicenseValid = isValidLicense(state);
  const isTrial = isTrialLicense(state);

  if (isLicenseValid) {
    if (isTrial) {
      return Status.TRIAL;
    } else {
      return Status.ACTIVE;
    }
  } else {
    return Status.INACTIVE;
  }
};

export const isAdminUser = (state: AppState) =>
  state.tenant?.userPermissions?.includes(EE_PERMISSION_TYPE.MANAGE_TENANTS);

export const isLicenseValidating = (state: AppState) =>
  state.tenant?.tenantConfiguration?.license?.validatingLicense;

export const getLicenseOrigin = (state: AppState) =>
  state.tenant?.tenantConfiguration?.license?.origin;

export const isLicenseModalOpen = (state: AppState) =>
  state.tenant?.tenantConfiguration?.license?.showLicenseModal;
