describe("Value Sets Index Page", () => {
    beforeEach(() => {
        // this isn't doing the trick - need to figure out how to do a SUITE WIDE BEFORE HOOK
        // or take a different approach
        cy.loginByLocalStorage();
        cy.visit("/admin/value-sets");
    });

    // depends on database state, is this a useful test?
    it("should display the page with expected rows and header", () => {
        cy.get("tr").should("have.length", 12);
        cy.get("tr:first-child td").should("have.length", 4);
    });

    it("should have clickable links for each row that navigate to detail pages", () => {
        cy.get("tr").each((row, i) => {
            if (!i) {
                // skip header row
                return;
            }
            row.get("td:first-child a").should("have.length", 1);
        });

        cy.get("tr:first-child td:first-child a")
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
