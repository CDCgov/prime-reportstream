import submissionsPage from "../../support/pages/Submissions";

describe("Submissions Page", () => {
    let submissionsResponse: Cypress.Response<Array<any>>;

    beforeEach(() => {
        cy.login();
        // TODO: parameterize the organization
        cy.setOrganization("ignore");
        submissionsPage.go();

        // TODO: parameterize the format
        cy.fetchSubmissions("ignore", "CSV").then(
            (response: Cypress.Response<Array<any>>) => {
                submissionsResponse = response;
            }
        );
    });

    it("renders the correct heading", () => {
        submissionsPage
            .getElements()
            .heading.should("contain", "Submission History");
    });

    // TODO: kind of a dumb test -- could check for pagination options?
    // same dumb assertion in daily-data-page
    it("renders a table with up to ten rows", () => {
        submissionsPage.paginatedTable
            .getElements()
            .tableRows.should(
                "have.length",
                submissionsPage.paginatedTable.getInitiallyDisplayedRows(
                    submissionsResponse.body.length
                )
            );
    });
});
