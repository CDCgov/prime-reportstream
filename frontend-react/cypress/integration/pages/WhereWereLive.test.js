import live from "../../../src/content/live.json";

describe("Where we're live", () => {
    beforeEach(() => {
        cy.visit("/product/where-were-live");
    });
    it("should display all the states we're live in", () => {
        const sortedStates = live.data.sort((a, b) =>
            a.state.localeCompare(b.state)
        );
        cy.get(".rs-livestate-two-column li")
            .each((element, index, list) => {
                expect(element).to.have.text(sortedStates[index].state);
            })
            .then((list) => {
                expect(list).to.have.length(live.data.length);
            });
    });
});
