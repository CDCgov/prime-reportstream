import { ResourcesBasePage } from "./Base";

export class ResourcesShowPage extends ResourcesBasePage {
    /**
     * @override
     */
    go(slug) {
        cy.visit(`${this.path}/${slug}`);
    }
}

export default new ResourcesShowPage();
