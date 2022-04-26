import Menu from "pages/Editor/gitSync/Menu";
import React, { useEffect, useState } from "react";
import { useParams } from "react-router";
import { connect, useDispatch } from "react-redux";
import { formValueSelector, InjectedFormProps, reduxForm } from "redux-form";
import _ from "lodash";
import { AppState } from "reducers";
import { BASE_URL } from "constants/routes";
import { REDIRECT_URL_FORM, ENTITYID_URL_FORM } from "constants/forms";
import { getSettingsSavingState } from "selectors/settingsSelectors";
import SaveAdminSettings from "pages/Settings/SaveSettings";
import AdminConfig from "pages/Settings/config";
import { Callout } from "components/ads/CalloutV2";
import { CopyUrlReduxForm } from "components/ads/formFields/CopyUrlForm";
import {
  BodyContainer,
  Info,
  MenuContainer,
  HeaderSecondary,
  RenderForm,
  SettingsFormWrapper,
  InputProps,
} from "./components";
import { SSO_IDENTITY_PROVIDER_FORM } from "@appsmith/constants/forms";
import { fetchSamlMetadata } from "@appsmith/actions/settingsAction";
import {
  createMessage,
  ENTITY_ID_TOOLTIP,
  REDIRECT_URL_TOOLTIP,
  MANDATORY_FIELDS_ERROR,
} from "@appsmith/constants/messages";
import { Toaster, Variant } from "components/ads";

export type MenuItemsProps = {
  id: string;
  key: MENU_ITEM;
  title: string;
  subText: string;
  inputs: InputProps[];
};

export enum MENU_ITEM {
  METADATA_URL = "METADATA_URL",
  XML = "XML",
  IDP_DATA = "IDP_DATA",
}

export const MENU_ITEMS_MAP: MenuItemsProps[] = [
  {
    id: "APPSMITH_SSO_SAML_METADATA_URL",
    key: MENU_ITEM.METADATA_URL,
    title: "Metadata URL",
    subText: "Provide a Metadata URL to retrieve the IdP details.",
    inputs: [
      {
        className: "t--sso-metadata-url-input",
        label: "Metadata URL",
        name: "metadataUrl",
        isRequired: true,
      },
    ],
  },
  {
    id: "APPSMITH_SSO_SAML_METADATA_XML",
    key: MENU_ITEM.XML,
    title: "XML",
    subText: "Paste the raw Metadata XML IdP here.",
    inputs: [
      {
        className: "t--sso-metadata-xml-input",
        label: "Metadata XML",
        name: "metadataXml",
        type: "Area",
        isRequired: true,
      },
    ],
  },
  {
    id: "APPSMITH_SSO_SAML_IDP_DATA",
    key: MENU_ITEM.IDP_DATA,
    title: "IdP Data",
    subText: "Provide your individual Identity Provider metadata fields.",
    inputs: [
      {
        className: "t--sso-metadata-entity-id-input",
        label: "Entity ID",
        name: "metadataEntityId",
        isRequired: true,
      },
      {
        className: "t--sso-metadata-sso-url-input",
        label: "Single Sign On URL",
        name: "metadataSsoUrl",
        isRequired: true,
      },
      {
        className: "t--sso-metadata-pub-cert-input",
        label: "X509 Public Certificate",
        name: "metadataPubCert",
        isRequired: true,
      },
      {
        className: "t--email-input",
        label: "Email",
        hint: "Configure the mapping of IdP attribute keys for email.",
        name: "metadataEmail",
        isRequired: true,
      },
    ],
  },
];

export type MetadataFormValuesType = {
  metadataPubCert?: string;
  metadataEmail?: string;
  metadataSsoUrl?: string;
  metadataUrl?: string;
  metadataXml?: string;
};

const allSAMLSetupOptions = Object.values(MENU_ITEMS_MAP);

type FormProps = {
  settings: Record<string, string>;
  isSaving: boolean;
};

function MetadataForm(
  props: InjectedFormProps &
    FormProps & {
      activeTabIndex: number;
    },
) {
  const params = useParams() as any;
  const { category, subCategory } = params;
  const dispatch = useDispatch();
  const isSavable = AdminConfig.savableCategories.includes(
    subCategory ?? category,
  );
  const { activeTabIndex = 0 } = props;
  const providerForm = allSAMLSetupOptions[activeTabIndex];

  const onClear = () => {
    _.forEach(props.settings, (value, settingName) => {
      props.settings[settingName] = "";
    });
    if (activeTabIndex === 2) {
      props.settings[
        "metadataEntityId"
      ] = `${window.location.origin}${BASE_URL}auth/realms/appsmith`;
    }
    props.initialize(props.settings);
  };

  useEffect(onClear, [activeTabIndex]);

  const submit = () => {
    const {
      metadataEmail,
      metadataEntityId,
      metadataPubCert,
      metadataSsoUrl,
      metadataUrl,
      metadataXml,
    } = props.settings;
    if (activeTabIndex === 0 && metadataUrl?.toString().trim()) {
      dispatch(
        fetchSamlMetadata({
          isEnabled: true,
          importFromUrl: metadataUrl,
        }),
      );
    } else if (activeTabIndex === 1 && metadataXml?.toString().trim()) {
      dispatch(
        fetchSamlMetadata({
          isEnabled: true,
          importFromXml: metadataXml,
        }),
      );
    } else if (
      activeTabIndex === 2 &&
      metadataEmail?.toString().trim() &&
      metadataSsoUrl?.toString().trim() &&
      metadataPubCert?.toString().trim() &&
      metadataEntityId?.toString().trim()
    ) {
      dispatch(
        fetchSamlMetadata({
          isEnabled: true,
          configuration: {
            singleSignOnServiceUrl: metadataSsoUrl,
            signingCertificate: metadataPubCert,
            emailField: metadataEmail,
          },
        }),
      );
    } else {
      Toaster.show({
        text: createMessage(MANDATORY_FIELDS_ERROR),
        variant: Variant.danger,
      });
    }
  };

  return (
    <>
      <Info>{providerForm.subText}</Info>
      <RenderForm inputs={providerForm.inputs} />
      {isSavable && (
        <SaveAdminSettings
          isSaving={props.isSaving}
          onClear={onClear}
          onSave={submit}
          settings={props.settings}
          valid={props.valid}
        />
      )}
    </>
  );
}

const validate = (values: Record<string, any>) => {
  const errors: any = {};
  _.filter(values, (value, name) => {
    const message = AdminConfig.validate(name, value);
    if (message) {
      errors[name] = message;
    }
  });
  return errors;
};

const selector = formValueSelector(SSO_IDENTITY_PROVIDER_FORM);

const MetadataReduxForm = connect(
  (
    state: AppState,
    props: {
      activeTabIndex: number;
    },
  ) => {
    const newProps: any = {
      settings: {},
      isSaving: getSettingsSavingState(state),
    };
    _.forEach(MENU_ITEMS_MAP, (setting, index) => {
      if (index === props.activeTabIndex) {
        _.forEach(setting.inputs, (input) => {
          const fieldValue = selector(state, input.name);
          if (fieldValue !== newProps.settings[input.name]) {
            newProps.settings[input.name] = fieldValue;
          }
        });
      }
    });
    return newProps;
  },
  null,
)(
  reduxForm<any, any>({
    validate,
    form: SSO_IDENTITY_PROVIDER_FORM,
    touchOnBlur: true,
    enableReinitialize: true,
  })(MetadataForm),
);

function ReadMetadata() {
  const [activeTabIndex, setActiveTabIndex] = useState(0);

  return (
    <SettingsFormWrapper>
      <CopyUrlReduxForm
        fieldName={"redirect-url-form"}
        form={REDIRECT_URL_FORM}
        helpText={"Paste this URL in your IdP service providers console."}
        title={"Redirect URL"}
        tooltip={createMessage(REDIRECT_URL_TOOLTIP)}
        value={`${BASE_URL}auth/realms/appsmith/broker/saml/endpoint`}
      />
      {activeTabIndex !== 2 && (
        <CopyUrlReduxForm
          fieldName={"entity-id--url-form"}
          form={ENTITYID_URL_FORM}
          helpText={"Paste this URL in your IdP service providers console."}
          title={"Entity ID"}
          tooltip={createMessage(ENTITY_ID_TOOLTIP)}
          value={`${BASE_URL}auth/realms/appsmith`}
        />
      )}
      <HeaderSecondary>Register Identity Provider</HeaderSecondary>
      <MenuContainer>
        <Menu
          activeTabIndex={activeTabIndex}
          onSelect={setActiveTabIndex}
          options={allSAMLSetupOptions}
        />
      </MenuContainer>
      <Callout
        actionLabel="Read Documentation"
        title="Cannot locate raw Metadata XML?"
        type="Info"
      />
      <BodyContainer>
        <MetadataReduxForm activeTabIndex={activeTabIndex} />
      </BodyContainer>
    </SettingsFormWrapper>
  );
}

export default ReadMetadata;
