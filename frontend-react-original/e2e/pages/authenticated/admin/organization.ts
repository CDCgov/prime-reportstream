import { expect } from "@playwright/test";
import { RSOrganizationSettings } from "../../../../src/config/endpoints/settings";
import { MOCK_GET_ORGANIZATION_SETTINGS_LIST } from "../../../mocks/organizations";
import { BasePage, BasePageTestArgs, type RouteHandlerFulfillEntry } from "../../BasePage";

export class OrganizationPage extends BasePage {
    static readonly API_ORGANIZATIONS = "/api/settings/organizations";
    protected _organizationSettings: RSOrganizationSettings[];

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/admin/settings",
                title: "Admin-Organizations",
                heading: testArgs.page.getByRole("heading", {
                    name: "Organizations",
                }),
            },
            testArgs,
        );

        this._organizationSettings = [];
        this.addResponseHandlers([
            [OrganizationPage.API_ORGANIZATIONS, async (res) => (this._organizationSettings = await res.json())],
        ]);
        this.addMockRouteHandlers([this.createMockOrganizationHandler()]);
    }

    get isPageLoadExpected() {
        return super.isPageLoadExpected && this.isAdminSession;
    }

    createMockOrganizationHandler(): RouteHandlerFulfillEntry {
        return [
            OrganizationPage.API_ORGANIZATIONS,
            () => {
                return {
                    json: MOCK_GET_ORGANIZATION_SETTINGS_LIST,
                };
            },
        ];
    }

    async testTableHeaders() {
        await expect(this.page.locator(".usa-table th").nth(0)).toHaveText(/Name/);
        await expect(this.page.locator(".usa-table th").nth(1)).toHaveText(/Description/);
        await expect(this.page.locator(".usa-table th").nth(2)).toHaveText(/Jurisdiction/);
        await expect(this.page.locator(".usa-table th").nth(3)).toHaveText(/State/);
        await expect(this.page.locator(".usa-table th").nth(4)).toHaveText(/County/);

        return true;
    }
}
