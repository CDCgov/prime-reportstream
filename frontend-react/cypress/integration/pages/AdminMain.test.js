describe("Admin Main Page", () => {
    beforeEach(() => {
        cy.login();
        cy.visit("/admin/settings");
    });
    it("should display the page", () => {
        cy.get("#tBodyFac").each((element) => expect(element).to.exist);
    });
});
