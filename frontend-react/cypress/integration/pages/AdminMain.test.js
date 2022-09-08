describe("Admin Main Page", () => {
    beforeEach(() => {
        cy.loginByOktaApi(
            Cypress.env("auth_username"),
            Cypress.env("auth_password")
        );
        cy.visit("/admin/settings");
    });
    it("should display the page", () => {
        cy.get("#tBodyFac").each((element) => expect(element).to.exist);
    });
});
