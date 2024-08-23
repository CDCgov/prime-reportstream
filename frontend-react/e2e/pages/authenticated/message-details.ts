import { MOCK_GET_MESSAGE } from "../../mocks/messages";
import { MESSAGE_ID } from "../../pages/authenticated/message-id-search";
import * as messageIdSearch from "../../pages/authenticated/message-id-search";

import { BasePage, BasePageTestArgs, RouteHandlerFulfillEntry } from "../BasePage";

export class MessageDetailsPage extends BasePage {
    static readonly URL_MESSAGE_DETAILS = `/message-details/${MESSAGE_ID}`;

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
            messageIdSearch.API_MESSAGE,
            () => {
                return {
                    json: MOCK_GET_MESSAGE,
                };
            },
        ];
    }
}
