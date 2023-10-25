export * from "ce/actions/environmentAction";

import { ReduxActionTypes } from "@appsmith/constants/ReduxActionConstants";
import type { CurrentEnvironmentDetails } from "@appsmith/reducers/environmentReducer";
import { saveCurrentEnvironment } from "utils/storage";

// Redux action to initiate fetching the environment configs
export const fetchingEnvironmentConfigs = (workspaceId: string) => ({
  type: ReduxActionTypes.FETCH_ENVIRONMENT_INIT,
  payload: workspaceId,
});

// Redux action to update the current environment details
export const setCurrentEnvironment = (
  currentEnvDetails: CurrentEnvironmentDetails,
) => {
  saveCurrentEnvironment(currentEnvDetails.id, currentEnvDetails.appId);
  return {
    type: ReduxActionTypes.SET_CURRENT_ENVIRONMENT,
    payload: currentEnvDetails,
  };
};

// Redux action to update the current editing environment ID
export const setCurrentEditingEnvironmentID = (currentEditingId: string) => {
  return {
    type: ReduxActionTypes.SET_CURRENT_EDITING_ENVIRONMENT,
    payload: { currentEditingId },
  };
};
