import { featureFlagIntercept } from "../../../../support/Objects/FeatureFlags";
import {
  agHelper,
  dataSources,
  deployMode,
  entityExplorer,
  locators,
  tedTestConfig,
  apiPage,
} from "../../../../support/Objects/ObjectsCore";
import { Widgets } from "../../../../support/Pages/DataSources";
import { EntityItems } from "../../../../support/Pages/AssertHelper";
import { multipleEnv } from "../../../../support/ee/ObjectsCore_EE";
import { data } from "cypress/types/jquery";

let meDatasourceName: string,
  meQueryName: string,
  prodEnv: string,
  stagingEnv: string,
  APIName: any;

//TODO: add exclude for airgap
describe("Multiple environment datasource creation and test flow", function () {
  before(() => {
    // Need to remove the previous user preference for the callout
    window.localStorage.removeItem("userPreferenceDismissEnvCallout");
    featureFlagIntercept({ release_datasource_environments_enabled: true });
    prodEnv = tedTestConfig.defaultEnviorment;
    stagingEnv = tedTestConfig.environments[1];
    multipleEnv.SwitchEnv(prodEnv);
    meQueryName = "rest_select";
  });

  it("1. Creates a new Authenticated API ds for both envs", function () {
    dataSources.NavigateToDSCreateNew();
    agHelper.GetNClick(dataSources._authApiDatasource, 0, true);
    agHelper.GenerateUUID();
    cy.get("@guid").then((uid) => {
      APIName = uid;
      agHelper.RenameWithInPane(APIName, false);
    });
    // Fill Auth Form
    agHelper.UpdateInput(
      locators._inputFieldByName("URL"),
      tedTestConfig.dsValues[prodEnv].ApiUrlME,
    );
    agHelper.Sleep(500);
    dataSources.SaveDatasource(false, true);
    // Add staging env details
    dataSources.EditDatasource();
    multipleEnv.SwitchEnvInDSEditor(stagingEnv);
    // Enter wrong values and test
    agHelper.UpdateInput(
      locators._inputFieldByName("URL"),
      tedTestConfig.dsValues[stagingEnv].ApiUrlME,
    );
    // Save env details
    dataSources.SaveDatasource(false, true);
  });

  it("2. Create and test query responses for both ds on both environmets and add to a table", function () {
    // Create a query on the ME ds
    agHelper.GetNClick(dataSources._createQuery);
    cy.get(apiPage._editorDS).type("/getResponse");
    agHelper.Sleep();
    apiPage.RunAPI();
    // Check both query responses on staging
    multipleEnv.SwitchEnv(stagingEnv);
    agHelper.Sleep();
    apiPage.RunAPI();
    dataSources.AddSuggesstedWidget(Widgets.Table);
  });

  it("3. Check table response for both environments", function () {
    // Check the records on the table
    cy.get(locators._tableRecordsContainer).should("contain", "2 Records");
    entityExplorer.SelectEntityByName("Table1", "Widgets");
    multipleEnv.SwitchEnv(prodEnv);
    cy.get(locators._tableRecordsContainer).should("contain", "1 Record");
  });

  it("4. Deploy the app, check for modal and check table response for both envs", function () {
    // Need to remove the previous user preference for the callout
    window.localStorage.removeItem("userPreferenceDismissEnvCallout");
    agHelper.Sleep();
    deployMode.DeployApp(undefined, true, true, true, "present");
    featureFlagIntercept({ release_datasource_environments_enabled: true });
    // Check for env switcher
    agHelper.AssertElementExist(multipleEnv.env_switcher);
    // Check table values
    multipleEnv.SwitchEnv(prodEnv);
    cy.get(locators._tableRecordsContainer).should("contain", "1 Records");
    multipleEnv.SwitchEnv(stagingEnv);
    cy.get(locators._tableRecordsContainer).should("contain", "2 Records");
    deployMode.NavigateBacktoEditor();
    multipleEnv.SwitchEnv(prodEnv);
    // Clean up
    entityExplorer.SelectEntityByName("Table1", "Widgets");
    entityExplorer.ActionContextMenuByEntityName({
      entityNameinLeftSidebar: "Table1",
      action: "Delete",
      entityType: EntityItems.Widget,
    });
  });
});
