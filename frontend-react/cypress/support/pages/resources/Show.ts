import { ResourcesBasePage } from "./Base";

export class ResourcesShowPage extends ResourcesBasePage {
    /**
     * @override
     */
    go(slug?: string) {
        if (slug) {
            cy.visit(`${this.path}/${slug}`);
        } else {
            super.go();
        }
    }
}

export default new ResourcesShowPage();
