import LicenseLocators from "../../../../locators/LicenseLocators.json";
import commonlocators from "../../../../locators/commonlocators.json";
import * as _ from "../../../../support/Objects/ObjectsCore";

describe("excludeForAirgap", "Trial License", function () {
  before(() => {
    cy.interceptLicenseApi({
      licenseStatus: "ACTIVE",
      licenseType: "TRIAL",
    });
    cy.window().then((win) => {
      win.localStorage.setItem("showLicenseBanner", JSON.stringify(true));
    });
    cy.LogOut();
    cy.LoginFromAPI(Cypress.env("USERNAME"), Cypress.env("PASSWORD"));
    cy.window()
      .its("localStorage.showLicenseBanner")
      .should("eq", JSON.stringify(true));
    cy.wait(2000);
    cy.closeWelcomeBanner();
  });
  it("1. should show warning banner and upgrade CTA on left pane on Homepage", function () {
    cy.interceptLicenseApi({
      licenseStatus: "ACTIVE",
      licenseType: "TRIAL",
    });
    cy.LogOut();
    cy.LoginFromAPI(Cypress.env("USERNAME"), Cypress.env("PASSWORD"));
    cy.wait(2000);
    cy.get(LicenseLocators.welcomeBanner).should("not.exist");
    cy.get(LicenseLocators.upgradeLeftPane).should("be.visible");
    cy.get(LicenseLocators.warningBanner).should("be.visible");
  });
  it("2. should have 30 days left in the trial", () => {
    cy.interceptLicenseApi({ licenseStatus: "ACTIVE", licenseType: "TRIAL" });
    cy.get(LicenseLocators.warningBannerMainText).should(
      "have.text",
      "Your trial will expire in 30 days. ",
    );
    // should have yellow background
    cy.get(LicenseLocators.warningBanner).should(
      "have.css",
      "background-color",
      "rgb(255, 251, 235)",
    );
  });
  it("3. should have red banner for trial left less than 3 days", () => {
    cy.interceptLicenseApi({
      licenseStatus: "ACTIVE",
      licenseType: "TRIAL",
      expiry: (new Date().getTime() + 2 * 24 * 60 * 60 * 1000) / 1000,
    });
    cy.reload();
    cy.get(LicenseLocators.warningBannerMainText).should(
      "have.text",
      "Your trial will expire in 2 days. ",
    );
    // should have red background
    cy.get(LicenseLocators.warningBanner).should(
      "have.css",
      "background-color",
      "rgb(255, 242, 242)",
    );
    // should take the user to customer portal on clicking upgrade CTA
    cy.get(LicenseLocators.warningBanner).within(() => {
      cy.get(LicenseLocators.warningBannerUpgradeBtn).should(
        "have.attr",
        "href",
        "https://customer.appsmith.com/plans",
      );
    });
  });
  it("4. should have red banner with hours remaining in the trial license", () => {
    cy.interceptLicenseApi({
      licenseStatus: "ACTIVE",
      licenseType: "TRIAL",
      expiry: (new Date().getTime() + 8 * 60 * 60 * 1000 + 60 * 1000) / 1000,
    });
    cy.reload();
    cy.get(LicenseLocators.warningBannerMainText).should(
      "have.text",
      "Your trial will expire in 8 hours. ",
    );
  });
  it("5. should not have banner in paid license", () => {
    cy.interceptLicenseApi({ licenseStatus: "ACTIVE", licenseType: "PAID" });
    cy.reload();
    cy.get(LicenseLocators.warningBanner).should("not.exist");
  });
  it("6. should force recheck license on clicking recheck license CTA", () => {
    cy.interceptLicenseApi({
      licenseStatus: "ACTIVE",
      licenseType: "TRIAL",
    });
    cy.reload();
    cy.get(LicenseLocators.warningBannerMainText).should(
      "have.text",
      "Your trial will expire in 30 days. ",
    );
    cy.interceptLicenseApi({
      licenseStatus: "ACTIVE",
      licenseType: "PAID",
      url: "/api/v1/tenants/license",
    });
    cy.get(LicenseLocators.licenseRefreshBtn).click();
    cy.wait(1000);
    cy.get(commonlocators.toastMsg).contains(
      "Your license has been updated successfully",
    );
    cy.get(LicenseLocators.warningBanner).should("not.exist");
  });
  it("7. should have Enterprise text in banner for Enterprise", () => {
    cy.interceptLicenseApi({
      licenseStatus: "ACTIVE",
      licenseType: "TRIAL",
      licenseOrigin: "ENTERPRISE",
    });
    cy.reload();
    cy.get(LicenseLocators.warninngBannerContinueText).should(
      "contain.text",
      "Appsmith Enterprise",
    );
  });
  it("8. should have Business text in banner for Business", () => {
    cy.interceptLicenseApi({
      licenseStatus: "ACTIVE",
      licenseType: "TRIAL",
      licenseOrigin: "SELF_SERVE",
    });
    cy.reload();
    cy.get(LicenseLocators.warninngBannerContinueText).should(
      "contain.text",
      "Appsmith Business",
    );
  });
});
