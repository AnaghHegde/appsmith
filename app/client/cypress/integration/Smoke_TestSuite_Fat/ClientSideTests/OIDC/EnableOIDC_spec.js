const adminSettings = require("../../../../locators/AdminsSettings");
import homePage from "../../../../locators/HomePage";
describe("SSO with OIDC test functionality", function() {
  before(() => {
    cy.intercept("PUT", "/api/v1/admin/env", {
      body: { responseMeta: { status: 200, success: true }, data: {} },
    }).as("postEnvVariables");
  });

  it("1. Go to admin settings and enable OIDC", function() {
    cy.LogOut();
    cy.LoginFromAPI(Cypress.env("USERNAME"), Cypress.env("PASSWORD"));
    cy.visit("/applications");
    cy.get(".t--profile-menu-icon").should("be.visible");
    cy.get(".t--profile-menu-icon").click();
    cy.get(".t--admin-settings-menu").should("be.visible");
    cy.get(".t--admin-settings-menu").click();
    cy.url().should("contain", "/settings/general");
    // click authentication tab
    cy.get(adminSettings.authenticationTab).click();
    cy.url().should("contain", "/settings/authentication");
    cy.get(adminSettings.oidcTab).click();
    cy.wait(2000);
    // fill oidc form
    cy.fillOIDCform();
    // assert server is restarting
    cy.xpath(adminSettings.restartServer).should("be.visible");
    cy.wait("@postEnvVariables").should(
      "have.nested.property",
      "response.body.responseMeta.status",
      200,
    );
    // adding wait for server to restart
    cy.wait(30000);
    cy.waitUntil(() => cy.get(homePage.profileMenu).should("be.visible"));
    cy.get(homePage.profileMenu).click();
    cy.get(homePage.signOutIcon).click();
    cy.wait(500);
    // validating sso with oidc is enabled
    cy.get(adminSettings.loginWithOIDC).should(
      "have.text",
      "Sign In with OIDC SSO",
    );
  });
});
