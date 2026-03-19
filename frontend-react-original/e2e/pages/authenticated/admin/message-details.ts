import { MessageIDSearchPage } from "./message-id-search";
import { MOCK_GET_MESSAGE } from "../../../mocks/messages";

import { BasePage, BasePageTestArgs, RouteHandlerFulfillEntry } from "../../BasePage";

export class MessageDetailsPage extends BasePage {
    static readonly URL_MESSAGE_DETAILS = `/message-details/${MessageIDSearchPage.MESSAGE_ID}`;

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: MessageDetailsPage.URL_MESSAGE_DETAILS,
                title: "ReportStream - CDC's free, interoperable data transfer platform",
                heading: testArgs.page.getByRole("heading", {
                    name: "Message ID Search",
                }),
            },
            testArgs,
        );

        this.addMockRouteHandlers([this.createMessageIDSearchAPIHandler()]);
    }

    createMessageIDSearchAPIHandler(): RouteHandlerFulfillEntry {
        return [
            MessageIDSearchPage.API_MESSAGE,
            () => {
                return {
                    json: MOCK_GET_MESSAGE,
                };
            },
        ];
    }
}
