import homePage from "../../support/pages/Home";

describe("Home page", () => {
    beforeEach(() => {
        homePage.go();
    });

    it("returns a 200", () => {
        cy.request("/").then((response) => {
            expect(response).to.have.property("status", 200);
            expect(response.body).not.to.be.null;
        });
    });

    it("renders the correct heading", () => {
        homePage
            .getElements()
            .heading.should(
                "contain",
                "ReportStream makes it easier to send and collect public health data — for free"
            );
    });

    it("shows the correct page title", () => {
        cy.title().then((title) => {
            expect(title).to.equal(homePage.baseTitle);
        });
    });

    it("allows tabbing to skip to the main content", () => {
        homePage.skipToMainContent().url().should("contain", "#main-content");
    });
});
