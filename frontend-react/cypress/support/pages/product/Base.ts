import { BasePage } from "../Base";

/**
 * @abstract
 */
export class ProductBasePage extends BasePage {
    path = "/product";

    elementsMap = {
        heading: "h1",
        subheading: "h2",
        sidenav: "[data-testid='sidenav']",
    };
}
