import { ReduxActionTypes } from "@appsmith/constants/ReduxActionConstants";
import actionsReducer from "./actionsReducer";
import type { ActionDataState } from "./actionsReducer";

describe("actionsReducer", () => {
  it("should return the initial state", () => {
    const initialState: ActionDataState = [];
    const action = { type: "UNKNOWN_ACTION", payload: {} };
    const state = actionsReducer(undefined, action);
    expect(state).toEqual(initialState);
  });

  it("should handle FETCH_MODULE_ACTIONS_SUCCESS action", () => {
    const initialState: ActionDataState = [];
    const actionPayload = [
      {
        id: "65265ab24b7c8d700a10265e",
        moduleId: "652519c44b7c8d700a102643",
        actionConfiguration: {
          timeoutInMillisecond: 10000,
          paginationType: "NONE",
        },
        executeOnLoad: false,
        isValid: true,
        validName: "Api4",
        entityReferenceType: "ACTION",
        executableConfiguration: {
          timeoutInMillisecond: 10000,
        },
        configurationPath: "Api4.actionConfiguration",
      },
      {
        id: "6526621d4b7c8d700a102663",
        moduleId: "652519c44b7c8d700a102643",
        actionConfiguration: {
          timeoutInMillisecond: 10000,
          paginationType: "NONE",
          encodeParamsToggle: true,
          selfReferencingDataPaths: [],
        },
        executeOnLoad: false,
        isValid: true,
      },
    ];

    const action = {
      type: ReduxActionTypes.FETCH_MODULE_ACTIONS_SUCCESS,
      payload: actionPayload,
    };

    const state = actionsReducer(initialState, action);

    expect(state.length).toBe(actionPayload.length);

    // Check if the state contains the expected data
    actionPayload.forEach((action, index) => {
      const stateAction = state[index];
      expect(stateAction.data).toBe(undefined);
      expect(stateAction.isLoading).toBe(false);
      expect(stateAction.config).toBe(action);
    });

    // Check if the state retains any additional items not replaced
    const additionalAction = {
      id: "6525302c4b7c8d700a10264a",
      moduleId: "652519c44b7c8d700a102640",
      actionConfiguration: {
        timeoutInMillisecond: 10000,
        paginationType: "NONE",
      },
      executeOnLoad: false,
      isValid: true,
      eventData: {},
      selfReferencingDataPaths: [],
    };

    const updatedState: ActionDataState = actionsReducer(state, {
      type: ReduxActionTypes.FETCH_MODULE_ACTIONS_SUCCESS,
      payload: [additionalAction],
    });

    expect(updatedState.length).toBe(actionPayload.length + 1);

    // Check if the additional action is present in the state
    expect(
      updatedState.some((action) => action.config.id === additionalAction.id),
    ).toBe(true);
  });

  // Add more test cases as needed
});
