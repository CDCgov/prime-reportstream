import type { Locator } from "@playwright/test";
import * as fs from "fs";
import language from "../../../../src/components/Admin/MessageTesting/language.json" assert { type: "json" };
import {
    errorMessageResult,
    passMessageResult,
    warningMessageResult,
} from "../../../../src/components/Admin/MessageTesting/MessageTestingResult.fixtures";
import { RSMessage } from "../../../../src/config/endpoints/reports";
import { MOCK_GET_TEST_MESSAGES } from "../../../mocks/message-test";
import { BasePage, BasePageTestArgs, type RouteHandlerFulfillEntry } from "../../BasePage";

/**
 * Uses org ignore's FULL_ELR receiver
 */
export class OrganizationReceiverMessageTestPage extends BasePage {
    static readonly API_REPORTS_TESTING = "/api/reports/testing";
    static readonly API_REPORTS_TEST = "/api/reports/testing/test?*";
    protected customI: number;
    testMessages: RSMessage[];

    readonly expectedStatusSuccess = new RegExp(`^${language.successAlertHeading}`);
    readonly expectedStatusFailure = new RegExp(`^${language.errorAlertHeading}`);
    readonly expectedStatusWarning = new RegExp(`^${language.warningAlertHeading}`);

    readonly form: Locator;
    readonly addCustomMessageButton: Locator;
    readonly submitCustomMessageButton: Locator;
    readonly cancelCustomMessageButton: Locator;
    readonly customMessageTextArea: Locator;
    readonly submitButton: Locator;
    readonly submitStatus: Locator;
    readonly submissionOutputMessageButton: Locator;
    readonly submissionOutputMessage: Locator;
    readonly submissionTestMessageButton: Locator;
    readonly submissionTestMessage: Locator;
    readonly submissionTransformErrorsButton: Locator;
    readonly submissionTransformErrors: Locator;
    readonly submissionTransformWarningsButton: Locator;
    readonly submissionTransformWarnings: Locator;

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
        this.customI = 0;
        this.form = this.page.getByRole("form");
        this.addCustomMessageButton = this.form.getByRole("button", { name: "Test custom message" });
        this.submitCustomMessageButton = this.form.getByRole("button", { name: "Add" });
        this.cancelCustomMessageButton = this.form.getByRole("button", { name: "Cancel" });
        this.customMessageTextArea = this.form.getByRole("textbox", { name: "Custom message text" });
        this.submitButton = this.form.getByRole("button", { name: "Run test" });
        this.submitStatus = this.page
            .getByRole("status")
            .or(this.page.getByRole("alert"))
            .or(this.page.getByRole("region", { name: "Information" }))
            .first();
        this.submissionOutputMessageButton = this.page.getByRole("button", { name: "Output message" });
        this.submissionOutputMessage = this.page.getByLabel("Output message");
        this.submissionTestMessageButton = this.page.getByRole("button", { name: "Test message" });
        this.submissionTestMessage = this.page.getByLabel("Test message");
        this.submissionTransformErrorsButton = this.page.getByRole("button", { name: "Transform errors" });
        this.submissionTransformErrors = this.page.getByLabel("Transform errors");
        this.submissionTransformWarningsButton = this.page.getByRole("button", { name: "Transform warnings" });
        this.submissionTransformWarnings = this.page.getByLabel("Transform warnings");
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

    createMockTestSubmissionHandler(resultType: "pass" | "fail" | "warn" = "pass"): RouteHandlerFulfillEntry {
        let result;
        switch (resultType) {
            case "fail":
                result = errorMessageResult;
                break;
            case "warn":
                result = warningMessageResult;
                break;
            default:
                result = passMessageResult;
                break;
        }
        return [
            OrganizationReceiverMessageTestPage.API_REPORTS_TEST,
            () => {
                return {
                    json: result,
                };
            },
        ];
    }

    addMockTestSubmissionHandler(resultType: "pass" | "fail" | "warn" = "pass") {
        return this.addMockRouteHandlers([this.createMockTestSubmissionHandler(resultType)]);
    }

    async downloadPDF() {
        // Listen for the 'download' event before firing
        const [download] = await Promise.all([
            this.page.waitForEvent("download"),
            this.page.click('text="Download PDF"'),
        ]);

        const filePath = await download.path();
        const stats = fs.statSync(filePath);

        return stats;
    }

    async submit() {
        const p = this.route();
        const reqP = this.page.waitForRequest(OrganizationReceiverMessageTestPage.API_REPORTS_TEST);
        await this.submitButton.click();
        await p;
        return reqP;
    }

    async addCustomMessage(message: string) {
        await this.addCustomMessageButton.click();
        await this.customMessageTextArea.fill(message);
        await this.submitCustomMessageButton.click();
        this.customI++;
        const fileName = `Custom message ${this.customI}`;
        return [this.form.getByLabel(fileName), this.form.getByText(fileName)];
    }
}
