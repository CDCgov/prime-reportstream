import live from "../../../src/content/live.json";
import overviewPage from "../../support/pages/product/Overview";
import whereWereLivePage from "../../support/pages/product/WhereWereLive";
import releaseNotesPage from "../../support/pages/product/ReleaseNotes";
import aboutPage from "../../support/pages/product/About";

describe("Product Pages", () => {
    context("Overview", () => {
        beforeEach(() => {
            overviewPage.go();
        });

        it("renders the correct heading", () => {
            overviewPage
                .getElements()
                .heading.should("contain", "Overview of ReportStream");
        });
    });

    context("Where We're Live", () => {
        beforeEach(() => {
            whereWereLivePage.go();
        });

        it("renders the correct heading", () => {
            whereWereLivePage
                .getElements()
                .heading.should("contain", "Where we're live");
        });

        // TODO: add more specific selectors
        it("should display all the states we're live in", () => {
            const sortedStates = live.data.sort((a, b) =>
                a.state.localeCompare(b.state)
            );
            cy.get(".rs-livestate-two-column li")
                .each((element, index) => {
                    expect(element).to.have.text(sortedStates[index].state);
                })
                .then((list) => {
                    expect(list).to.have.length(live.data.length);
                });
        });
    });

    context("Release Notes", () => {
        beforeEach(() => {
            releaseNotesPage.go();
        });

        it("renders the correct heading", () => {
            releaseNotesPage
                .getElements()
                .heading.should("contain", "Release notes");
        });
    });

    context("About", () => {
        beforeEach(() => {
            aboutPage.go();
        });

        it("renders the correct heading", () => {
            aboutPage.getElements().heading.should("contain", "About");
        });
    });
});
