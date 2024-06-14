import { Response } from "@playwright/test";

import { BasePage, BasePageTestArgs, RouteHandlerEntry } from "./BasePage";
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
    }

    /**
     * Error expected additionally if user context isn't admin
     */
    get isErrorExpected() {
        return (
            super.isErrorExpected ||
            this.testArgs.storageState !== this.testArgs.adminLogin.path
        );
    }

    async handlePageLoad(res: Promise<Response | null>) {
        if (this.isErrorExpected) return await res;

        const apiOrganizationSettingsRes = await this.page.waitForResponse(
            OrganizationPage.API_ORGANIZATIONS,
        );

        const organizationSettingsData: RSOrganizationSettings[] =
            await apiOrganizationSettingsRes.json();
        this._organizationSettings = organizationSettingsData;

        return await super.handlePageLoad(res);
    }

    resetRouteHandler(): void {
        if (this.isMocked) {
            this.addMockRouteHandlers([this.createMockOrganizationHandler()]);
        }
    }

    createMockOrganizationHandler(): RouteHandlerEntry {
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
