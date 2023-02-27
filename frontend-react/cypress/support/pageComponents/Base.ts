import Cypress from "cypress";

type ElementMap = {
    [key: string]: string;
};

type Elements = {
    [key: string]: Cypress.Chainable<any>;
};

/**
 * @abstract
 */
export class BasePageComponent {
    elementsMap: ElementMap = {};

    elements: Elements = {};

    getElements(): Elements {
        if (Object.keys(this.elements).length > 0) {
            return this.elements;
        }

        Object.entries(this.elementsMap).forEach(([name, selector]) => {
            Object.defineProperty(this.elements, name, {
                get() {
                    return cy.get(selector);
                },
                enumerable: true,
                configurable: true,
            });
        });

        return this.elements;
    }
}
