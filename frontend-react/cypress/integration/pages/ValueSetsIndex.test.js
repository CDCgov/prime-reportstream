describe("Value Sets Index Page", () => {
    beforeEach(() => {
        cy.login();
        cy.visit("/admin/value-sets");
    });

    // depends on database state, is this a useful test?
    it("should display the page with expected rows and header", () => {
        cy.get("tr").should("have.length", 12);
        cy.get("tr:first-child td").should("have.length", 4);
    });

    it("should have clickable links for each row that navigate to detail pages", () => {
        cy.get("tbody tr:first-child td:first-child a").should("exist");
        cy.get("tbody tr:first-child td:first-child a").should(
            "have.length",
            1
        );
        cy.get("tbody tr:first-child td:first-child a")
            .invoke("text")
            .then((linkText) => {
                cy.get("tr:first-child td:first-child a").click();
                cy.location("pathname").should(
                    "eq",
                    `/admin/value-sets/${linkText}`
                );
            });
    });
});
