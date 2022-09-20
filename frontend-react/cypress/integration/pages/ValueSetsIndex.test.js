describe("Admin Main Page", () => {
    beforeEach(() => {
        cy
            .loginByOktaApi
            // Cypress.env("auth_username"),
            // Cypress.env("auth_password")
            ();
        cy.visit("/admin/value-sets");
    });
    // depends on database state, is this a useful test?
    it("should display the page with expected rows and header", () => {
        const rows = cy.get("tr");
        expect(rows).toHaveLength(12); // 11 value sets + 1 header row
        expect(rows[0].get("th")).toHaveLength(4);
    });

    it("should have clickable links for each row that navigate to detail pages", () => {
        const rows = cy.get("tr");
        rows.each((row, i) => {
            if (!i) {
                // skip header row
                return;
            }
            const firstCell = row.get("td")[0];
            const link = firstCell.get("a");
            expect(link).toHaveLength(1);
        });
        const firstLink = rows[0].get("a");
        const linkText = firstLink.text();
        cy.click(firstLink);
        expect(cy.location().pathname).toEqual(`admin/value-sets/${linkText}`);
    });
});
