import React, { useEffect, useState } from "react";
import styled from "styled-components";
import type { AppState } from "@appsmith/reducers";
import { connect } from "react-redux";
import type { EnvironmentType } from "@appsmith/reducers/environmentReducer";
import {
  ENVIRONMENT_QUERY_KEY,
  updateLocalStorage,
} from "@appsmith/utils/Environments";
import {
  getDefaultEnvironment,
  getEnvironments,
} from "@appsmith/selectors/environmentSelectors";
import { Option, Select, Text } from "design-system";
import { useFeatureFlagCheck } from "selectors/featureFlagsSelectors";
import { FEATURE_FLAG } from "@appsmith/entities/FeatureFlag";

const Wrapper = styled.div`
  display: flex;
  border-right: 1px solid var(--ads-v2-color-border);
  padding: 0px 16px;

  .rc-select-selector {
    min-width: 129px;
    width: 129px;
    border: none;
  }
`;

type Props = {
  defaultEnvironment?: EnvironmentType;
  environmentList: Array<EnvironmentType>;
};

const SwitchEnvironment = ({ defaultEnvironment, environmentList }: Props) => {
  // state to store the selected environment
  const [selectedEnv, setSelectedEnv] = useState(defaultEnvironment);
  useEffect(() => {
    !!selectedEnv && updateLocalStorage(selectedEnv.name, selectedEnv.id);
  }, [environmentList.length]);
  const allowedToRender = useFeatureFlagCheck(
    FEATURE_FLAG.release_datasource_environments_enabled,
  );

  // function to set the selected environment
  const setSelectedEnvironment = (env: EnvironmentType) => {
    if (env.id !== selectedEnv?.id) {
      const queryParams = new URLSearchParams(window.location.search);
      // Set new or modify existing parameter value.
      queryParams.set(ENVIRONMENT_QUERY_KEY, env.name.toLowerCase());
      updateLocalStorage(env.name, env.id);
      // Replace current querystring with the new one.
      window.history.replaceState({}, "", "?" + queryParams.toString());
      setSelectedEnv(env);
    }
  };
  // skip the render if feature is not enabled or no environments are present
  if (!allowedToRender || environmentList.length <= 0) {
    return null;
  }
  return (
    <Wrapper>
      <Select
        className="select_environemnt"
        dropdownClassName="select_environemnt_dropdown"
        onSelect={setSelectedEnvironment}
        value={
          selectedEnv &&
          selectedEnv.name.charAt(0).toUpperCase() + selectedEnv.name.slice(1)
        }
      >
        {environmentList.map((role: any) => (
          <Option key={role.id} label={role.name} value={role}>
            <div className="flex flex-col gap-1">
              <Text
                color="var(--ads-v2-color-fg-emphasis)"
                kind={role.description && "heading-xs"}
              >
                {role.name.charAt(0).toUpperCase() + role.name.slice(1)}
              </Text>
            </div>
          </Option>
        ))}
      </Select>
    </Wrapper>
  );
};

const mapStateToProps = (state: AppState) => {
  return {
    environmentList: getEnvironments(state),
    defaultEnvironment: getDefaultEnvironment(state),
  };
};

export default connect(mapStateToProps)(SwitchEnvironment);
