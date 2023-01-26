import dailyDataPage from "../../support/pages/DailyData";

describe("Daily Data Page", () => {
    let receiversResponse: Cypress.Response<Array<any>>;

    beforeEach(() => {
        cy.login();
        // TODO: parameterize the organization
        cy.setOrganization("ignore");
        dailyDataPage.go();

        cy.fetchReceivers("ignore").then(
            (response: Cypress.Response<Array<any>>) => {
                receiversResponse = response;
            }
        );
    });

    it("renders the correct heading", () => {
        dailyDataPage.getElements().heading.should("contain", "Daily Data");
    });

    it("renders a dropdown with the correct number of receivers", () => {
        debugger;
        dailyDataPage
            .getElements()
            .receiversSelectOptions.should(
                "have.length",
                receiversResponse.body.length
            );
    });

    describe("when selecting a receiver", () => {
        let deliveriesResponse: Cypress.Response<Array<any>>;

        beforeEach(() => {
            // TODO: parameterize the receiver
            dailyDataPage.getElements().receiversSelect.select("CSV");

            cy.fetchDeliveries("ignore", "CSV").then(
                (response: Cypress.Response<Array<any>>) => {
                    deliveriesResponse = response;
                }
            );
        });

        // TODO: kind of a dumb test -- could check for pagination options?
        // same dumb assertion in submissions-page
        it("renders a table with up to ten rows", () => {
            dailyDataPage.paginatedTable
                .getElements()
                .tableRows.should(
                    "have.length",
                    dailyDataPage.paginatedTable.getInitiallyDisplayedRows(
                        deliveriesResponse.body.length
                    )
                );
        });
    });
});
