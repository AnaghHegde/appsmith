export * from "ce/components/editorComponents/GPT/trigger";

import { ReduxActionTypes } from "@appsmith/constants/ReduxActionConstants";
import classNames from "classnames";
import React, { useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useLocation } from "react-router";
import MagicIcon from "remixicon-react/MagicLineIcon";
import { getEntityInCurrentPath } from "sagas/RecentEntitiesSagas";
import { selectIsAIWindowOpen } from "./utils";
import { selectFeatureFlags } from "selectors/usersSelectors";
import { Colors } from "constants/Colors";
import { getActionsForCurrentPage } from "selectors/entitiesSelector";

export const addAISlashCommand = true;

export function GPTTrigger() {
  const dispatch = useDispatch();
  const location = useLocation();
  const pageInfo = useMemo(
    () => getEntityInCurrentPath(location.pathname),
    [location.pathname],
  );
  const { id, pageType } = pageInfo || {};
  const actions = useSelector(getActionsForCurrentPage);
  const featureFlags = useSelector(selectFeatureFlags);
  let hide =
    !["jsEditor", "canvas", "queryEditor"].includes(pageType || "") ||
    !featureFlags.ask_ai;
  const windowOpen = useSelector(selectIsAIWindowOpen);
  if (pageType === "queryEditor") {
    const action = actions.find((action) => action.config.id === id);
    // If the action is not a SQL query, hide the AI button
    if (action?.config.actionConfiguration.hasOwnProperty("formData")) {
      hide = true;
    }
  }

  const toggleWindow = () => {
    dispatch({
      type: ReduxActionTypes.TOGGLE_AI_WINDOW,
      payload: { show: !windowOpen },
    });
  };
  return (
    <div
      className={classNames({
        "flex flex-row gap-1 px-4 h-full items-center border-l border-l-[#E7E7E7] cursor-pointer hover:bg-[#F1F1F1]":
          true,
        hidden: hide,
      })}
      onClick={toggleWindow}
    >
      <MagicIcon color={Colors.SCORPION} size={16} />
      <span className="text-xs">Ask AI</span>
    </div>
  );
}
