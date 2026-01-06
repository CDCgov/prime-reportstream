import { MOCK_GET_MESSAGE, MOCK_GET_MESSAGES } from "../../../mocks/messages";
import { BasePage, BasePageTestArgs, RouteHandlerFulfillEntry } from "../../BasePage";

export class MessageIDSearchPage extends BasePage {
    static readonly URL_MESSAGE_ID_SEARCH = "/admin/message-tracker";
    static readonly API_MESSAGES = "**/api/messages?messageId=*";
    static readonly API_MESSAGE = "**/api/message/*";
    static readonly MESSAGE_ID = "582098";

    constructor(testArgs: BasePageTestArgs) {
        super(
            {
                url: MessageIDSearchPage.URL_MESSAGE_ID_SEARCH,
                title: "Message ID search - Admin",
                heading: testArgs.page.getByRole("heading", {
                    name: "Message ID Search",
                }),
            },
            testArgs,
        );

        this.addMockRouteHandlers([this.createMessageIDSearchAPIHandler(), this.createMessagesIDSearchAPIHandler()]);
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

    createMessagesIDSearchAPIHandler(): RouteHandlerFulfillEntry {
        return [
            MessageIDSearchPage.API_MESSAGES,
            () => {
                return {
                    json: MOCK_GET_MESSAGES,
                };
            },
        ];
    }
}
