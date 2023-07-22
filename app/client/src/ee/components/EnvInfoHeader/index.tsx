import React, { useState } from "react";
import { useSelector } from "react-redux";
import { Callout, Link } from "design-system";
import { getCurrentWorkspaceId } from "ce/selectors/workspaceSelectors";
import { areEnvironmentsFetched } from "ce/selectors/environmentSelectors";
import type { AppState } from "@appsmith/reducers";
import {
  getUserPreferenceFromStorage,
  setUserPreferenceInStorage,
} from "@appsmith/utils/Environments";
import InfoContent from "./InfoContent";
import {
  createMessage,
  ENV_INFO_MODAL_DISMISS_ACTION,
} from "@appsmith/constants/messages";

// show only if envs are fetched and the user has not clicked on `Don't show me again` before
export function EnvInfoHeader() {
  const [userPreference, setUserPreference] = useState(
    getUserPreferenceFromStorage(),
  );

  const workspaceId = useSelector(getCurrentWorkspaceId);
  const showInfoCallout = useSelector((state: AppState) =>
    areEnvironmentsFetched(state, workspaceId),
  );

  if ((!!userPreference && userPreference === "true") || !showInfoCallout) {
    return null;
  }
  return (
    <Callout kind="info">
      <InfoContent />
      <Link
        kind="secondary"
        onClick={() => {
          setUserPreference(setUserPreferenceInStorage());
        }}
      >
        {createMessage(ENV_INFO_MODAL_DISMISS_ACTION)}
      </Link>
    </Callout>
  );
}
