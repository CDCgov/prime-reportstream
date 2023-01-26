import { BasePageComponent } from "../pageComponents/Base";

/**
 * @abstract
 */
export class BasePage extends BasePageComponent {
    path = "/";

    get baseTitle() {
        return Cypress.env("base_page_title");
    }

    go() {
        cy.visit(this.path);
    }

    skipToMainContent() {
        return cy
            .get("body")
            .tab()
            .findByText("Skip Nav")
            .click({ force: true });
    }
}
