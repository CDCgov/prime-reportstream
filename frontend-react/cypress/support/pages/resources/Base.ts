import { BasePage } from "../Base";

/**
 * @abstract
 */
export class ResourcesBasePage extends BasePage {
    path = "/resources";

    elementsMap = {
        heading: "h1",
        subheading: "h2",
    };
}
