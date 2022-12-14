import React from "react";
import { useParams } from "react-router-dom";
import { useSelector } from "react-redux";
import {
  Categories,
  getSettingsCategory,
  HeaderContainer,
  StyledHeader,
  Wrapper,
} from "ce/pages/AdminSettings/LeftPane";
import { AclFactory } from "./config";
import { getCurrentUser } from "selectors/usersSelectors"; //selectFeatureFlags removed for now
import { Category } from "./config/types";
import { getTenantPermissions } from "@appsmith/selectors/tenantSelectors";
import {
  isPermitted,
  PERMISSION_TYPE,
} from "@appsmith/utils/permissionHelpers";

export * from "ce/pages/AdminSettings/LeftPane";

function getAclCategory() {
  return Array.from(AclFactory.categories);
}

export default function LeftPane() {
  const categories = getSettingsCategory();
  const aclCategories = getAclCategory();
  /** otherCategories will be built from its own factory in future;
   * The last value in `categories` (ATM) is AuditLogs.
   * */
  const othersCategories: Category[] = [categories.splice(-1, 1)[0]];
  const { category, selected: subCategory } = useParams() as any;
  // const featureFlags = useSelector(selectFeatureFlags);
  const user = useSelector(getCurrentUser);
  const isSuperUser = user?.isSuperUser;

  const tenantPermissions = useSelector(getTenantPermissions);
  const isAuditLogsEnabled = isPermitted(
    tenantPermissions,
    PERMISSION_TYPE.READ_AUDIT_LOGS,
  );

  const filteredAclCategories = aclCategories
    ?.map((category) => {
      if (category.title === "Users" && !isSuperUser) {
        return null;
      }
      return category;
    })
    .filter(Boolean) as Category[];

  return (
    <Wrapper>
      {isSuperUser && (
        <HeaderContainer>
          <StyledHeader>Admin Settings</StyledHeader>
          <Categories
            categories={categories}
            currentCategory={category}
            currentSubCategory={subCategory}
          />
        </HeaderContainer>
      )}
      {/* {featureFlags.RBAC && ( */}
      <HeaderContainer>
        <StyledHeader>Access Control</StyledHeader>
        <Categories
          categories={filteredAclCategories}
          currentCategory={category}
          currentSubCategory={subCategory}
        />
      </HeaderContainer>
      {/* )} */}
      {isAuditLogsEnabled && (
        <HeaderContainer>
          <StyledHeader>Others</StyledHeader>
          <Categories
            categories={othersCategories}
            currentCategory={category}
            currentSubCategory={subCategory}
          />
        </HeaderContainer>
      )}
    </Wrapper>
  );
}
