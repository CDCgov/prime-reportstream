import type { Locator } from "@playwright/test";
import { RSMessage } from "../../../../src/config/endpoints/reports";
import { MOCK_GET_TEST_MESSAGES } from "../../../mocks/message-test";
import { BasePage, BasePageTestArgs, type RouteHandlerFulfillEntry } from "../../BasePage";

/**
 * Uses org ignore's FULL_ELR receiver
 */
export class OrganizationReceiverMessageTestPage extends BasePage {
    static readonly API_REPORTS_TESTING = "/api/reports/testing";
    testMessages: RSMessage[];
    readonly form: Locator;
    readonly addCustomMessageButton: Locator;
    readonly submitCustomMessageButton: Locator;
    readonly cancelCustomMessageButton: Locator;
    readonly customMessageTextArea: Locator;
    readonly submitButton: Locator;

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: "/admin/orgreceiversettings/org/ignore/receiver/FULL_ELR/action/edit/message-testing",
                title: "Message testing - ReportStream",
                heading: testArgs.page.getByRole("heading", {
                    name: "Message testing",
                }),
            },
            testArgs,
        );

        this.testMessages = [];
        this.form = this.page.getByRole("form");
        this.addCustomMessageButton = this.form.getByRole("button", { name: "Test custom message" });
        this.submitCustomMessageButton = this.form.getByRole("button", { name: "Add" });
        this.cancelCustomMessageButton = this.form.getByRole("button", { name: "Cancel" });
        this.customMessageTextArea = this.form.getByRole("textbox", { name: "Custom message text" });
        this.submitButton = this.form.getByRole("button", { name: "Run test" });
        this.addMockRouteHandlers([this.createMockTestMessagesHandler()]);
        this.addResponseHandlers([
            [
                OrganizationReceiverMessageTestPage.API_REPORTS_TESTING,
                async (res) => (this.testMessages = await res.json()),
            ],
        ]);
    }

    get isPageLoadExpected() {
        return super.isPageLoadExpected && this.isAdminSession;
    }

    createMockTestMessagesHandler(): RouteHandlerFulfillEntry {
        return [
            OrganizationReceiverMessageTestPage.API_REPORTS_TESTING,
            () => {
                return {
                    json: MOCK_GET_TEST_MESSAGES,
                };
            },
        ];
    }
}
