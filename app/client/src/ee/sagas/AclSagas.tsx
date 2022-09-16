import AclApi, {
  CreateGroupResponse,
  CreateRoleResponse,
  FetchSingleDataPayload,
} from "@appsmith/api/AclApi";
import {
  ReduxAction,
  ReduxActionTypes,
} from "@appsmith/constants/ReduxActionConstants";
import { takeLatest, all, call, put } from "redux-saga/effects";
import { validateResponse } from "sagas/ErrorSagas";

import { ApiResponse } from "api/ApiResponses";
import { User } from "constants/userConstants";
import { RoleProps } from "@appsmith/pages/AdminSettings/acl/types";
import history from "utils/history";

export function* fetchAclUsersSaga() {
  try {
    const response: ApiResponse = yield call(AclApi.fetchAclUsers);
    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_USERS_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_USERS_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.FETCH_ACL_USERS_ERROR,
    });
  }
}

export function* deleteAclUserSaga(action: ReduxAction<any>) {
  try {
    const response: ApiResponse = yield AclApi.deleteAclUser(action.payload);

    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.DELETE_ACL_USER_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.DELETE_ACL_USER_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.DELETE_ACL_USER_ERROR,
    });
  }
}

export function* fetchAclUserSagaById(
  action: ReduxAction<FetchSingleDataPayload>,
) {
  try {
    const response: ApiResponse = yield AclApi.fetchSingleAclUser(
      action.payload,
    );

    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_USER_BY_ID_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_USER_BY_ID_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.FETCH_ACL_USER_BY_ID_ERROR,
    });
  }
}

export function* fetchAclGroupsSaga() {
  try {
    const response: ApiResponse = yield call(AclApi.fetchAclGroups);
    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_GROUPS_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_GROUPS_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.FETCH_ACL_GROUPS_ERROR,
    });
  }
}

export function* fetchAclGroupSagaById(
  action: ReduxAction<FetchSingleDataPayload>,
) {
  try {
    const response: ApiResponse = yield AclApi.fetchSingleAclGroup(
      action.payload,
    );
    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_GROUP_BY_ID_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_GROUP_BY_ID_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.FETCH_ACL_GROUP_BY_ID_ERROR,
    });
  }
}

export function* createAclGroupSaga(action: ReduxAction<any>) {
  try {
    const response: CreateGroupResponse = yield AclApi.createAclGroup(
      action.payload,
    );
    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.CREATE_ACL_GROUP_SUCCESS,
        payload: response.data,
      });
      const role: RoleProps = {
        ...response.data,
        id: response.data.id,
        name: response.data.name,
      };
      history.push(`/settings/groups/${role.id}`);
    } else {
      yield put({
        type: ReduxActionTypes.CREATE_ACL_GROUP_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.CREATE_ACL_GROUP_ERROR,
    });
  }
}

export function* deleteAclGroupSaga(action: ReduxAction<any>) {
  try {
    const response: ApiResponse = yield AclApi.deleteAclGroup(action.payload);

    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.DELETE_ACL_GROUP_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.DELETE_ACL_GROUP_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.DELETE_ACL_GROUP_ERROR,
    });
  }
}

/*export function* cloneGroupSaga(action: ReduxAction<any>) {
  try {
    const response: ApiResponse = yield AclApi.cloneAclGroup(action.payload);

    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.CLONE_ACL_GROUP_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.CLONE_ACL_GROUP_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.CLONE_ACL_GROUP_ERROR,
    });
  }
}*/

export function* updateGroupSaga(action: ReduxAction<any>) {
  try {
    const response: ApiResponse = yield AclApi.updateAclGroup(action.payload);

    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.UPDATE_ACL_GROUP_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.UPDATE_ACL_GROUP_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.UPDATE_ACL_GROUP_ERROR,
    });
  }
}

export function* fetchAclRolesSaga() {
  try {
    const response: ApiResponse = yield call(AclApi.fetchAclRoles);
    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_ROLES_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_ROLES_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.FETCH_ACL_ROLES_ERROR,
    });
  }
}

export function* fetchAclRoleSagaById(
  action: ReduxAction<FetchSingleDataPayload>,
) {
  try {
    const response: ApiResponse = yield AclApi.fetchSingleRole(action.payload);

    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_ROLE_BY_ID_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.FETCH_ACL_ROLE_BY_ID_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.FETCH_ACL_ROLE_BY_ID_ERROR,
    });
  }
}

export function* createAclRoleSaga(action: ReduxAction<any>) {
  try {
    const response: CreateRoleResponse = yield AclApi.createAclRole(
      action.payload,
    );
    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.CREATE_ACL_ROLE_SUCCESS,
        payload: response.data,
      });
      const role: RoleProps = {
        ...response.data,
        id: response.data.id,
        name: response.data.name,
      };
      history.push(`/settings/roles/${role.id}`);
    } else {
      yield put({
        type: ReduxActionTypes.CREATE_ACL_ROLE_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.CREATE_ACL_ROLE_ERROR,
    });
  }
}

export function* deleteAclRoleSaga(action: ReduxAction<any>) {
  try {
    const response: ApiResponse = yield AclApi.deleteAclRole(action.payload);

    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.DELETE_ACL_ROLE_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.DELETE_ACL_ROLE_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.DELETE_ACL_ROLE_ERROR,
    });
  }
}

export function* cloneRoleSaga(action: ReduxAction<any>) {
  try {
    const response: ApiResponse = yield AclApi.cloneAclRole(action.payload);

    const isValidResponse: boolean = yield validateResponse(response);

    if (isValidResponse) {
      yield put({
        type: ReduxActionTypes.CLONE_ACL_ROLE_SUCCESS,
        payload: response.data,
      });
    } else {
      yield put({
        type: ReduxActionTypes.CLONE_ACL_ROLE_ERROR,
      });
    }
  } catch (e) {
    yield put({
      type: ReduxActionTypes.CLONE_ACL_ROLE_ERROR,
    });
  }
}

export function* InitAclSaga(action: ReduxAction<User>) {
  const user = action.payload;
  if (user.isSuperUser) {
    yield all([
      takeLatest(ReduxActionTypes.FETCH_ACL_USERS, fetchAclUsersSaga),
      takeLatest(ReduxActionTypes.FETCH_ACL_USER_BY_ID, fetchAclUserSagaById),
      takeLatest(ReduxActionTypes.FETCH_ACL_GROUPS, fetchAclGroupsSaga),
      takeLatest(ReduxActionTypes.FETCH_ACL_GROUP_BY_ID, fetchAclGroupSagaById),
      takeLatest(ReduxActionTypes.FETCH_ACL_ROLES, fetchAclRolesSaga),
      takeLatest(ReduxActionTypes.FETCH_ACL_ROLE_BY_ID, fetchAclRoleSagaById),
      // takeLatest(ReduxActionTypes.CREATE_ACL_USER, createAclUserSaga),
      takeLatest(ReduxActionTypes.CREATE_ACL_GROUP, createAclGroupSaga),
      takeLatest(ReduxActionTypes.CREATE_ACL_ROLE, createAclRoleSaga),
      takeLatest(ReduxActionTypes.DELETE_ACL_USER, deleteAclUserSaga),
      takeLatest(ReduxActionTypes.DELETE_ACL_GROUP, deleteAclGroupSaga),
      takeLatest(ReduxActionTypes.DELETE_ACL_ROLE, deleteAclRoleSaga),
      // takeLatest(ReduxActionTypes.CLONE_ACL_GROUP, cloneGroupSaga),
      takeLatest(ReduxActionTypes.CLONE_ACL_ROLE, cloneRoleSaga),
      takeLatest(ReduxActionTypes.UPDATE_ACL_GROUP, updateGroupSaga),
    ]);
  }
}

export default function* AclSagas() {
  yield takeLatest(ReduxActionTypes.FETCH_USER_DETAILS_SUCCESS, InitAclSaga);
}
