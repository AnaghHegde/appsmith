package com.appsmith.server.solutions;

import com.appsmith.server.acl.AclPermission;
import com.appsmith.server.solutions.ce.DatasourcePermissionCE;

public interface DatasourcePermission extends DatasourcePermissionCE {
    AclPermission getActionCreatePermission();
}
