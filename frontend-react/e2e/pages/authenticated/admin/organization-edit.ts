import { expect, Locator, Page } from "@playwright/test";
import { RSReceiver, RSSender } from "../../../../src/config/endpoints/settings";
import { formatDate } from "../../../../src/utils/misc";
import {
    MOCK_GET_ORGANIZATION_IGNORE,
    MOCK_GET_RECEIVERS_IGNORE,
    MOCK_GET_SENDERS_IGNORE,
} from "../../../mocks/organizations";
import { BasePage, BasePageTestArgs, type RouteHandlerFulfillEntry } from "../../BasePage";

export class OrganizationEditPage extends BasePage {
    static readonly URL_ORGANIZATION_EDIT = `/admin/orgsettings/org/ignore`;
    static readonly API_ORGANIZATION_IGNORE = "/api/settings/organizations/ignore";
    static readonly API_SENDERS = "/api/settings/organizations/ignore/senders";
    static readonly API_RECEIVERS = "/api/settings/organizations/ignore/receivers";
    protected _organizationSenders: RSSender[];
    protected _organizationReceivers: RSReceiver[];

    readonly orgSenderNew: {
        cancel: Locator;
        save: Locator;
    };
    readonly orgSenderEdit: {
        modal: Locator;
        cancelButton: Locator;
        editJsonButton: Locator;
        editJsonModal: {
            save: Locator;
            checkSyntax: Locator;
            back: Locator;
        };
    };
    readonly orgReceiverNew: {
        cancel: Locator;
        save: Locator;
    };
    readonly orgReceiverEdit: {
        modal: Locator;
        cancelButton: Locator;
        editJsonButton: Locator;
        editJsonModal: {
            save: Locator;
            checkSyntax: Locator;
            back: Locator;
        };
    };

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: OrganizationEditPage.URL_ORGANIZATION_EDIT,
                title: "Organization edit - Admin",
            },
            testArgs,
        );

        this._organizationSenders = [];
        this._organizationReceivers = [];

        this.addMockRouteHandlers([
            this.createMockOrganizationIgnoreAPIHandler(),
            this.createMockOrganizationSenderAPIHandler(),
            this.createMockOrganizationReceiverAPIHandler(),
        ]);
        this.orgSenderNew = {
            cancel: this.page.getByRole("button", {
                name: "Cancel",
            }),
            save: this.page.getByRole("button", {
                name: "Save",
            }),
        };
        this.orgSenderEdit = {
            modal: this.page.getByTestId("modalWindow"),
            cancelButton: this.page.getByRole("button", {
                name: "Cancel",
            }),
            editJsonButton: this.page.getByRole("button", {
                name: "Edit json and save...",
            }),
            editJsonModal: {
                save: this.page.getByTestId("editCompareSaveButton"),
                checkSyntax: this.page.getByTestId("editCheckSyntaxButton"),
                back: this.page.getByRole("button", {
                    name: "Back",
                }),
            },
        };
        this.orgReceiverNew = {
            cancel: this.page.getByRole("button", {
                name: "Cancel",
            }),
            save: this.page.getByRole("button", {
                name: "Save",
            }),
        };

        this.orgReceiverEdit = {
            modal: this.page.getByTestId("modalWindow"),
            cancelButton: this.page.getByRole("button", {
                name: "Cancel",
            }),
            editJsonButton: this.page.getByRole("button", {
                name: "Edit json and save...",
            }),
            editJsonModal: {
                save: this.page.getByTestId("editCompareSaveButton"),
                checkSyntax: this.page.getByTestId("editCheckSyntaxButton"),
                back: this.page.getByRole("button", {
                    name: "Back",
                }),
            },
        };
    }

    get isPageLoadExpected() {
        return super.isPageLoadExpected && this.testArgs.storageState === this.testArgs.adminLogin.path;
    }

    createMockOrganizationIgnoreAPIHandler(): RouteHandlerFulfillEntry {
        return [
            OrganizationEditPage.API_ORGANIZATION_IGNORE,
            () => {
                return {
                    json: MOCK_GET_ORGANIZATION_IGNORE,
                };
            },
        ];
    }

    createMockOrganizationSenderAPIHandler(): RouteHandlerFulfillEntry {
        return [
            OrganizationEditPage.API_SENDERS,
            () => {
                return {
                    json: MOCK_GET_SENDERS_IGNORE,
                };
            },
        ];
    }

    createMockOrganizationReceiverAPIHandler(): RouteHandlerFulfillEntry {
        return [
            OrganizationEditPage.API_RECEIVERS,
            () => {
                return {
                    json: MOCK_GET_RECEIVERS_IGNORE,
                };
            },
        ];
    }

    async testTableHeaders() {
        await expect(this.page.locator(".usa-table th").nth(0)).toHaveText(/Name/);
        await expect(this.page.locator(".usa-table th").nth(1)).toHaveText(/Org Name/);
        await expect(this.page.locator(".usa-table th").nth(2)).toHaveText(/Topic/);
        await expect(this.page.locator(".usa-table th").nth(3)).toHaveText(/Status/);
        await expect(this.page.locator(".usa-table th").nth(4)).toHaveText(/Meta/);
        await expect(this.page.locator(".usa-table th").nth(4)).toHaveText(/Action/);

        return true;
    }

    getOrgMeta(metaData: any) {
        const { version, createdAt, createdBy } = metaData;

        // handle cases where individual metadata are not available
        const versionDisplay = version || version === 0 ? `v${version} ` : "";
        const createdAtDisplay = createdAt ? `[${formatDate(metaData.createdAt)}] ` : "";
        const createdByDisplay = createdBy ?? "";

        return `${versionDisplay}${createdAtDisplay}${createdByDisplay}`;
    }

    getApplyButton(page: Page) {
        return page.getByRole("button", {
            name: "Apply",
        });
    }
}
