import {
    BasePage,
    BasePageTestArgs,
    type RouteHandlerFulfillEntry,
} from "./BasePage";
import { RSOrganizationSettings } from "../../src/config/endpoints/settings";
import { MOCK_GET_ORGANIZATION_SETTINGS_LIST } from "../mocks/organizations";

export class OrganizationPage extends BasePage {
    static readonly API_ORGANIZATIONS = "/api/settings/organizations";
    protected _organizationSettings: RSOrganizationSettings[];
    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/admin/settings",
                title: "Organizations",
                heading: testArgs.page.getByRole("heading", {
                    name: "Organizations",
                }),
            },
            testArgs,
        );

        this._organizationSettings = [];
        this.addResponseHandlers([
            [
                OrganizationPage.API_ORGANIZATIONS,
                async (res) => (this._organizationSettings = await res.json()),
            ],
        ]);
        this.addMockRouteHandlers([this.createMockOrganizationHandler()]);
    }

    get isPageLoadExpected() {
        return (
            super.isPageLoadExpected ||
            this.testArgs.storageState !== this.testArgs.adminLogin.path
        );
    }

    createMockOrganizationHandler(): RouteHandlerFulfillEntry {
        console.log("createMockOrganizationHandler ");
        return [
            OrganizationPage.API_ORGANIZATIONS,
            () => {
                return {
                    json: MOCK_GET_ORGANIZATION_SETTINGS_LIST,
                };
            },
        ];
    }
}
