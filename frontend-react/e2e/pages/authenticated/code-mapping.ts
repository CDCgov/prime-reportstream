import { expect, Page } from "@playwright/test";
import { BasePage, BasePageTestArgs } from "../BasePage";

export class CodeMappingPage extends BasePage {
    static readonly URL_CODE_MAPPING = "/onboarding/code-mapping";

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: CodeMappingPage.URL_CODE_MAPPING,
                title: "Code mapping tool - ReportStream",
                heading: testArgs.page.getByRole("heading", {
                    name: "Code mapping tool",
                    exact: true,
                }),
            },
            testArgs,
        );
    }
}

export async function onLoad(page: Page) {
    await expect(page).toHaveURL(CodeMappingPage.URL_CODE_MAPPING);
    await expect(page).toHaveTitle(/Code mapping tool - ReportStream/);
}
