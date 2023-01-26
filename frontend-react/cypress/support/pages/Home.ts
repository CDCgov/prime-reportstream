import { BasePage } from "./Base";

export class HomePage extends BasePage {
    path = "/";

    elementsMap = {
        heading: "[data-testid='heading']",
    };
}

export default new HomePage();
